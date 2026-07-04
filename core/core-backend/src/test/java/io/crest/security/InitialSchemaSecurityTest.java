package io.crest.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InitialSchemaSecurityTest {

    private static final List<String> INITIAL_SCHEMA_RESOURCES = List.of(
            "db/migration/V1.5.0.1__initial_schema.sql",
            "db/migration-ob-mysql/V1.5.0.1__initial_schema.sql",
            "db/migration-ob-oracle/V1.5.0.1__initial_schema.sql"
    );
    private static final List<String> V21_FULL_SCHEMA_FILES = List.of(
            "installer/upgrade-sql/v2.1.0_full_schema_mysql.sql",
            "installer/upgrade-sql/v2.1.0_full_schema_ob_mysql.sql",
            "installer/upgrade-sql/v2.1.0_full_schema_ob_oracle.sql"
    );
    private static final List<String> V203_UPGRADE_FILES = List.of(
            "installer/upgrade-sql/v2.0.3_to_v2.1.0_mysql.sql",
            "installer/upgrade-sql/v2.0.3_to_v2.1.0_ob_mysql.sql",
            "installer/upgrade-sql/v2.0.3_to_v2.1.0_ob_oracle.sql"
    );

    @Test
    void initialSchemasShouldNotShipTemplateAccessKey() throws IOException {
        for (String resource : INITIAL_SCHEMA_RESOURCES) {
            String sql = readResource(resource);

            assertDoesNotShipTemplateAccessKey(resource, sql);
            assertThat(sql)
                    .as(resource)
                    .contains("'template.accessKey','','text'");
        }
    }

    @Test
    void installerFullSchemasShouldNotShipTemplateAccessKey() throws IOException {
        for (String file : V21_FULL_SCHEMA_FILES) {
            String sql = readFile(file);

            assertDoesNotShipTemplateAccessKey(file, sql);
            assertThat(sql)
                    .as(file)
                    .contains("'template.accessKey','','text'");
        }
    }

    @Test
    void upgradeScriptsShouldClearOnlyHistoricalTemplateAccessKeyDefault() throws IOException {
        for (String file : V203_UPGRADE_FILES) {
            String sql = readFile(file);

            assertThat(sql)
                    .as(file)
                    .contains("SET pval = ''")
                    .contains("pkey = 'template.accessKey'");

            assertThat(sql)
                    .as(file)
                    .contains(file.endsWith("_ob_oracle.sql")
                            ? "DBMS_LOB.SUBSTR(pval, 4000, 1) = 'crest'"
                            : "pval = 'crest'");
        }

        String v1514Upgrade = readFile("installer/upgrade-sql/v1.5.14_to_v2.1.0_mysql.sql");
        assertDoesNotShipTemplateAccessKey("installer/upgrade-sql/v1.5.14_to_v2.1.0_mysql.sql", v1514Upgrade);
    }

    private String readResource(String resource) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(inputStream).as(resource).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String readFile(String path) throws IOException {
        Path sourcePath = Path.of(path);
        if (!Files.exists(sourcePath)) {
            sourcePath = Path.of("..", "..", path);
        }
        return Files.readString(sourcePath);
    }

    private void assertDoesNotShipTemplateAccessKey(String source, String sql) {
        assertThat(sql)
                .as(source)
                .doesNotContain("'template.accessKey','crest'")
                .doesNotContain("'template.accessKey', 'crest'");
    }
}
