package io.crest.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrestRuntimeEnvironmentPostProcessorTest {

    private final CrestRuntimeEnvironmentPostProcessor processor = new CrestRuntimeEnvironmentPostProcessor();

    @Test
    void defaultsKeepAllInOneDeploymentCompatible() {
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("CREST_RUNTIME_ROLE")).isEqualTo("all");
        assertThat(environment.getProperty("crest.runtime.role")).isEqualTo("all");
        assertThat(environment.getProperty("crest.production-mode")).isEqualTo("false");
        assertThat(environment.getProperty("spring.cache.type")).isNull();
    }

    @Test
    void apiRoleDisablesQuartzAutoStartupByDefault() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_RUNTIME_ROLE", "api");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.quartz.auto-startup")).isEqualTo("false");
    }

    @Test
    void apiRoleForcesQuartzAutoStartupDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_RUNTIME_ROLE", "api")
                .withProperty("spring.quartz.auto-startup", "true");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.quartz.auto-startup")).isEqualTo("false");
    }

    @Test
    void productionModeAddsSafeDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_PRODUCTION_MODE", "true")
                .withProperty("CREST_TOKEN_SECRET", "secret-value")
                .withProperty("spring.cache.type", "jcache");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.cache.type")).isEqualTo("redis");
        assertThat(environment.getProperty("management.health.redis.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("CREST_FLYWAY_ENABLED")).isEqualTo("false");
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("spring.quartz.jdbc.initialize-schema")).isEqualTo("never");
        assertThat(environment.getProperty("CREST_LOAD_DEMO")).isEqualTo("false");
        assertThat(environment.getProperty("crest.demo.enabled")).isEqualTo("false");
    }

    @Test
    void productionModeForcesFlywayDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_PRODUCTION_MODE", "true")
                .withProperty("CREST_TOKEN_SECRET", "secret-value")
                .withProperty("CREST_FLYWAY_ENABLED", "true");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("CREST_FLYWAY_ENABLED")).isEqualTo("false");
    }

    @Test
    void productionModeRequiresTokenSecret() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_PRODUCTION_MODE", "true");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CREST_TOKEN_SECRET");
    }

    @Test
    void redisClusterNodesCanBeConfiguredWithCrestEnvironmentKey() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_REDIS_CLUSTER_NODES", "redis-0:6379,redis-1:6379")
                .withProperty("CREST_REDIS_CLUSTER_MAX_REDIRECTS", "5")
                .withProperty("CREST_REDIS_USERNAME", "crest")
                .withProperty("CREST_REDIS_SSL_ENABLED", "true")
                .withProperty("CREST_REDIS_CONNECT_TIMEOUT", "2s")
                .withProperty("CREST_REDIS_TIMEOUT", "5s")
                .withProperty("CREST_REDIS_CLUSTER_REFRESH_ADAPTIVE", "true")
                .withProperty("CREST_REDIS_CLUSTER_REFRESH_PERIOD", "30s")
                .withProperty("CREST_REDIS_CLUSTER_DYNAMIC_REFRESH_SOURCES", "true");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.data.redis.cluster.nodes")).isEqualTo("redis-0:6379,redis-1:6379");
        assertThat(environment.getProperty("spring.data.redis.cluster.max-redirects")).isEqualTo("5");
        assertThat(environment.getProperty("spring.data.redis.username")).isEqualTo("crest");
        assertThat(environment.getProperty("spring.data.redis.ssl.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("spring.data.redis.connect-timeout")).isEqualTo("2s");
        assertThat(environment.getProperty("spring.data.redis.timeout")).isEqualTo("5s");
        assertThat(environment.getProperty("spring.data.redis.lettuce.cluster.refresh.adaptive")).isEqualTo("true");
        assertThat(environment.getProperty("spring.data.redis.lettuce.cluster.refresh.period")).isEqualTo("30s");
        assertThat(environment.getProperty("spring.data.redis.lettuce.cluster.refresh.dynamic-refresh-sources")).isEqualTo("true");
    }

    @Test
    void explicitSpringRedisClusterNodesKeepPriority() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_REDIS_CLUSTER_NODES", "crest-env:6379")
                .withProperty("spring.data.redis.cluster.nodes", "spring-env:6379");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.data.redis.cluster.nodes")).isEqualTo("spring-env:6379");
    }

    @Test
    void redisClusterRedirectsDoNotEnableClusterWithoutNodes() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_REDIS_CLUSTER_MAX_REDIRECTS", "3");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.data.redis.cluster.nodes")).isNull();
        assertThat(environment.getProperty("spring.data.redis.cluster.max-redirects")).isNull();
    }

    @Test
    void redisClusterRejectsNonZeroDatabase() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CREST_REDIS_CLUSTER_NODES", "redis-0:6379")
                .withProperty("CREST_REDIS_DATABASE", "1");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CREST_REDIS_DATABASE");
    }
}
