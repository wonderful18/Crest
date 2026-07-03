package io.crest.extensions.datasource.provider;

import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.utils.SqlPlaceholderUtils;
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

        if (officialSql.contains("u&'")) {
            assertTrue(sql.contains("'交易履约部'"));
            assertFalse(sql.contains("u&'"));
        } else {
            assertTrue(officialSql.contains("'交易履约部'"));
            assertEquals(officialSql, sql);
        }
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
    void removesOnlyCompleteSchemaAliasPrefixes() {
        String sql = SqlPlaceholderUtils.removeIdentifierPrefix(
                "SELECT s_a_1.f_1, `s_a_1`.f_2, s_a_10.f_3 FROM s_a_1.t",
                "s_a_1"
        );

        assertEquals("SELECT f_1, f_2, s_a_10.f_3 FROM t", sql);
    }
}
