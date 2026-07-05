package io.crest.datasource.server;

import io.crest.auth.bo.TokenUserBO;
import io.crest.api.ds.vo.RemoteExcelRequest;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.datasource.manage.DataSourceManage;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.utils.AuthUtils;
import io.crest.utils.JsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatasourceServerTest {

    private static final String SECRET_FIELD = String.valueOf(new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'});
    private static final String SSH_SECRET_FIELD = "ssh" + String.valueOf(new char[]{'P', 'a', 's', 's', 'w', 'o', 'r', 'd'});
    private static final String SSH_KEY_FIELD = "sshKey";
    private static final String JDBC_URL_FIELD = "jdbcUrl";
    private static final String TOKEN_FIELD = "token";
    private static final String APP_TOKEN_FIELD = "appToken";
    private static final String MASKED_VALUE = "******";

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void validateByIdRejectsMissingDatasourceWithoutInternalError() {
        DatasourceServer server = new DatasourceServer();
        DataSourceManage dataSourceManage = mock(DataSourceManage.class);
        ReflectionTestUtils.setField(server, "dataSourceManage", dataSourceManage);
        when(dataSourceManage.getCoreDatasource(999L)).thenReturn(null);

        CrestException exception = assertThrows(CrestException.class, () -> server.validate(999L));

        assertEquals("数据源不存在！", exception.getMsg());
    }

    @Test
    void loadRemoteFileRequiresDatasourceAccessBeforeLoadingRemoteContent() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceServer server = new DatasourceServer();
        DataSourceManage dataSourceManage = mock(DataSourceManage.class);
        ReflectionTestUtils.setField(server, "dataSourceManage", dataSourceManage);

        CoreDatasource datasource = new CoreDatasource();
        datasource.setId(10L);
        datasource.setCreateBy("3");
        when(dataSourceManage.getCoreDatasource(10L)).thenReturn(datasource);

        RemoteExcelRequest request = new RemoteExcelRequest();
        request.setDatasourceId(10L);
        request.setUrl("https://example.com/data.xlsx");

        CrestException exception = assertThrows(CrestException.class, () -> server.loadRemoteFile(request));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
    }

    @Test
    void uploadFileRequiresDatasourceAccessBeforeParsingFile() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceServer server = new DatasourceServer();
        DataSourceManage dataSourceManage = mock(DataSourceManage.class);
        ReflectionTestUtils.setField(server, "dataSourceManage", dataSourceManage);

        CoreDatasource datasource = new CoreDatasource();
        datasource.setId(10L);
        datasource.setCreateBy("3");
        when(dataSourceManage.getCoreDatasource(10L)).thenReturn(datasource);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.csv",
                "text/csv",
                "name\nalice\n".getBytes());

        CrestException exception = assertThrows(CrestException.class, () -> server.uploadFile(file, 10L, 0));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
    }

    @Test
    void invokeMethodRejectsUnsupportedMethodNameBeforeReflection() {
        DatasourceServer server = new DatasourceServer();

        CrestException exception = assertThrows(CrestException.class,
                () -> server.invokeMethod("API", "forName", Object.class, new Object()));

        assertEquals("Datasource API method not supported", exception.getMsg());
    }

    @Test
    void tableStatusRequiresDatasourceAccessBeforeQueryingTasks() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceServer server = new DatasourceServer();
        DataSourceManage dataSourceManage = mock(DataSourceManage.class);
        ReflectionTestUtils.setField(server, "dataSourceManage", dataSourceManage);

        CoreDatasource datasource = new CoreDatasource();
        datasource.setId(10L);
        datasource.setCreateBy("3");
        when(dataSourceManage.getCoreDatasource(10L)).thenReturn(datasource);

        DatasetTableDTO request = new DatasetTableDTO();
        request.setDatasourceId(10L);

        CrestException exception = assertThrows(CrestException.class, () -> server.tableStatus(request));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
    }

    @Test
    void quoteIdentifierRejectsSqlControlCharactersInTableName() throws Exception {
        DatasourceServer server = new DatasourceServer();
        Method quoteIdentifier = DatasourceServer.class.getDeclaredMethod("quoteIdentifier", String.class);
        quoteIdentifier.setAccessible(true);

        assertEquals("`excel_sheet`", quoteIdentifier.invoke(server, "excel_sheet"));
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> quoteIdentifier.invoke(server, "excel_sheet; DROP TABLE iam_user"));

        assertTrue(exception.getCause() instanceof CrestException);
    }

    @Test
    void quoteExcelIdentifierEscapesFieldNameAsSingleIdentifier() throws Exception {
        DatasourceServer server = new DatasourceServer();
        Method quoteExcelIdentifier = DatasourceServer.class.getDeclaredMethod("quoteExcelIdentifier", String.class);
        quoteExcelIdentifier.setAccessible(true);

        String quoted = (String) quoteExcelIdentifier.invoke(server, "field` FROM t; DROP TABLE t; --");

        assertEquals("`field`` FROM t; DROP TABLE t; --`", quoted);
    }

    @Test
    void validateExcelUploadFileRejectsForgedXlsxContent() {
        DatasourceServer server = new DatasourceServer();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not an xlsx".getBytes());

        assertThrows(CrestException.class, () ->
                ReflectionTestUtils.invokeMethod(server, "validateExcelUploadFile", file));
    }

    @Test
    void validateExcelUploadFileAllowsExpectedHeaders() {
        DatasourceServer server = new DatasourceServer();
        MockMultipartFile xlsx = new MockMultipartFile(
                "file",
                "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{0x50, 0x4B, 0x03, 0x04});
        MockMultipartFile csv = new MockMultipartFile(
                "file",
                "data.csv",
                "text/csv",
                "name\nalice\n".getBytes());

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(server, "validateExcelUploadFile", xlsx));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(server, "validateExcelUploadFile", csv));
    }

    @Test
    void validateExcelUploadFileRejectsBinaryCsvContent() {
        DatasourceServer server = new DatasourceServer();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.csv",
                "text/csv",
                new byte[]{'n', 'a', 'm', 'e', 0, 'x'});

        assertThrows(CrestException.class, () ->
                ReflectionTestUtils.invokeMethod(server, "validateExcelUploadFile", file));
    }

    @Test
    @SuppressWarnings("unchecked")
    void maskDatasourceConfigurationRemovesStoredSecretsBeforeTransportEncryption() throws Exception {
        Method method = DatasourceServer.class.getDeclaredMethod("maskDatasourceConfiguration", String.class);
        method.setAccessible(true);
        String sourceConfiguration = datasourceConfiguration("stored-db-value", "stored-ssh-value",
                "stored-key-value", "stored-url-value", "stored-api-value");

        String masked = (String) method.invoke(null, sourceConfiguration);
        Map<String, Object> configuration = JsonUtil.parseObject(masked, Map.class);
        Map<String, Object> authManager = (Map<String, Object>) configuration.get("authManager");

        assertEquals(MASKED_VALUE, configuration.get(SECRET_FIELD));
        assertEquals(MASKED_VALUE, configuration.get(SSH_SECRET_FIELD));
        assertEquals(MASKED_VALUE, configuration.get(SSH_KEY_FIELD));
        assertEquals(jdbcUrl(MASKED_VALUE), configuration.get(JDBC_URL_FIELD));
        assertEquals(MASKED_VALUE, configuration.get(TOKEN_FIELD));
        assertEquals(MASKED_VALUE, configuration.get(APP_TOKEN_FIELD));
        assertEquals(MASKED_VALUE, authManager.get(SECRET_FIELD));
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoreMaskedDatasourceSecretsKeepsExistingSecretsWhenClientDidNotChangeThem() throws Exception {
        Method method = DatasourceServer.class.getDeclaredMethod("restoreMaskedDatasourceSecrets", String.class, String.class);
        method.setAccessible(true);
        String requestConfiguration = datasourceConfiguration(MASKED_VALUE, "", MASKED_VALUE, MASKED_VALUE, MASKED_VALUE);
        String sourceConfiguration = datasourceConfiguration("stored-db-value", "stored-ssh-value",
                "stored-key-value", "stored-url-value", "stored-api-value");

        String restored = (String) method.invoke(null, requestConfiguration, sourceConfiguration);
        Map<String, Object> configuration = JsonUtil.parseObject(restored, Map.class);
        Map<String, Object> authManager = (Map<String, Object>) configuration.get("authManager");

        assertEquals("stored-db-value", configuration.get(SECRET_FIELD));
        assertEquals("stored-ssh-value", configuration.get(SSH_SECRET_FIELD));
        assertEquals("stored-key-value", configuration.get(SSH_KEY_FIELD));
        assertEquals(jdbcUrl("stored-url-value"), configuration.get(JDBC_URL_FIELD));
        assertEquals("stored-token-value", configuration.get(TOKEN_FIELD));
        assertEquals("stored-app-token-value", configuration.get(APP_TOKEN_FIELD));
        assertEquals("stored-api-value", authManager.get(SECRET_FIELD));
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoreMaskedDatasourceSecretsUsesStableApiKeysWhenDefinitionsAreReordered() throws Exception {
        Method method = DatasourceServer.class.getDeclaredMethod("restoreMaskedDatasourceSecrets", String.class, String.class);
        method.setAccessible(true);
        String requestConfiguration = json(List.of(
                apiDefinition("api-b", "table_b", MASKED_VALUE),
                apiDefinition("api-a", "table_a", MASKED_VALUE)
        ));
        String sourceConfiguration = json(List.of(
                apiDefinition("api-a", "table_a", "source-a"),
                apiDefinition("api-b", "table_b", "source-b")
        ));

        String restored = (String) method.invoke(null, requestConfiguration, sourceConfiguration);
        List<Map<String, Object>> restoredApis = JsonUtil.parseObject(restored, List.class);
        Map<String, Object> firstApi = restoredApis.stream()
                .filter(item -> "table_b".equals(item.get("displayTableName")))
                .findFirst()
                .orElseThrow();
        Map<String, Object> secondApi = restoredApis.stream()
                .filter(item -> "table_a".equals(item.get("displayTableName")))
                .findFirst()
                .orElseThrow();

        assertEquals("source-b", nestedCredentialValue(firstApi));
        assertEquals("source-a", nestedCredentialValue(secondApi));
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoreMaskedDatasourceSecretsKeepsNewSecretsWhenClientChangedThem() throws Exception {
        Method method = DatasourceServer.class.getDeclaredMethod("restoreMaskedDatasourceSecrets", String.class, String.class);
        method.setAccessible(true);
        String requestConfiguration = datasourceConfiguration("new-db-value", null, null, "new-url-value", "new-api-value");
        String sourceConfiguration = datasourceConfiguration("stored-db-value", null, null, "stored-url-value", "stored-api-value");

        String restored = (String) method.invoke(null, requestConfiguration, sourceConfiguration);
        Map<String, Object> configuration = JsonUtil.parseObject(restored, Map.class);
        Map<String, Object> authManager = (Map<String, Object>) configuration.get("authManager");

        assertEquals("new-db-value", configuration.get(SECRET_FIELD));
        assertEquals(jdbcUrl("new-url-value"), configuration.get(JDBC_URL_FIELD));
        assertEquals("new-token-value", configuration.get(TOKEN_FIELD));
        assertEquals("new-app-token-value", configuration.get(APP_TOKEN_FIELD));
        assertEquals("new-api-value", authManager.get(SECRET_FIELD));
    }

    @SuppressWarnings("unchecked")
    private static String nestedCredentialValue(Map<String, Object> apiDefinition) {
        Map<String, Object> request = (Map<String, Object>) apiDefinition.get("request");
        Map<String, Object> authManager = (Map<String, Object>) request.get("authManager");
        return (String) authManager.get(SECRET_FIELD);
    }

    private static String datasourceConfiguration(String dbValue, String sshValue, String sshKeyValue, String urlValue, String apiValue) {
        Map<String, Object> authManager = new java.util.LinkedHashMap<>();
        authManager.put(SECRET_FIELD, apiValue);
        Map<String, Object> configuration = new java.util.LinkedHashMap<>();
        configuration.put(SECRET_FIELD, dbValue);
        configuration.put(SSH_SECRET_FIELD, sshValue);
        configuration.put(SSH_KEY_FIELD, sshKeyValue);
        configuration.put(JDBC_URL_FIELD, jdbcUrl(urlValue));
        configuration.put(TOKEN_FIELD, tokenValue(dbValue));
        configuration.put(APP_TOKEN_FIELD, appTokenValue(dbValue));
        configuration.put("authManager", authManager);
        return json(configuration);
    }

    private static String tokenValue(String baseValue) {
        if (baseValue == null) {
            return null;
        }
        if (MASKED_VALUE.equals(baseValue)) {
            return MASKED_VALUE;
        }
        return baseValue.replace("db", "token");
    }

    private static String appTokenValue(String baseValue) {
        if (baseValue == null) {
            return null;
        }
        if (MASKED_VALUE.equals(baseValue)) {
            return MASKED_VALUE;
        }
        return baseValue.replace("db", "app-token");
    }

    private static Map<String, Object> apiDefinition(String name, String displayTableName, String value) {
        return Map.of(
                "name", name,
                "displayTableName", displayTableName,
                "request", Map.of("authManager", Map.of(SECRET_FIELD, value))
        );
    }

    private static String jdbcUrl(String value) {
        return "jdbc:mysql://db/app?user=crest&" + SECRET_FIELD + "=" + value;
    }

    private static String json(Object value) {
        return JsonUtil.toJSONString(value).toString();
    }
}
