package io.crest.datasource.provider;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalciteProviderSecurityTest {

    @Test
    void quoteOracleSchemaShouldQuoteConfiguredSchema() {
        assertEquals("\"S_A_1\"", CalciteProvider.quoteOracleSchema("S_A_1"));
    }

    @Test
    void quoteOracleSchemaShouldEscapeEmbeddedQuote() {
        assertEquals("\"A\"\"B\"", CalciteProvider.quoteOracleSchema("A\"B"));
    }

    @Test
    void quoteOracleSchemaShouldRejectQualifiedName() {
        assertThrows(CrestException.class, () -> CalciteProvider.quoteOracleSchema("A.B"));
    }
}
