package io.crest.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ObOracleReservedIdentifierInterceptorTest {

    @Test
    void rewritesFavoriteResourceReservedColumns() {
        ObOracleReservedIdentifierInterceptor interceptor = new ObOracleReservedIdentifierInterceptor(true);

        String rewrittenSql = ReflectionTestUtils.invokeMethod(interceptor, "rewrite",
                "select s.uid, s.time from workspace_favorite_resource s where s.uid = ?");

        assertThat(rewrittenSql)
                .isEqualTo("select s.\"UID\", s.\"TIME\" from workspace_favorite_resource s where s.\"UID\" = ?");
    }

    @Test
    void leavesQuotedStringLiteralsUntouched() {
        ObOracleReservedIdentifierInterceptor interceptor = new ObOracleReservedIdentifierInterceptor(true);

        String rewrittenSql = ReflectionTestUtils.invokeMethod(interceptor, "rewrite",
                "select uid from iam_user_role where name = 'uid mode time'");

        assertThat(rewrittenSql)
                .isEqualTo("select \"UID\" from iam_user_role where name = 'uid mode time'");
    }
}
