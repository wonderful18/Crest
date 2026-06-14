package io.crest.extensions.datasource.provider;

import io.crest.api.chart.dto.ChartSortFieldDTO;
import io.crest.constant.FieldTypeConstants;
import io.crest.engine.sql.SQLProvider;
import io.crest.engine.trans.Dimension2SQLObj;
import io.crest.engine.trans.Field2SQLObj;
import io.crest.engine.trans.Order2SQLObj;
import io.crest.engine.trans.Table2SQLObj;
import io.crest.engine.trans.Quota2SQLObj;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.utils.SqlPlaceholderUtils;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.constant.SqlPlaceholderConstants;
import io.crest.extensions.view.dto.ChartViewDTO;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import io.crest.extensions.view.dto.ChartViewFieldFilterDTO;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.parser.SqlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_release_day`");
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_test_day`");
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_dev_workload`");
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_pause_days`");
        assertTrue(rebuiltSql.contains("STR_TO_DATE(CASE WHEN `t_a_0`.`f_release_day` IS NULL THEN NULL"), rebuiltSql);
        assertTrue(rebuiltSql.contains("STR_TO_DATE(CASE WHEN `t_a_0`.`f_test_day` IS NULL THEN NULL"), rebuiltSql);
        assertTrue(rebuiltSql.contains("CAST(CASE WHEN `t_a_0`.`f_dev_workload` IS NULL THEN NULL"), rebuiltSql);
        assertTrue(rebuiltSql.contains("CAST(CASE WHEN `t_a_0`.`f_pause_days` IS NULL THEN NULL"), rebuiltSql);
    }

    @Test
    void rebuildSqlNormalizesTextPlaceholdersBeforeFieldGeneratedConversionsForWideExcelDatasetPreview() {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");
        Map<Long, DatasourceSchemaDTO> dsMap = Map.of(1L, datasource);
        SQLMeta sqlMeta = new SQLMeta();
        Table2SQLObj.table2sqlobj(
                sqlMeta,
                null,
                "(SELECT `f_release_day`, `f_dev_workload`, `f_final_days`, `f_department` FROM `excel_table`)",
                false
        );
        List<DatasetTableFieldDTO> fields = List.of(
                field(1L, "项目版本投产时间", "f_release_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(2L, "开发预估工作量", "f_dev_workload", FieldTypeConstants.FLOAT, FieldTypeConstants.STRING, null),
                field(3L, "需求提出到项目终投产天数", "f_final_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(4L, "归属处室", "f_department", FieldTypeConstants.STRING, FieldTypeConstants.STRING, null)
        );

        Field2SQLObj.field2sqlObj(sqlMeta, fields, fields, false, dsMap, List.of(), null, null);
        String generatedSql = SQLProvider.createQuerySQL(sqlMeta, false, false, false);
        String rebuiltSql = provider.rebuildSQL(generatedSql, sqlMeta, false, dsMap, true);

        assertFalse(rebuiltSql.contains("DE_"), rebuiltSql);
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_release_day`");
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_dev_workload`");
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_final_days`");
        assertTrue(rebuiltSql.contains("STR_TO_DATE(CASE WHEN `t_a_0`.`f_release_day` IS NULL THEN NULL"), rebuiltSql);
        assertTrue(rebuiltSql.contains("CAST(CASE WHEN `t_a_0`.`f_dev_workload` IS NULL THEN NULL"), rebuiltSql);
        assertTrue(rebuiltSql.contains("CAST(CASE WHEN `t_a_0`.`f_final_days` IS NULL THEN NULL"), rebuiltSql);
        assertTrue(rebuiltSql.contains("`t_a_0`.`f_department` AS `f_ax_3`"), rebuiltSql);
    }

    @Test
    void wideExcelDatasetPreviewSqlNormalizesEveryConvertedFieldAndExplicitSorts() {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");
        Map<Long, DatasourceSchemaDTO> dsMap = Map.of(1L, datasource);
        SQLMeta sqlMeta = new SQLMeta();
        List<DatasetTableFieldDTO> fields = representativeExcelConversionFields();
        Table2SQLObj.table2sqlobj(sqlMeta, null, tableSqlFor(fields), false);

        Field2SQLObj.field2sqlObj(sqlMeta, fields, fields, false, dsMap, List.of(), null, null);
        List<ChartSortFieldDTO> sortFields = List.of(
                sortField(findField(fields, "f_release_day"), "asc"),
                sortField(findField(fields, "f_dev_workload"), "desc"),
                sortField(findField(fields, "f_req_final_days"), "desc")
        );
        Order2SQLObj.getOrders(sqlMeta, sortFields, fields, false, dsMap, List.of(), null, null);
        String generatedSql = SQLProvider.createQuerySQLWithLimit(sqlMeta, false, true, false, 0, 1000);
        String rebuiltSql = provider.rebuildSQL(generatedSql, sqlMeta, false, dsMap, true);

        assertFalse(rebuiltSql.contains("DE_"), rebuiltSql);
        assertTrue(rebuiltSql.contains("ORDER BY"), rebuiltSql);
        assertTrue(rebuiltSql.contains("LIMIT 1000 OFFSET 0"), rebuiltSql);
        for (DatasetTableFieldDTO field : convertedFields(fields)) {
            assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`" + field.getEngineFieldName() + "`");
        }
        assertTrue(rebuiltSql.contains("STR_TO_DATE(CASE WHEN `t_a_0`.`f_release_day` IS NULL THEN NULL"), rebuiltSql);
        assertTrue(rebuiltSql.contains("CAST(CASE WHEN `t_a_0`.`f_dev_workload` IS NULL THEN NULL"), rebuiltSql);
        assertTrue(rebuiltSql.contains("CAST(CASE WHEN `t_a_0`.`f_req_final_days` IS NULL THEN NULL"), rebuiltSql);
    }

    @Test
    void chartSqlNormalizesWideExcelFieldsAcrossDimensionsMetricsFiltersAndSorts() {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");
        Map<Long, DatasourceSchemaDTO> dsMap = Map.of(1L, datasource);
        List<DatasetTableFieldDTO> originFields = representativeExcelConversionFields();
        SQLMeta sqlMeta = new SQLMeta();
        Table2SQLObj.table2sqlobj(sqlMeta, null, tableSqlFor(originFields), false);

        ChartViewFieldDTO monthDimension = chartField(findField(originFields, "f_release_day"));
        monthDimension.setDateStyle("y_M");
        monthDimension.setDatePattern("-");
        monthDimension.setSort("asc");
        ChartViewFieldDTO departmentDimension = chartField(findField(originFields, "f_department"));
        ChartViewFieldDTO finalDaysMetric = chartField(findField(originFields, "f_req_final_days"));
        finalDaysMetric.setSummary("avg");
        finalDaysMetric.setFilter(List.of(chartFilter("gt", "30")));
        ChartViewFieldDTO workloadMetric = chartField(findField(originFields, "f_dev_workload"));
        workloadMetric.setSummary("sum");
        workloadMetric.setFilter(List.of(chartFilter("in", "10.5,20")));
        ChartViewFieldDTO releaseCountMetric = chartField(findField(originFields, "f_release_count"));
        releaseCountMetric.setSummary("sum");

        Dimension2SQLObj.dimension2sqlObj(sqlMeta, List.of(monthDimension, departmentDimension), originFields, false, dsMap, List.of(), null, null);
        Quota2SQLObj.quota2sqlObj(sqlMeta, List.of(finalDaysMetric, workloadMetric, releaseCountMetric), originFields, false, dsMap, List.of(), null, null);
        ChartViewDTO view = new ChartViewDTO();
        view.setType("bar");
        view.setResultMode("all");
        String chartSql = SQLProvider.createQuerySQL(sqlMeta, true, true, view);
        String rebuiltSql = provider.rebuildSQL(chartSql, sqlMeta, false, dsMap, true);

        assertFalse(rebuiltSql.contains("DE_"), rebuiltSql);
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_release_day`");
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_req_final_days`");
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_dev_workload`");
        assertContainsNormalizedTextOrigin(rebuiltSql, "`t_a_0`.`f_release_count`");
        assertTrue(rebuiltSql.contains("ORDER BY"), rebuiltSql);
        assertTrue(rebuiltSql.contains("`f_ax_0`"), rebuiltSql);
        assertTrue(rebuiltSql.contains("`f_ay_0` > 30"), rebuiltSql);
        assertTrue(rebuiltSql.contains("`f_ay_1` IN (10.5, 20)"), rebuiltSql);
    }

    @Test
    void chartSqlNormalizesTextPlaceholdersBeforeDateBucketingAndMetricAggregation() {
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");
        Map<Long, DatasourceSchemaDTO> dsMap = Map.of(1L, datasource);
        SQLMeta sqlMeta = new SQLMeta();
        Table2SQLObj.table2sqlobj(
                sqlMeta,
                null,
                "(SELECT `f_release_day`, `f_final_days` FROM `excel_table`)",
                false
        );
        ChartViewFieldDTO monthDimension = chartField(
                1L,
                "项目版本投产时间",
                "f_release_day",
                FieldTypeConstants.TIME,
                FieldTypeConstants.STRING,
                "yyyy/MM/dd"
        );
        monthDimension.setDateStyle("y_M");
        monthDimension.setDatePattern("-");
        ChartViewFieldDTO finalDaysMetric = chartField(
                2L,
                "需求提出到项目终投产天数",
                "f_final_days",
                FieldTypeConstants.INTEGER,
                FieldTypeConstants.STRING,
                null
        );
        finalDaysMetric.setSummary("avg");

        Dimension2SQLObj.dimension2sqlObj(sqlMeta, List.of(monthDimension), List.of(monthDimension, finalDaysMetric), false, dsMap, List.of(), null, null);
        Quota2SQLObj.quota2sqlObj(sqlMeta, List.of(finalDaysMetric), List.of(monthDimension, finalDaysMetric), false, dsMap, List.of(), null, null);
        String dimensionSql = sqlMeta.getXFields().get(0).getFieldName();
        String metricSql = sqlMeta.getYFields().get(0).getFieldName();

        assertContainsNormalizedTextOrigin(dimensionSql, "t_a_0.`f_release_day`");
        assertContainsNormalizedTextOrigin(metricSql, "t_a_0.`f_final_days`");
        assertTrue(dimensionSql.contains("DE_STR_TO_DATE(CASE WHEN t_a_0.`f_release_day` IS NULL THEN NULL"), dimensionSql);
        assertTrue(metricSql.toUpperCase(java.util.Locale.ROOT).contains("AVG(CAST(CASE WHEN T_A_0.`F_FINAL_DAYS` IS NULL THEN NULL"), metricSql);
        assertTrue(metricSql.contains("DECIMAL(19,0)"), metricSql);
    }

    @ParameterizedTest
    @MethodSource("nullableExcelConversionCandidates")
    void fieldGeneratedConversionsNormalizeRepresentativeExcelFields(
            String fieldName,
            String engineFieldName,
            int fieldType,
            String dateFormat,
            String expectedSqlPrefix
    ) {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");
        Map<Long, DatasourceSchemaDTO> dsMap = Map.of(1L, datasource);
        SQLMeta sqlMeta = new SQLMeta();
        Table2SQLObj.table2sqlobj(
                sqlMeta,
                null,
                "(SELECT `" + engineFieldName + "` FROM `excel_table`)",
                false
        );
        DatasetTableFieldDTO field = field(1L, fieldName, engineFieldName, fieldType, FieldTypeConstants.STRING, dateFormat);

        Field2SQLObj.field2sqlObj(sqlMeta, List.of(field), List.of(field), false, dsMap, List.of(), null, null);
        String generatedSql = SQLProvider.createQuerySQL(sqlMeta, false, false, false);
        String rebuiltSql = provider.rebuildSQL(generatedSql, sqlMeta, false, dsMap, true);
        String origin = "`t_a_0`.`" + engineFieldName + "`";

        assertFalse(rebuiltSql.contains("DE_"), rebuiltSql);
        assertContainsNormalizedTextOrigin(rebuiltSql, origin);
        assertTrue(rebuiltSql.contains(expectedSqlPrefix + "CASE WHEN " + origin + " IS NULL THEN NULL"), rebuiltSql);
    }

    @Test
    void explicitSortNormalizesConvertedTextBackedDateField() {
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setId(1L);
        datasource.setType("mysql");
        Map<Long, DatasourceSchemaDTO> dsMap = Map.of(1L, datasource);
        SQLMeta sqlMeta = new SQLMeta();
        Table2SQLObj.table2sqlobj(
                sqlMeta,
                null,
                "(SELECT `f_release_day` FROM `excel_table`)",
                false
        );
        DatasetTableFieldDTO originField = field(1L, "项目版本投产时间", "f_release_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd");
        ChartSortFieldDTO sortField = sortField(originField);
        sortField.setOrderDirection("asc");

        Order2SQLObj.getOrders(sqlMeta, List.of(sortField), List.of(originField), false, dsMap, List.of(), null, null);
        String orderAlias = sqlMeta.getXOrders().get(0).getOrderAlias();
        String origin = "`t_a_0`.`f_release_day`";
        String rebuiltSql = provider.rebuildSQL("SELECT * FROM `excel_table` ORDER BY " + orderAlias, sqlMeta, false, dsMap, true);

        assertContainsNormalizedTextOrigin(orderAlias, "t_a_0.`f_release_day`");
        assertFalse(rebuiltSql.contains("DE_"), rebuiltSql);
        assertTrue(rebuiltSql.contains("STR_TO_DATE(CASE WHEN `t_a_0`.`f_release_day` IS NULL THEN NULL"), rebuiltSql);
        assertContainsNormalizedTextOrigin(rebuiltSql, origin);
    }

    @Test
    void removesOnlyCompleteSchemaAliasPrefixes() {
        String sql = SqlPlaceholderUtils.removeIdentifierPrefix(
                "SELECT s_a_1.f_1, `s_a_1`.f_2, s_a_10.f_3 FROM s_a_1.t",
                "s_a_1"
        );

        assertEquals("SELECT f_1, f_2, s_a_10.f_3 FROM t", sql);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> nullableExcelConversionCandidates() {
        return Stream.of(
                arguments("项目版本投产时间", "f_release_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("项目版本提测时间", "f_test_day", FieldTypeConstants.TIME, "yyyy-MM-dd", "STR_TO_DATE("),
                arguments("项目首次投产日期", "f_first_release_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("项目最终投产日期", "f_final_release_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("验收测试报告提交时间", "f_acceptance_report_time", FieldTypeConstants.TIME, "yyyy/MM/dd HH:mm:ss", "STR_TO_DATE("),
                arguments("项目暂停时间", "f_project_pause_start", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("项目恢复时间", "f_project_pause_end", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("范围变更时间", "f_scope_change_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("立项通过时间", "f_project_approval_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("需求立项时间", "f_requirement_project_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("需求提出部门审批日期", "f_requirement_dept_approval_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("需求首次投产时间", "f_requirement_first_release_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("需求确认日期", "f_requirement_confirm_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("需求受理日期", "f_requirement_accept_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("需求分析完成日期", "f_requirement_analysis_done_day", FieldTypeConstants.TIME, "yyyy/MM/dd", "STR_TO_DATE("),
                arguments("开发预估工作量", "f_dev_workload", FieldTypeConstants.FLOAT, null, "CAST("),
                arguments("架构师评估工作量", "f_arch_workload", FieldTypeConstants.FLOAT, null, "CAST("),
                arguments("平均耗时", "f_avg_days", FieldTypeConstants.FLOAT, null, "CAST("),
                arguments("均值指标", "f_mean_value", FieldTypeConstants.FLOAT, null, "CAST("),
                arguments("阈值", "f_threshold", FieldTypeConstants.FLOAT, null, "CAST("),
                arguments("金额", "f_amount", FieldTypeConstants.FLOAT, null, "CAST("),
                arguments("业务审批耗时", "f_business_approval_days", FieldTypeConstants.FLOAT, null, "CAST("),
                arguments("暂停时长", "f_pause_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("项目暂停时长", "f_project_pause_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("需求提出到项目首投产天数", "f_req_first_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("需求提出到项目终投产天数", "f_req_final_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("需求确认到项目首投产天数", "f_confirm_first_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("需求确认到项目终投产天数", "f_confirm_final_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("需求受理到首次投产天数", "f_accept_first_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("需求受理到最终投产天数", "f_accept_final_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("需求分析完成到首次投产天数", "f_analysis_first_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("需求分析完成到最终投产天数", "f_analysis_final_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("需求首次投产到需求确认天数", "f_first_confirm_days", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("投产版本数", "f_release_count", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("超期数量", "f_overdue_count", FieldTypeConstants.INTEGER, null, "CAST("),
                arguments("序号", "f_row_no", FieldTypeConstants.INTEGER, null, "CAST(")
        );
    }

    private List<DatasetTableFieldDTO> representativeExcelConversionFields() {
        return List.of(
                field(1L, "项目编号", "f_project_code", FieldTypeConstants.STRING, FieldTypeConstants.STRING, null),
                field(2L, "需求编号", "f_requirement_code", FieldTypeConstants.STRING, FieldTypeConstants.STRING, null),
                field(3L, "归属处室", "f_department", FieldTypeConstants.STRING, FieldTypeConstants.STRING, null),
                field(4L, "项目版本投产时间", "f_release_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(5L, "项目版本提测时间", "f_test_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy-MM-dd"),
                field(6L, "项目首次投产日期", "f_first_release_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(7L, "项目最终投产日期", "f_final_release_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(8L, "验收测试报告提交时间", "f_acceptance_report_time", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd HH:mm:ss"),
                field(9L, "项目暂停时间", "f_project_pause_start", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(10L, "项目恢复时间", "f_project_pause_end", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(11L, "范围变更时间", "f_scope_change_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(12L, "立项通过时间", "f_project_approval_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(13L, "需求立项时间", "f_requirement_project_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(14L, "需求提出部门审批日期", "f_requirement_dept_approval_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(15L, "需求首次投产时间", "f_requirement_first_release_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(16L, "需求确认日期", "f_requirement_confirm_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(17L, "需求受理日期", "f_requirement_accept_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(18L, "需求分析完成日期", "f_requirement_analysis_done_day", FieldTypeConstants.TIME, FieldTypeConstants.STRING, "yyyy/MM/dd"),
                field(19L, "开发预估工作量", "f_dev_workload", FieldTypeConstants.FLOAT, FieldTypeConstants.STRING, null),
                field(20L, "架构师评估工作量", "f_arch_workload", FieldTypeConstants.FLOAT, FieldTypeConstants.STRING, null),
                field(21L, "平均耗时", "f_avg_days", FieldTypeConstants.FLOAT, FieldTypeConstants.STRING, null),
                field(22L, "阈值", "f_threshold", FieldTypeConstants.FLOAT, FieldTypeConstants.STRING, null),
                field(23L, "金额", "f_amount", FieldTypeConstants.FLOAT, FieldTypeConstants.STRING, null),
                field(24L, "业务审批耗时", "f_business_approval_days", FieldTypeConstants.FLOAT, FieldTypeConstants.STRING, null),
                field(25L, "暂停时长", "f_pause_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(26L, "项目暂停时长", "f_project_pause_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(27L, "需求提出到项目首投产天数", "f_req_first_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(28L, "需求提出到项目终投产天数", "f_req_final_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(29L, "需求确认到项目首投产天数", "f_confirm_first_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(30L, "需求确认到项目终投产天数", "f_confirm_final_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(31L, "需求受理到首次投产天数", "f_accept_first_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(32L, "需求受理到最终投产天数", "f_accept_final_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(33L, "需求分析完成到首次投产天数", "f_analysis_first_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(34L, "需求分析完成到最终投产天数", "f_analysis_final_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(35L, "需求首次投产到需求确认天数", "f_first_confirm_days", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(36L, "投产版本数", "f_release_count", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null),
                field(37L, "超期数量", "f_overdue_count", FieldTypeConstants.INTEGER, FieldTypeConstants.STRING, null)
        );
    }

    private List<DatasetTableFieldDTO> convertedFields(List<DatasetTableFieldDTO> fields) {
        return fields.stream()
                .filter(field -> !FieldTypeConstants.STRING.equals(field.getFieldType()))
                .toList();
    }

    private String tableSqlFor(List<DatasetTableFieldDTO> fields) {
        String selectFields = fields.stream()
                .map(field -> "`" + field.getEngineFieldName() + "`")
                .collect(Collectors.joining(", "));
        return "(SELECT " + selectFields + " FROM `excel_table`)";
    }

    private DatasetTableFieldDTO findField(List<DatasetTableFieldDTO> fields, String engineFieldName) {
        return fields.stream()
                .filter(field -> engineFieldName.equals(field.getEngineFieldName()))
                .findFirst()
                .orElseThrow();
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

    private ChartSortFieldDTO sortField(DatasetTableFieldDTO source) {
        return sortField(source, "asc");
    }

    private ChartSortFieldDTO sortField(DatasetTableFieldDTO source, String direction) {
        ChartSortFieldDTO field = new ChartSortFieldDTO();
        field.setId(source.getId());
        field.setName(source.getName());
        field.setOriginName(source.getOriginName());
        field.setDbFieldName(source.getDbFieldName());
        field.setEngineFieldName(source.getEngineFieldName());
        field.setType(source.getType());
        field.setFieldType(source.getFieldType());
        field.setExtractedFieldType(source.getExtractedFieldType());
        field.setDateFormat(source.getDateFormat());
        field.setOrderDirection(direction);
        return field;
    }

    private ChartViewFieldDTO chartField(DatasetTableFieldDTO source) {
        return chartField(
                source.getId(),
                source.getName(),
                source.getEngineFieldName(),
                source.getFieldType(),
                source.getExtractedFieldType(),
                source.getDateFormat()
        );
    }

    private ChartViewFieldDTO chartField(Long id, String name, String engineFieldName, int fieldType, int extractedFieldType, String dateFormat) {
        ChartViewFieldDTO field = new ChartViewFieldDTO();
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

    private ChartViewFieldFilterDTO chartFilter(String term, String value) {
        ChartViewFieldFilterDTO filter = new ChartViewFieldFilterDTO();
        filter.setTerm(term);
        filter.setValue(value);
        return filter;
    }

    private void assertContainsNormalizedTextOrigin(String sql, String origin) {
        assertTrue(sql.contains("CASE WHEN " + origin + " IS NULL THEN NULL"), sql);
        assertTrue(
                sql.contains("WHEN LOWER(TRIM(" + origin + ")) IN ('', 'null', '\\\\n') THEN NULL")
                        || sql.contains("WHEN LOWER(TRIM(BOTH ' ' FROM " + origin + ")) IN ('', 'null', '\\\\n') THEN NULL"),
                sql
        );
        assertTrue(
                sql.contains("ELSE TRIM(" + origin + ") END")
                        || sql.contains("ELSE TRIM(BOTH ' ' FROM " + origin + ") END"),
                sql
        );
    }
}
