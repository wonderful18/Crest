package io.crest.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InitialSchemaSecurityTest {

    @Test
    void initialSchemaShouldNotShipTemplateAccessKey() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("db/migration/V1.5.0.1__initial_schema.sql")) {
            assertThat(inputStream).isNotNull();
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql).doesNotContain("'template.accessKey','crest'");
        }
    }

    @Test
    void initialSchemaShouldGenerateAssociationMenuPermissions() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("db/migration/V1.5.0.1__initial_schema.sql")) {
            assertThat(inputStream).isNotNull();
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql).contains("(66,0,2,'association'");
            assertThat(sql).contains("SELECT id * 100 + 1, 1, id, 'manage'");
            assertThat(sql).contains("SELECT id * 100 + 2, 2, id, 'read'");
            assertThat(sql).contains("SELECT id * 100 + 3, 3, id, 'read'");
        }
    }
}
