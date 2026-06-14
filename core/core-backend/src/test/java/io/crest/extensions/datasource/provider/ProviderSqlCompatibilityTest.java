package io.crest.extensions.datasource.provider;

import io.crest.constant.FieldTypeConstants;
import io.crest.engine.sql.SQLProvider;
import io.crest.engine.trans.Field2SQLObj;
import io.crest.engine.trans.Table2SQLObj;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.utils.SqlPlaceholderUtils;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.constant.SqlPlaceholderConstants;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.parser.SqlParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class ProviderSqlCompatibilityTest {

    @Test
    void restoresUnicodeStringLiteralsProducedByOfficialCalcite() throws Exception {
        SqlParser parser = SqlParser.create(
                "SELECT * FROM t WHERE department IN ('交易履约部')",
                SqlParser.Config.DEFAULT.withLex(Lex.JAVA)
        );
        SqlNode sqlNode = parser.parseStmt();
        String officialSql = sqlNode.toSqlString(MysqlSqlDialect.DEFAULT).toString();

        String sql = Provider.restoreUnicodeStringLiterals(officialSql);

        assertTrue(officialSql.contains("u&'"));
        assertTrue(sql.contains("'交易履约部'"));
        assertFalse(sql.contains("u&'"));
    }

    @Test
    void restoresUnicodeEscapesWithoutChangingNormalStringContent() {
        String sql = Provider.restoreUnicodeStringLiterals(
                "SELECT u&'\\4ea4\\6613 O''Brien \\\\ path', 'keep u&''text' FROM t"
        );

        assertEquals("SELECT '交易 O''Brien \\\\ path', 'keep u&''text' FROM t", sql);
    }

    @Test
    void replacesOnlyCompleteGeneratedIdentifiers() {
        String sql = SqlPlaceholderUtils.replaceIdentifier(
                "SELECT CREST_CALC_FIELD_PLACEHOLDER_1, `CREST_CALC_FIELD_PLACEHOLDER_1`, "
                        + "CREST_CALC_FIELD_PLACEHOLDER_10 FROM t",
                "CREST_CALC_FIELD_PLACEHOLDER_1",
                "SUM(amount)"
        );

        assertEquals(
                "SELECT SUM(amount), SUM(amount), CREST_CALC_FIELD_PLACEHOLDER_10 FROM t",
                sql
        );
    }

    @Test
    void calculatedFieldReplacementUsesBoundedIdentifierMatching() {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        SQLMeta sqlMeta = new SQLMeta();
        sqlMeta.setXFieldsDialect(Map.of("CREST_CALC_FIELD_PLACEHOLDER_1", "SUM(amount)"));

        String sql = provider.replaceCalcFieldPlaceHolder(
                "SELECT CREST_CALC_FIELD_PLACEHOLDER_1, CREST_CALC_FIELD_PLACEHOLDER_10 FROM t",
                sqlMeta
        );

        assertEquals("SELECT SUM(amount), CREST_CALC_FIELD_PLACEHOLDER_10 FROM t", sql);
    }

    @Test
    void rebuildSqlTranslatesCalculatedDateFunctionsAfterPlaceholderReplacement() {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        SQLMeta sqlMeta = new SQLMeta();
        sqlMeta.setTableDialect("SELECT `date_text` FROM `excel_table`");
        sqlMeta.setXFieldsDialect(Map.of(
                "CREST_CALC_FIELD_PLACEHOLDER_1",
                "DE_STR_TO_DATE(`date_text`,'yyyy/MM/dd')"
        ));
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");

        String sql = provider.rebuildSQL(
                "SELECT CREST_CALC_FIELD_PLACEHOLDER_1 AS `date_value` FROM (" + SqlPlaceholderConstants.TABLE_PLACEHOLDER + ") t_a_0",
                sqlMeta,
                false,
                Map.of(1L, datasource),
                true
        );

        assertFalse(sql.contains("DE_STR_TO_DATE"));
        assertTrue(sql.contains("STR_TO_DATE(`date_text`, '%Y/%m/%d')"));
    }

    @Test
    void rebuildSqlTranslatesDateFunctionsFromAllLateBoundSqlFragments() {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        SQLMeta sqlMeta = new SQLMeta();
        sqlMeta.setTableDialect("SELECT `day_text`, `month_text`, `epoch_seconds` FROM `excel_table`");
        sqlMeta.setXFieldsDialect(Map.of(
                "CREST_CALC_FIELD_PLACEHOLDER_1",
                "DE_STR_TO_DATE(`day_text`,'yyyy/MM/dd')"
        ));
        sqlMeta.setYFieldsDialect(Map.of(
                "CREST_CALC_FIELD_PLACEHOLDER_2",
                "DE_UNIX_TIMESTAMP(DE_STR_TO_DATE(`day_text`,'yyyy-MM-dd HH:mm:ss'))"
        ));
        sqlMeta.setWhereTreesDialect(Map.of(
                "CREST_CALC_FIELD_PLACEHOLDER_3",
                "DE_DATE_FORMAT(DE_STR_TO_DATE(`month_text`,'yyyyMMdd'),'yyyy-MM')"
        ));
        sqlMeta.setCustomWheresDialect(Map.of(
                "CREST_CALC_FIELD_PLACEHOLDER_4",
                "DE_FROM_UNIXTIME(`epoch_seconds`,'yyyy-MM-dd HH:mm:ss')"
        ));
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");

        String sql = provider.rebuildSQL(
                "SELECT CREST_CALC_FIELD_PLACEHOLDER_1 AS `day_value`, "
                        + "SUM(CREST_CALC_FIELD_PLACEHOLDER_2) AS `day_score` "
                        + "FROM (" + SqlPlaceholderConstants.TABLE_PLACEHOLDER + ") t_a_0 "
                        + "WHERE CREST_CALC_FIELD_PLACEHOLDER_3 = '2026-06' "
                        + "ORDER BY CREST_CALC_FIELD_PLACEHOLDER_4 DESC",
                sqlMeta,
                false,
                Map.of(1L, datasource),
                true
        );

        assertFalse(sql.contains("DE_"));
        assertTrue(sql.contains("STR_TO_DATE(`day_text`, '%Y/%m/%d')"));
        assertTrue(sql.contains("UNIX_TIMESTAMP(STR_TO_DATE(`day_text`, '%Y-%m-%d %H:%i:%s'))"));
        assertTrue(sql.contains("DATE_FORMAT(STR_TO_DATE(`month_text`, '%Y%m%d'), '%Y-%m')"));
        assertTrue(sql.contains("FROM_UNIXTIME(`epoch_seconds`, '%Y-%m-%d %H:%i:%s')"));
    }

    @Test
    void rebuildSqlTranslatesDateFunctionsInsideWrappedExcelTableSql() {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        SQLMeta sqlMeta = new SQLMeta();
        sqlMeta.setTableDialect(
                "SELECT DE_STR_TO_DATE(`raw_day`,'yyyy/MM/dd') AS `raw_day` FROM `excel_table`"
        );
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");

        String sql = provider.rebuildSQL(
                "SELECT `raw_day` FROM (" + SqlPlaceholderConstants.TABLE_PLACEHOLDER + ") t_a_0",
                sqlMeta,
                false,
                Map.of(1L, datasource),
                true
        );

        assertFalse(sql.contains("DE_STR_TO_DATE"));
        assertTrue(sql.contains("STR_TO_DATE(`raw_day`, '%Y/%m/%d')"));
    }

    @Test
    void rebuildSqlTranslatesFieldGeneratedConversionsForWideExcelDatasetPreview() {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");
        Map<Long, DatasourceSchemaDTO> dsMap = Map.of(1L, datasource);
        SQLMeta sqlMeta = new SQLMeta();
        Table2SQLObj.table2sqlobj(
                sqlMeta,
                null,
                "(SELECT `f_release_day`, `f_test_day`, `f_dev_workload`, `f_pause_days`, `f_department`, `f_project_code` FROM `excel_table`)",
                false
        );
        List<DatasetTableFieldDTO> fields = List.of(
                field(1L, "项目版本投产时间", "f_release_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(2L, "项目版本提测时间", "f_test_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy-MM-dd"),
                field(3L, "开发预估工作量", "f_dev_workload", FieldTypeConstants.FLOAT, FieldTypeConstants.STRING, null),
                field(4L, "暂停时长", "f_pause_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(5L, "归属处室", "f_department", FieldTypeConstants.STRING, FieldTypeConstants.STRING, null),
                field(6L, "项目编号", "f_project_code", FieldTypeConstants.STRING, FieldTypeConstants.STRING, null)
        );

        Field2SQLObj.field2sqlObj(sqlMeta, fields, fields, false, dsMap, List.of(), null, null);
        String generatedSql = SQLProvider.createQuerySQL(sqlMeta, false, false, false);
        String rebuiltSql = provider.rebuildSQL(generatedSql, sqlMeta, false, dsMap, true);

        assertTrue(generatedSql.contains("DE_STR_TO_DATE"));
        assertFalse(rebuiltSql.contains("DE_"), rebuiltSql);
        assertTrue(rebuiltSql.contains("STR_TO_DATE(`t_a_0`.`f_release_day`, '%Y/%m/%d')"), rebuiltSql);
        assertTrue(rebuiltSql.contains("STR_TO_DATE(`t_a_0`.`f_test_day`, '%Y-%m-%d')"), rebuiltSql);
        assertTrue(rebuiltSql.contains("CAST(`t_a_0`.`f_dev_workload` AS DECIMAL(26, 8))"), rebuiltSql);
        assertTrue(rebuiltSql.contains("CAST(`t_a_0`.`f_pause_days` AS DECIMAL(19, 0))"), rebuiltSql);
    }

    @Test
    void removesOnlyCompleteSchemaAliasPrefixes() {
        String sql = SqlPlaceholderUtils.removeIdentifierPrefix(
                "SELECT s_a_1.f_1, `s_a_1`.f_2, s_a_10.f_3 FROM s_a_1.t",
                "s_a_1"
        );

        assertEquals("SELECT f_1, f_2, s_a_10.f_3 FROM t", sql);
    }

    private DatasetTableFieldDTO field(Long id, String name, String engineFieldName, int fieldType, int extractedFieldType, String dateFormat) {
        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setId(id);
        field.setName(name);
        field.setOriginName(name);
        field.setDbFieldName(engineFieldName);
        field.setEngineFieldName(engineFieldName);
        field.setType("TEXT");
        field.setFieldType(fieldType);
        field.setExtractedFieldType(extractedFieldType);
        field.setDateFormat(dateFormat);
        return field;
    }
}
