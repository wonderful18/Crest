package io.crest.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataDbEnvironmentPostProcessorTest {

    private final MetadataDbEnvironmentPostProcessor processor = new MetadataDbEnvironmentPostProcessor();

    @Test
    void mysqlDefaultsDoNotChangeExistingDeploymentUrl() {
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("CREST_DB_TYPE")).isEqualTo("mysql");
        assertThat(environment.getProperty("CREST_DB_DRIVER_CLASS_NAME"))
                .isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(environment.getProperty("CREST_FLYWAY_LOCATIONS"))
                .isEqualTo("classpath:db/migration,classpath:db/demo-migration");
        assertThat(environment.getProperty("CREST_DB_URL")).isNull();
    }

    @Test
    void explicitOceanBaseUrlKeepsHigherPriorityThanGeneratedDefault() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_DB_TYPE", "ob-oracle")
                .withProperty("CREST_DB_URL", "jdbc:oceanbase://obproxy.example.com:2883");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("CREST_DB_URL"))
                .isEqualTo("jdbc:oceanbase://obproxy.example.com:2883");
        assertThat(environment.getProperty("CREST_DB_DRIVER_CLASS_NAME"))
                .isEqualTo("com.oceanbase.jdbc.Driver");
        assertThat(environment.getProperty("CREST_FLYWAY_LOCATIONS"))
                .isEqualTo("classpath:db/migration-ob-oracle");
    }

    @Test
    void obMysqlHasExplicitDefaultsWithoutUsingOracleMigration() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_DB_TYPE", "ob-mysql");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("CREST_DB_DRIVER_CLASS_NAME"))
                .isEqualTo("com.oceanbase.jdbc.Driver");
        assertThat(environment.getProperty("CREST_DB_URL"))
                .isEqualTo("jdbc:oceanbase://127.0.0.1:2883/crest");
        assertThat(environment.getProperty("CREST_FLYWAY_LOCATIONS"))
                .isEqualTo("classpath:db/migration,classpath:db/demo-migration");
    }

    @Test
    void disablingDemoDataKeepsMysqlMigrationOnly() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_LOAD_DEMO", "false");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("CREST_FLYWAY_LOCATIONS"))
                .isEqualTo("classpath:db/migration");
    }
}
