package io.crest.datasource.provider;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalciteProviderSecurityTest {

    @Test
    void oracleSchemaIdentifierQuotesSimpleSchema() throws Exception {
        assertEquals("\"S_A_1\"", oracleSchemaIdentifier("S_A_1"));
    }

    @Test
    void oracleSchemaIdentifierRejectsQualifiedOrQuotedSchema() throws Exception {
        assertThrows(CrestException.class, () -> oracleSchemaIdentifier("A.B"));
        assertThrows(CrestException.class, () -> oracleSchemaIdentifier("A\"B"));
    }

    private String oracleSchemaIdentifier(String schema) throws Exception {
        Method method = CalciteProvider.class.getDeclaredMethod("oracleSchemaIdentifier", String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(new CalciteProvider(), schema);
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof CrestException crestException) {
                throw crestException;
            }
            throw e;
        }
    }
}
