package io.crest.datasource.security;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlSecurityPolicyTest {

    @Test
    void validateSingleStatementShouldAllowSemicolonInsideStringLiteral() {
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateSingleStatement(
                "select concat(case when metric_count > 0 then '标签A;' else '' end, '标签B;') as metric_label"
        ));
    }

    @Test
    void validateSingleStatementShouldAllowSemicolonInsideQuotedIdentifierAndComments() {
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateSingleStatement(
                "select `field;name` from table_a /* ; not a statement */ where name <> ';'"
        ));
    }

    @Test
    void validateSingleStatementShouldRejectExecutableSqlAfterTopLevelSemicolon() {
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateSingleStatement("select 1; select 2"));
    }
}
