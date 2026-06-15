package io.crest.datasource.provider;

import io.crest.datasource.type.ObOracle;
import io.crest.datasource.type.Oracle;
import io.crest.extensions.datasource.dto.DatasourceRequest;
import io.crest.extensions.datasource.dto.TableField;
import io.crest.extensions.datasource.vo.DatasourceConfiguration;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 校验 OceanBase Oracle 兼容模式下的字段注释和只读连接策略。
 */
public class CalciteProviderObOracleCommentsTest {

    /**
     * 验证 Oracle 字典注释会覆盖字段展示名称。
     */
    @Test
    public void applyColumnCommentsUsesOracleDictionaryName() {
        TableField id = field("ID", "ID");
        TableField name = field("USER_NAME", "USER_NAME");
        Map<String, String> comments = new HashMap<>();
        comments.put("ID", "主键ID");
        comments.put("USER_NAME", "用户名称");

        CalciteProvider.applyOracleColumnComments(java.util.Arrays.asList(id, name), comments);

        assertEquals("主键ID", id.getName());
        assertEquals("用户名称", name.getName());
    }

    /**
     * 验证空白注释不会覆盖字段原有展示名称。
     */
    @Test
    public void applyColumnCommentsKeepsFallbackNameWhenCommentIsBlank() {
        TableField amount = field("AMOUNT", "AMOUNT");
        Map<String, String> comments = new HashMap<>();
        comments.put("AMOUNT", " ");

        CalciteProvider.applyOracleColumnComments(java.util.Collections.singletonList(amount), comments);

        assertEquals("AMOUNT", amount.getName());
    }

    /**
     * 验证未显式配置 schema 时使用 OceanBase 用户名中的默认 schema。
     */
    @Test
    public void parseDatasourceConfigurationUsesObOracleDefaultSchema() throws Exception {
        CalciteProvider provider = new CalciteProvider();
        Method method = CalciteProvider.class.getDeclaredMethod("parseDatasourceConfiguration", String.class, String.class);
        method.setAccessible(true);

        DatasourceConfiguration configuration = (DatasourceConfiguration) method.invoke(
                provider,
                "{\"username\":\"test@obora#obdemo\"}",
                "obOracle"
        );

        assertEquals("TEST", configuration.getSchema());
        assertTrue(((ObOracle) configuration).getReadOnly());
    }

    /**
     * 验证 OceanBase Oracle 数据源会保留显式配置的只读开关。
     */
    @Test
    public void parseDatasourceConfigurationKeepsObOracleReadOnlySwitch() throws Exception {
        CalciteProvider provider = new CalciteProvider();
        Method method = CalciteProvider.class.getDeclaredMethod("parseDatasourceConfiguration", String.class, String.class);
        method.setAccessible(true);

        DatasourceConfiguration configuration = (DatasourceConfiguration) method.invoke(
                provider,
                "{\"username\":\"test@obora#obdemo\",\"readOnly\":false}",
                "obOracle"
        );

        assertFalse(((ObOracle) configuration).getReadOnly());
    }

    /**
     * 验证 OceanBase Oracle 默认使用只读连接。
     */
    @Test
    public void shouldUseReadOnlyConnectionForObOracleByDefault() {
        CalciteProvider provider = new CalciteProvider();

        assertTrue(provider.shouldUseReadOnlyConnection(new DatasourceRequest(), new ObOracle(), "obOracle"));
    }

    /**
     * 验证 OceanBase Oracle 可以通过数据源配置关闭只读连接。
     */
    @Test
    public void shouldRespectObOracleReadOnlySwitch() {
        CalciteProvider provider = new CalciteProvider();
        ObOracle obOracle = new ObOracle();
        obOracle.setReadOnly(false);

        assertFalse(provider.shouldUseReadOnlyConnection(new DatasourceRequest(), obOracle, "obOracle"));
    }

    /**
     * 验证请求级只读标记优先于数据源只读开关。
     */
    @Test
    public void explicitReadOnlyRequestOverridesDatasourceSwitch() {
        CalciteProvider provider = new CalciteProvider();
        DatasourceRequest request = new DatasourceRequest();
        ObOracle obOracle = new ObOracle();
        obOracle.setReadOnly(false);
        request.setReadOnly(true);

        assertTrue(provider.shouldUseReadOnlyConnection(request, obOracle, "obOracle"));
    }

    /**
     * 验证 OceanBase Oracle 的只读策略不会影响普通 Oracle 数据源。
     */
    @Test
    public void shouldNotApplyObOracleSwitchToOracle() {
        CalciteProvider provider = new CalciteProvider();

        assertFalse(provider.shouldUseReadOnlyConnection(new DatasourceRequest(), new Oracle(), "oracle"));
    }

    /**
     * 构造用于注释覆盖测试的字段描述对象。
     */
    private TableField field(String originName, String name) {
        TableField field = new TableField();
        field.setOriginName(originName);
        field.setName(name);
        return field;
    }
}
