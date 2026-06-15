package io.crest.datasource.provider;

import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.provider.CrestSqlFunctionTranslator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// 覆盖 Crest 日期函数到 MySQL 兼容 SQL 的翻译规则。
public class CrestSqlFunctionTranslatorTest {

    // 验证指标聚合中的字符串日期转换会先翻译再参与类型转换。
    @Test
    public void mysqlTranslationRewritesStringDateConversionBeforeMetricCast() {
        String sql = "SELECT SUM(CAST(DE_STR_TO_DATE(t.`date_text`,'yyyy-MM-dd') AS DECIMAL(26,8))) AS f_ay_0 FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_STR_TO_DATE"));
        assertEquals(
                "SELECT SUM(CAST(STR_TO_DATE(t.`date_text`, '%Y-%m-%d') AS DECIMAL(26,8))) AS f_ay_0 FROM t",
                translated
        );
    }

    // 验证嵌套日期函数会被递归翻译。
    @Test
    public void mysqlTranslationRewritesNestedDateFunctions() {
        String sql = "SELECT DE_CAST_DATE_FORMAT(DE_STR_TO_DATE(t.`day`,'yyyy-MM-dd HH:mm:ss'),'yyyy-MM-dd HH:mm:ss','yyyy-MM') AS month_key FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_CAST_DATE_FORMAT"));
        assertFalse(translated.contains("DE_STR_TO_DATE"));
        assertEquals(
                "SELECT DATE_FORMAT(STR_TO_DATE(t.`day`, '%Y-%m-%d %H:%i:%s'), '%Y-%m') AS month_key FROM t",
                translated
        );
    }

    // 验证小写函数名、空白和 NULL 日期格式化都能翻译。
    @Test
    public void mysqlTranslationHandlesLowercaseWhitespaceAndNullDateFormatting() {
        String sql = "SELECT de_cast_date_format ( de_str_to_date ( t.`compact_day` , 'yyyyMMdd' ) , 'yyyy-MM-dd HH:mm:ss' , 'yyyy/MM' ) AS month_key, "
                + "de_date_format(NULL, 'yyyy-MM') AS empty_month FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("de_cast_date_format"));
        assertFalse(translated.contains("de_str_to_date"));
        assertEquals(
                "SELECT DATE_FORMAT(STR_TO_DATE(t.`compact_day`, '%Y%m%d'), '%Y/%m') AS month_key, "
                        + "DATE_FORMAT(NULL, '%Y-%m') AS empty_month FROM t",
                translated
        );
    }

    // 验证字符串字面量中的函数文本保持不变。
    @Test
    public void mysqlTranslationKeepsQuotedTextUnchanged() {
        String sql = "SELECT 'DE_STR_TO_DATE(t,''yyyy-MM-dd'')' AS label, DE_UNIX_TIMESTAMP(t.`day`) AS ts FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertEquals(
                "SELECT 'DE_STR_TO_DATE(t,''yyyy-MM-dd'')' AS label, UNIX_TIMESTAMP(t.`day`) AS ts FROM t",
                translated
        );
    }

    // 验证 NULL 值和多种日期格式都能转换为 MySQL 格式。
    @Test
    public void mysqlTranslationCoversNullValuesAndMultipleDateFormats() {
        String sql = "SELECT DE_STR_TO_DATE(NULL,'yyyyMMdd') AS empty_day, "
                + "DE_STR_TO_DATE(t.`slash_day`,'yyyy/MM/dd') AS slash_day, "
                + "DE_FROM_UNIXTIME(t.`epoch_seconds`,'yyyy-MM-dd HH:mm:ss') AS from_epoch "
                + "FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_STR_TO_DATE"));
        assertFalse(translated.contains("DE_FROM_UNIXTIME"));
        assertEquals(
                "SELECT STR_TO_DATE(NULL, '%Y%m%d') AS empty_day, "
                        + "STR_TO_DATE(t.`slash_day`, '%Y/%m/%d') AS slash_day, "
                        + "FROM_UNIXTIME(t.`epoch_seconds`, '%Y-%m-%d %H:%i:%s') AS from_epoch "
                        + "FROM t",
                translated
        );
    }

    // 验证 SELECT、WHERE 和 ORDER BY 中的函数都会翻译。
    @Test
    public void mysqlTranslationRewritesFunctionsInSelectWhereAndOrder() {
        String sql = "SELECT DE_DATE_FORMAT(DE_STR_TO_DATE(t.`day`,'yyyy/MM/dd'),'yyyy-MM') AS month_key "
                + "FROM t "
                + "WHERE COALESCE(DE_STR_TO_DATE(t.`day`,'yyyy/MM/dd'), NULL) IS NOT NULL "
                + "ORDER BY DE_UNIX_TIMESTAMP(DE_STR_TO_DATE(t.`day`,'yyyy/MM/dd')) DESC";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_"));
        assertEquals(
                "SELECT DATE_FORMAT(STR_TO_DATE(t.`day`, '%Y/%m/%d'), '%Y-%m') AS month_key "
                        + "FROM t "
                        + "WHERE COALESCE(STR_TO_DATE(t.`day`, '%Y/%m/%d'), NULL) IS NOT NULL "
                        + "ORDER BY UNIX_TIMESTAMP(STR_TO_DATE(t.`day`, '%Y/%m/%d')) DESC",
                translated
        );
    }

    // 验证宽表达式中的空值和嵌套函数翻译。
    @Test
    public void mysqlTranslationRewritesWideCalculatedExpressionsWithNullsAndNestedFunctions() {
        String sql = "SELECT "
                + "SUM(CASE WHEN DE_STR_TO_DATE(t.`actual_day`,'yyyy/MM/dd') IS NULL THEN 0 "
                + "ELSE CAST(DE_UNIX_TIMESTAMP(DE_STR_TO_DATE(t.`actual_day`,'yyyy/MM/dd')) AS DECIMAL(26,8)) END) AS risk_score, "
                + "COUNT(CASE WHEN DE_DATE_FORMAT(DE_STR_TO_DATE(t.`month_text`,'yyyyMM'),'yyyy-MM') = '2026-06' THEN 1 END) AS month_cnt, "
                + "DE_FROM_UNIXTIME(NULL) AS empty_epoch "
                + "FROM t "
                + "WHERE DE_CAST_DATE_FORMAT(COALESCE(t.`finish_day`, t.`start_day`),'yyyy-MM-dd HH:mm:ss','yyyy-MM-dd') IS NOT NULL";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_"));
        assertEquals(
                "SELECT "
                        + "SUM(CASE WHEN STR_TO_DATE(t.`actual_day`, '%Y/%m/%d') IS NULL THEN 0 "
                        + "ELSE CAST(UNIX_TIMESTAMP(STR_TO_DATE(t.`actual_day`, '%Y/%m/%d')) AS DECIMAL(26,8)) END) AS risk_score, "
                        + "COUNT(CASE WHEN DATE_FORMAT(STR_TO_DATE(t.`month_text`, '%Y%m'), '%Y-%m') = '2026-06' THEN 1 END) AS month_cnt, "
                        + "FROM_UNIXTIME(NULL) AS empty_epoch "
                        + "FROM t "
                        + "WHERE DATE_FORMAT(COALESCE(t.`finish_day`, t.`start_day`), '%Y-%m-%d') IS NOT NULL",
                translated
        );
    }

    // 验证中文字段名和 NULLIF 表达式中的日期函数翻译。
    @Test
    public void mysqlTranslationHandlesNullIfEmptyStringsAndChineseFieldNames() {
        String sql = "SELECT "
                + "DE_STR_TO_DATE(NULLIF(t.`投产日期`, ''),'yyyy/MM/dd') AS release_day, "
                + "DE_DATE_FORMAT(DE_STR_TO_DATE(COALESCE(t.`投产月份`, NULL),'yyyyMM'),'yyyy-MM') AS release_month, "
                + "CASE WHEN DE_STR_TO_DATE(NULLIF(t.`业务审批耗时(天)`, ''),'yyyy-MM-dd') IS NULL THEN NULL ELSE t.`需求数` END AS checked_value "
                + "FROM t "
                + "WHERE DE_STR_TO_DATE(NULLIF(t.`投产日期`, ''),'yyyy/MM/dd') IS NOT NULL";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_"));
        assertEquals(
                "SELECT "
                        + "STR_TO_DATE(NULLIF(t.`投产日期`, ''), '%Y/%m/%d') AS release_day, "
                        + "DATE_FORMAT(STR_TO_DATE(COALESCE(t.`投产月份`, NULL), '%Y%m'), '%Y-%m') AS release_month, "
                        + "CASE WHEN STR_TO_DATE(NULLIF(t.`业务审批耗时(天)`, ''), '%Y-%m-%d') IS NULL THEN NULL ELSE t.`需求数` END AS checked_value "
                        + "FROM t "
                        + "WHERE STR_TO_DATE(NULLIF(t.`投产日期`, ''), '%Y/%m/%d') IS NOT NULL",
                translated
        );
    }

    // 验证带引号分隔符的日期格式和指标类型转换。
    @Test
    public void mysqlTranslationHandlesQuotedDateFormatSeparatorsAndMetricCasts() {
        String sql = "SELECT "
                + "CAST(DE_UNIX_TIMESTAMP(DE_STR_TO_DATE(NULLIF(t.`日期文本`, ''),'yyyy-MM-dd''T''HH:mm:ss')) AS DECIMAL(26,8)) AS day_score, "
                + "DE_FROM_UNIXTIME(NULL,'yyyy-MM-dd HH:mm:ss') AS empty_epoch "
                + "FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_"));
        assertEquals(
                "SELECT "
                        + "CAST(UNIX_TIMESTAMP(STR_TO_DATE(NULLIF(t.`日期文本`, ''), '%Y-%m-%d''T''%H:%i:%s')) AS DECIMAL(26,8)) AS day_score, "
                        + "FROM_UNIXTIME(NULL, '%Y-%m-%d %H:%i:%s') AS empty_epoch "
                        + "FROM t",
                translated
        );
    }

    // 验证 Excel 数据源中的空值占位符转换表达式。
    @Test
    public void mysqlTranslationHandlesNullPlaceholdersInExcelBackedConversionExpressions() {
        String sql = "SELECT "
                + "DE_STR_TO_DATE(NULLIF(NULLIF(t.`项目版本投产时间`, 'null'), '\\\\N'),'yyyy/MM/dd') AS release_day, "
                + "DE_DATE_FORMAT(DE_STR_TO_DATE(NULLIF(t.`项目版本投产时间`, ''),'yyyy/MM/dd'),'yyyy-MM') AS release_month, "
                + "SUM(CAST(NULLIF(NULLIF(t.`开发预估工作量`, 'null'), '') AS DECIMAL(26,8))) AS workload_sum "
                + "FROM t "
                + "WHERE DE_STR_TO_DATE(NULLIF(NULLIF(t.`项目版本投产时间`, 'NULL'), ''),'yyyy/MM/dd') IS NOT NULL";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_"));
        assertEquals(
                "SELECT "
                        + "STR_TO_DATE(NULLIF(NULLIF(t.`项目版本投产时间`, 'null'), '\\\\N'), '%Y/%m/%d') AS release_day, "
                        + "DATE_FORMAT(STR_TO_DATE(NULLIF(t.`项目版本投产时间`, ''), '%Y/%m/%d'), '%Y-%m') AS release_month, "
                        + "SUM(CAST(NULLIF(NULLIF(t.`开发预估工作量`, 'null'), '') AS DECIMAL(26,8))) AS workload_sum "
                        + "FROM t "
                        + "WHERE STR_TO_DATE(NULLIF(NULLIF(t.`项目版本投产时间`, 'NULL'), ''), '%Y/%m/%d') IS NOT NULL",
                translated
        );
    }

    // 验证宽需求数据集中的日期转换表达式。
    @Test
    public void mysqlTranslationHandlesWideRequirementDatasetConversionExpressions() {
        String sql = "SELECT "
                + "DE_DATE_FORMAT(DE_STR_TO_DATE(NULLIF(NULLIF(t.`f_version_release_day`, 'null'), '\\\\N'),'yyyy/MM/dd'),'yyyy-MM') AS release_month, "
                + "COUNT(DISTINCT t.`f_project_version`) AS version_count, "
                + "SUM(CAST(NULLIF(NULLIF(t.`f_dev_workload`, 'null'), '') AS DECIMAL(26,8))) AS workload_sum, "
                + "AVG(CAST(NULLIF(NULLIF(t.`f_req_to_project_final_days`, 'NULL'), '\\\\N') AS DECIMAL(26,8))) AS final_days_avg, "
                + "SUM(CASE WHEN CAST(NULLIF(t.`f_req_to_project_final_days`, '') AS DECIMAL(26,8)) > CAST(NULLIF(t.`f_threshold`, '') AS DECIMAL(26,8)) THEN 1 ELSE 0 END) AS risk_count "
                + "FROM t "
                + "WHERE DE_STR_TO_DATE(NULLIF(NULLIF(t.`f_version_release_day`, 'null'), '\\\\N'),'yyyy/MM/dd') IS NOT NULL "
                + "GROUP BY DE_DATE_FORMAT(DE_STR_TO_DATE(NULLIF(NULLIF(t.`f_version_release_day`, 'null'), '\\\\N'),'yyyy/MM/dd'),'yyyy-MM')";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_"));
        assertEquals(
                "SELECT "
                        + "DATE_FORMAT(STR_TO_DATE(NULLIF(NULLIF(t.`f_version_release_day`, 'null'), '\\\\N'), '%Y/%m/%d'), '%Y-%m') AS release_month, "
                        + "COUNT(DISTINCT t.`f_project_version`) AS version_count, "
                        + "SUM(CAST(NULLIF(NULLIF(t.`f_dev_workload`, 'null'), '') AS DECIMAL(26,8))) AS workload_sum, "
                        + "AVG(CAST(NULLIF(NULLIF(t.`f_req_to_project_final_days`, 'NULL'), '\\\\N') AS DECIMAL(26,8))) AS final_days_avg, "
                        + "SUM(CASE WHEN CAST(NULLIF(t.`f_req_to_project_final_days`, '') AS DECIMAL(26,8)) > CAST(NULLIF(t.`f_threshold`, '') AS DECIMAL(26,8)) THEN 1 ELSE 0 END) AS risk_count "
                        + "FROM t "
                        + "WHERE STR_TO_DATE(NULLIF(NULLIF(t.`f_version_release_day`, 'null'), '\\\\N'), '%Y/%m/%d') IS NOT NULL "
                        + "GROUP BY DATE_FORMAT(STR_TO_DATE(NULLIF(NULLIF(t.`f_version_release_day`, 'null'), '\\\\N'), '%Y/%m/%d'), '%Y-%m')",
                translated
        );
    }

    // 验证 MySQL 格式和反引号 schema 限定符处理。
    @Test
    public void mysqlTranslationPreservesMysqlFormatsAndHandlesBacktickSchemaQualifier() {
        String sql = "SELECT `crest`.de_str_to_date(t.`day`,'%Y-%m-%d') AS day_value, "
                + "DE_FROM_UNIXTIME(t.`epoch_seconds`) AS epoch_value FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertEquals(
                "SELECT STR_TO_DATE(t.`day`, '%Y-%m-%d') AS day_value, "
                        + "FROM_UNIXTIME(t.`epoch_seconds`) AS epoch_value FROM t",
                translated
        );
    }

    // 验证 Calcite 生成的反引号函数名可以被识别。
    @Test
    public void mysqlTranslationHandlesBacktickQuotedFunctionNamesProducedByCalcite() {
        String sql = "SELECT `DE_STR_TO_DATE`(t.`release_day`, 'yyyy/MM/dd') AS day_value, "
                + "`DE_DATE_FORMAT`(`DE_STR_TO_DATE`(t.`release_day`, 'yyyy/MM/dd'), 'yyyy-MM') AS month_value, "
                + "`crest`.`DE_UNIX_TIMESTAMP`(`DE_STR_TO_DATE`(t.`release_day`, 'yyyy/MM/dd')) AS ts_value FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertFalse(translated.contains("DE_"));
        assertEquals(
                "SELECT STR_TO_DATE(t.`release_day`, '%Y/%m/%d') AS day_value, "
                        + "DATE_FORMAT(STR_TO_DATE(t.`release_day`, '%Y/%m/%d'), '%Y-%m') AS month_value, "
                        + "UNIX_TIMESTAMP(STR_TO_DATE(t.`release_day`, '%Y/%m/%d')) AS ts_value FROM t",
                translated
        );
    }

    // 验证相似但不匹配的函数名不会被误改写。
    @Test
    public void mysqlTranslationDoesNotRewriteSimilarFunctionNames() {
        String sql = "SELECT MY_DE_STR_TO_DATE(t.`day`, 'yyyy-MM-dd') AS custom_value, "
                + "DE_STR_TO_DATE_SUFFIX(t.`day`, 'yyyy-MM-dd') AS suffix_value FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertEquals(sql, translated);
    }

    // 验证 Crest 函数的 schema 限定符会被移除。
    @Test
    public void mysqlTranslationDropsSchemaQualifierFromCrestFunctions() {
        String sql = "SELECT crest.DE_STR_TO_DATE(t.`day`,'yyyy/MM/dd') AS day_value FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertEquals(
                "SELECT STR_TO_DATE(t.`day`, '%Y/%m/%d') AS day_value FROM t",
                translated
        );
    }

    // 验证非 MySQL 数据源保留 Crest 函数给后续方言处理。
    @Test
    public void nonMysqlDatasourceKeepsCrestFunctionsForProviderSpecificHandling() {
        String sql = "SELECT DE_STR_TO_DATE(t.`day`,'yyyy/MM/dd') AS day_value FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("sqlServer"));

        assertEquals(sql, translated);
    }

    // 验证空 SQL 或空数据源会原样返回。
    @Test
    public void blankSqlOrDatasourceIsIgnored() {
        assertEquals("", CrestSqlFunctionTranslator.translate("", datasource("mysql")));
        String sql = "SELECT DE_STR_TO_DATE(t.`day`,'yyyy/MM/dd') FROM t";

        assertEquals(sql, CrestSqlFunctionTranslator.translate(sql, null));
    }

    // 构造指定类型的数据源元信息。
    private DatasourceSchemaDTO datasource(String type) {
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setType(type);
        return datasource;
    }
}
