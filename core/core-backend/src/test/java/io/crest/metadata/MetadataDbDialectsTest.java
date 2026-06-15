package io.crest.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataDbDialectsTest {

    @Test
    void mysqlDialectKeepsCurrentSqlFragments() {
        MetadataDbDialect dialect = MetadataDbDialects.forType(MetadataDbType.MYSQL);

        assertThat(dialect.limitOffset("SELECT 1", 10, 20))
                .isEqualTo("SELECT 1 LIMIT 10 OFFSET 20");
        assertThat(dialect.caseInsensitiveLike("v.name", "{0}"))
                .isEqualTo("LOWER(v.name) LIKE LOWER(CONCAT('%', {0}, '%'))");
        assertThat(dialect.stringCast("id"))
                .isEqualTo("CAST(id AS CHAR) COLLATE utf8mb4_unicode_ci");
        assertThat(dialect.numberCast("resource_id"))
                .isEqualTo("CAST(resource_id AS UNSIGNED)");
        assertThat(dialect.insertIgnore("iam_user_role", "id, uid", "?, ?"))
                .isEqualTo("INSERT IGNORE INTO iam_user_role (id, uid) VALUES (?, ?)");
    }

    @Test
    void obMysqlHasIndependentTypeButKeepsMysqlCompatibleFragments() {
        MetadataDbDialect dialect = MetadataDbDialects.forType(MetadataDbType.OB_MYSQL);

        assertThat(dialect.type()).isEqualTo(MetadataDbType.OB_MYSQL);
        assertThat(dialect.limitOne("SELECT 1")).isEqualTo("SELECT 1 LIMIT 1 OFFSET 0");
        assertThat(dialect.currentTimestamp()).isEqualTo("NOW()");
    }

    @Test
    void obOracleDialectUsesOracleCompatibleSqlFragments() {
        MetadataDbDialect dialect = MetadataDbDialects.forType(MetadataDbType.OB_ORACLE);

        assertThat(dialect.limitOffset("SELECT 1", 10, 20))
                .isEqualTo("SELECT 1 OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY");
        assertThat(dialect.caseInsensitiveLike("v.name", "{0}"))
                .isEqualTo("LOWER(v.name) LIKE LOWER('%' || {0} || '%')");
        assertThat(dialect.stringCast("id")).isEqualTo("TO_CHAR(id)");
        assertThat(dialect.numberCast("resource_id")).isEqualTo("TO_NUMBER(resource_id)");
        assertThat(dialect.mergeIfMissing("iam_user_role", "id, \"UID\"", "?, ?",
                "\"UID\" = ?"))
                .isEqualTo("INSERT INTO iam_user_role (id, \"UID\") SELECT ?, ? FROM DUAL "
                        + "WHERE NOT EXISTS (SELECT 1 FROM iam_user_role WHERE \"UID\" = ?)");
    }

    @Test
    void databaseIdProviderUsesConfiguredMetadataDbType() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(MetadataDbTypeResolver.ENV_NAME, "ob-oracle");
        MetadataDatabaseIdProvider provider = new MetadataDatabaseIdProvider(environment);

        assertThat(provider.getDatabaseId(null)).isEqualTo("ob-oracle");
    }
}
