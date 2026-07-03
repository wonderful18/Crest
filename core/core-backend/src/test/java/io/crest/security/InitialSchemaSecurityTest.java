package io.crest.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InitialSchemaSecurityTest {

    private static final List<String> INITIAL_SCHEMA_RESOURCES = List.of(
            "db/migration/V1.5.0.1__initial_schema.sql",
            "db/migration-ob-mysql/V1.5.0.1__initial_schema.sql",
            "db/migration-ob-oracle/V1.5.0.1__initial_schema.sql"
    );

    @Test
    void initialSchemasShouldNotShipTemplateAccessKey() throws IOException {
        for (String resource : INITIAL_SCHEMA_RESOURCES) {
            String sql = readResource(resource);

            assertThat(sql)
                    .as(resource)
                    .doesNotContain("'template.accessKey','crest'")
                    .doesNotContain("'template.accessKey', 'crest'");
            assertThat(sql)
                    .as(resource)
                    .contains("'template.accessKey','','text'");
        }
    }

    private String readResource(String resource) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(inputStream).as(resource).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
