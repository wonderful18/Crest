package io.crest.datasource.security;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void validateReadQueryShouldAllowReadOnlyStatements() {
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateReadQuery("select * from demo_table where id = ?"));
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateReadQuery("with base as (select 1 as id) select * from base"));
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateReadQuery("desc demo_table"));
    }

    @Test
    void validateReadQueryShouldReturnNormalizedStatement() {
        assertEquals("select * from demo_table", SqlSecurityPolicy.validateReadQuery("  select * from demo_table  "));
    }

    @Test
    void validateReadQueryShouldRejectWriteAndDdlStatements() {
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("update demo_table set name = 'x'"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("delete from demo_table"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("drop table demo_table"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("select 1; update demo_table set name = 'x'"));
    }

    @Test
    void validateReadQueryShouldAllowUnionForDatasetGeneration() {
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateReadQuery(
                "select id from a union all select id from b"));
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateReadQuery(
                "with base as (select 1 as id) select * from base union select 2"));
    }

    @Test
    void validateReadQueryShouldRejectWritePrimitivesAndFileOut() {
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("grant all on db.* to 'u'@'%'"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("truncate table demo_table"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("call proc()"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("exec proc"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("merge into t using s on t.id = s.id when matched then update set t.v = s.v"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("select * from t where id = 1 for update"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("select * from t into outfile '/tmp/x'"));
        assertThrows(CrestException.class, () -> SqlSecurityPolicy.validateReadQuery("select * from t into dumpfile '/tmp/x'"));
    }

    @Test
    void validateReadQueryShouldNotRejectCreateTimeColumn() {
        assertDoesNotThrow(() -> SqlSecurityPolicy.validateReadQuery(
                "select create_time, update_at from demo_table where id = ?"));
    }
}
