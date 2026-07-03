package io.crest.datasource.security;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlSecurityPolicyTest {

    @Test
    void validateSingleStatementAllowsSemicolonInsideStringsIdentifiersAndComments() {
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateSingleStatement(
                "select concat(case when metric_count > 0 then '标签A;' else '' end, '标签B;') as metric_label"
        ));
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateSingleStatement(
                "select `field;name` from table_a /* ; not a statement */ where name <> ';'"
        ));
    }

    @Test
    void validateSingleStatementRejectsExecutableSqlAfterTopLevelSemicolon() {
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateSingleStatement("select 1; select 2"));
    }

    @Test
    void validateReadQueryAllowsReadOnlyStatements() {
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateReadQuery("select * from demo_table where id = ?"));
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateReadQuery("with base as (select 1 as id) select * from base"));
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateReadQuery("desc demo_table"));
    }

    @Test
    void validateReadQueryRejectsWriteAndDdlStatements() {
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("update demo_table set name = 'x'"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("delete from demo_table"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("drop table demo_table"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("select 1; update demo_table set name = 'x'"));
    }
}
