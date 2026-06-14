package io.crest.extensions.datasource.provider;

import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.utils.SqlPlaceholderUtils;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.constant.SqlPlaceholderConstants;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.parser.SqlParser;
import org.junit.jupiter.api.Test;

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
    void removesOnlyCompleteSchemaAliasPrefixes() {
        String sql = SqlPlaceholderUtils.removeIdentifierPrefix(
                "SELECT s_a_1.f_1, `s_a_1`.f_2, s_a_10.f_3 FROM s_a_1.t",
                "s_a_1"
        );

        assertEquals("SELECT f_1, f_2, s_a_10.f_3 FROM t", sql);
    }
}
