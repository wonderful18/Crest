package io.crest.datasource.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.crest.extensions.datasource.dto.ApiDefinition;
import io.crest.extensions.datasource.dto.ApiDefinitionRequest;
import io.crest.extensions.datasource.dto.TableField;
import java.net.InetSocketAddress;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 验证 API JSONPath 解析和固定请求头处理。
public class ApiUtilsJsonPathTest {

    private static final String RESPONSE = """
            {
              "data": {
                "dataList": [
                  {"id": 1, "user": {"name": "Ada"}, "tags": [{"code": "vip"}]},
                  {"id": 2, "user": {"name": "Bob"}, "tags": [{"code": "new"}]}
                ]
              }
            }
            """;

    // 重置 API 工具的 JSON 解析器。
    @BeforeEach
    public void setUp() {
        ApiUtils.objectMapper = new ObjectMapper();
    }

    // 校验无点号 JSONPath 能解析嵌套字段。
    @Test
    public void jsonPathModeAcceptsDollarPathWithoutDotAndBuildsNestedFields() throws Exception {
        ApiDefinition definition = new ApiDefinition();
        definition.setUseJsonPath(true);
        definition.setJsonPath("$data.dataList");
        definition.setFields(new ArrayList<>());

        ApiDefinition checked = invokeCheckApiDefinition(definition, RESPONSE);

        Map<String, Object> user = field(checked.getJsonFields(), "user");
        assertNotNull(user);
        Map<String, Object> userName = field(children(user), "name");
        assertNotNull(userName);
        assertTrue((Boolean) userName.get("checked"));
        assertEquals(List.of("Ada", "Bob"), userName.get("value"));

        Map<String, Object> tags = field(checked.getJsonFields(), "tags");
        assertNotNull(tags);
        Map<String, Object> tagCode = field(children(tags), "code");
        assertNotNull(tagCode);
        assertTrue((Boolean) tagCode.get("checked"));
        assertEquals(List.of("vip", "new"), tagCode.get("value"));
    }

    // 校验结构预览在 JSONPath 模式下只展示选中行字段。
    @Test
    public void apiStructurePreviewUsesJsonPathRowsWhenJsonPathIsEnabled() throws Exception {
        ApiDefinition definition = new ApiDefinition();
        definition.setShowApiStructure(true);
        definition.setUseJsonPath(true);
        definition.setJsonPath("$data.dataList");
        definition.setFields(new ArrayList<>());

        ApiDefinition checked = invokeCheckApiDefinition(definition, RESPONSE);

        assertEquals("$.data.dataList", checked.getJsonPath());
        assertNotNull(field(checked.getJsonFields(), "id"));
        assertNotNull(field(checked.getJsonFields(), "user"));
        assertNotNull(field(checked.getJsonFields(), "tags"));
        assertEquals(null, field(checked.getJsonFields(), "data"));
    }

    // 校验 JSONPath 模式能从选中行提取嵌套字段。
    @Test
    public void jsonPathModeFetchesNestedFieldsFromSelectedRows() throws Exception {
        ApiDefinition definition = new ApiDefinition();
        definition.setUseJsonPath(true);
        definition.setJsonPath("$data.dataList");
        definition.setFields(List.of(
                tableField("id", "$.data.dataList[*].['id']"),
                tableField("name", "$.data.dataList[*].['user'].['name']"),
                tableField("tagCode", "$.data.dataList[*].['tags'][*].['code']")
        ));

        List<String[]> rows = invokeFetchResult(RESPONSE, definition);

        assertEquals(2, rows.size());
        assertEquals("1", rows.get(0)[0]);
        assertEquals("Ada", rows.get(0)[1]);
        assertEquals("vip", rows.get(0)[2]);
        assertEquals("2", rows.get(1)[0]);
        assertEquals("Bob", rows.get(1)[1]);
        assertEquals("new", rows.get(1)[2]);
    }

    // 校验当前行内的多个嵌套值会保留为数组文本。
    @Test
    public void jsonPathModeKeepsMultipleNestedValuesInCurrentRow() throws Exception {
        String response = """
                {
                  "data": {
                    "dataList": [
                      {"id": 1, "tags": [{"code": "vip"}, {"code": "paid"}]},
                      {"id": 2, "tags": [{"code": "new"}]}
                    ]
                  }
                }
                """;
        ApiDefinition definition = new ApiDefinition();
        definition.setUseJsonPath(true);
        definition.setJsonPath("$.data.dataList");
        definition.setFields(List.of(
                tableField("id", "$.data.dataList[*].['id']"),
                tableField("tagCode", "$.data.dataList[*].['tags'][*].['code']")
        ));

        List<String[]> rows = invokeFetchResult(response, definition);

        assertEquals(2, rows.size());
        assertEquals("1", rows.get(0)[0]);
        assertEquals("[\"vip\",\"paid\"]", rows.get(0)[1]);
        assertEquals("2", rows.get(1)[0]);
        assertEquals("new", rows.get(1)[1]);
    }

    // 校验固定 Authorization 请求头会被发送。
    @Test
    public void execHttpRequestSendsFixedAuthorizationHeader() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/users", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = RESPONSE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ApiDefinition definition = new ApiDefinition();
            definition.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/users");
            definition.setMethod("GET");
            definition.setRequest(new ApiDefinitionRequest());
            definition.getRequest().setHeaders(List.of(Map.of(
                    "name", "Authorization",
                    "nameType", "fixed",
                    "value", "Bearer crest-token"
            )));

            String response = ApiUtils.execHttpRequest(true, definition, 10, List.of());

            assertTrue(response.contains("dataList"));
            assertEquals("Bearer crest-token", authorization.get());
        } finally {
            server.stop(0);
        }
    }

    // 反射调用 API 定义检查方法。
    private ApiDefinition invokeCheckApiDefinition(ApiDefinition definition, String response) throws Exception {
        Method method = ApiUtils.class.getDeclaredMethod("checkApiDefinition", ApiDefinition.class, String.class);
        method.setAccessible(true);
        return (ApiDefinition) method.invoke(null, definition, response);
    }

    // 反射调用 API 结果提取方法。
    @SuppressWarnings("unchecked")
    private List<String[]> invokeFetchResult(String response, ApiDefinition definition) throws Exception {
        Method method = ApiUtils.class.getDeclaredMethod("fetchResult", String.class, ApiDefinition.class);
        method.setAccessible(true);
        return (List<String[]>) method.invoke(null, response, definition);
    }

    // 读取字段节点的子节点。
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> children(Map<String, Object> field) {
        return (List<Map<String, Object>>) field.get("children");
    }

    // 按原始字段名查找字段节点。
    private Map<String, Object> field(List<Map<String, Object>> fields, String originName) {
        for (Map<String, Object> field : fields) {
            if (originName.equals(field.get("originName"))) {
                return field;
            }
        }
        return null;
    }

    // 构造测试用表字段。
    private TableField tableField(String name, String jsonPath) {
        TableField field = new TableField();
        field.setName(name);
        field.setOriginName(name);
        field.setJsonPath(jsonPath);
        return field;
    }
}
