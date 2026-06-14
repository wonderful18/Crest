package io.crest.datasource.provider;

import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.provider.CrestSqlFunctionTranslator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CrestSqlFunctionTranslatorTest {

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

    @Test
    public void mysqlTranslationKeepsQuotedTextUnchanged() {
        String sql = "SELECT 'DE_STR_TO_DATE(t,''yyyy-MM-dd'')' AS label, DE_UNIX_TIMESTAMP(t.`day`) AS ts FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertEquals(
                "SELECT 'DE_STR_TO_DATE(t,''yyyy-MM-dd'')' AS label, UNIX_TIMESTAMP(t.`day`) AS ts FROM t",
                translated
        );
    }

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

    @Test
    public void mysqlTranslationDoesNotRewriteSimilarFunctionNames() {
        String sql = "SELECT MY_DE_STR_TO_DATE(t.`day`, 'yyyy-MM-dd') AS custom_value, "
                + "DE_STR_TO_DATE_SUFFIX(t.`day`, 'yyyy-MM-dd') AS suffix_value FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertEquals(sql, translated);
    }

    @Test
    public void mysqlTranslationDropsSchemaQualifierFromCrestFunctions() {
        String sql = "SELECT crest.DE_STR_TO_DATE(t.`day`,'yyyy/MM/dd') AS day_value FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("mysql"));

        assertEquals(
                "SELECT STR_TO_DATE(t.`day`, '%Y/%m/%d') AS day_value FROM t",
                translated
        );
    }

    @Test
    public void nonMysqlDatasourceKeepsCrestFunctionsForProviderSpecificHandling() {
        String sql = "SELECT DE_STR_TO_DATE(t.`day`,'yyyy/MM/dd') AS day_value FROM t";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource("sqlServer"));

        assertEquals(sql, translated);
    }

    @Test
    public void blankSqlOrDatasourceIsIgnored() {
        assertEquals("", CrestSqlFunctionTranslator.translate("", datasource("mysql")));
        String sql = "SELECT DE_STR_TO_DATE(t.`day`,'yyyy/MM/dd') FROM t";

        assertEquals(sql, CrestSqlFunctionTranslator.translate(sql, null));
    }

    private DatasourceSchemaDTO datasource(String type) {
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setType(type);
        return datasource;
    }
}
