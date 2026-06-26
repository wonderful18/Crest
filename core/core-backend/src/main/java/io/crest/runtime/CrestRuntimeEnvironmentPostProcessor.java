package io.crest.runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class CrestRuntimeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "crestRuntimeDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new LinkedHashMap<>();
        CrestRuntimeRole role = CrestRuntimeRole.from(environment);
        boolean productionMode = booleanProperty(environment, "CREST_PRODUCTION_MODE", "crest.production-mode");

        properties.put("CREST_RUNTIME_ROLE", role.value());
        properties.put("crest.runtime.role", role.value());
        properties.put("crest.production-mode", Boolean.toString(productionMode));

        if (role == CrestRuntimeRole.API) {
            properties.put("spring.quartz.auto-startup", "false");
        }

        if (productionMode) {
            requireText(environment, "CREST_TOKEN_SECRET");
            properties.put("spring.cache.type", "redis");
            properties.put("management.health.redis.enabled", "true");
            properties.put("CREST_FLYWAY_ENABLED", "false");
            properties.put("spring.flyway.enabled", "false");
            properties.put("spring.quartz.jdbc.initialize-schema", "never");
            properties.put("CREST_LOAD_DEMO", "false");
            properties.put("crest.demo.enabled", "false");
        }

        addRedisClusterProperties(environment, properties);
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void addRedisClusterProperties(ConfigurableEnvironment environment, Map<String, Object> properties) {
        String clusterNodes = environment.getProperty("CREST_REDIS_CLUSTER_NODES");
        if (!StringUtils.hasText(clusterNodes)) {
            return;
        }

        String database = environment.getProperty("CREST_REDIS_DATABASE");
        if (StringUtils.hasText(database) && !"0".equals(database.trim())) {
            throw new IllegalStateException("CREST_REDIS_DATABASE must be 0 when CREST_REDIS_CLUSTER_NODES is configured");
        }

        putIfMissing(environment, properties, "spring.data.redis.cluster.nodes", clusterNodes);
        putIfMissing(
                environment,
                properties,
                "spring.data.redis.cluster.max-redirects",
                environment.getProperty("CREST_REDIS_CLUSTER_MAX_REDIRECTS")
        );
        putIfMissing(environment, properties, "spring.data.redis.username", environment.getProperty("CREST_REDIS_USERNAME"));
        putIfMissing(environment, properties, "spring.data.redis.ssl.enabled", environment.getProperty("CREST_REDIS_SSL_ENABLED"));
        putIfMissing(environment, properties, "spring.data.redis.connect-timeout", environment.getProperty("CREST_REDIS_CONNECT_TIMEOUT"));
        putIfMissing(environment, properties, "spring.data.redis.timeout", environment.getProperty("CREST_REDIS_TIMEOUT"));
        putIfMissing(
                environment,
                properties,
                "spring.data.redis.lettuce.cluster.refresh.adaptive",
                environment.getProperty("CREST_REDIS_CLUSTER_REFRESH_ADAPTIVE")
        );
        putIfMissing(
                environment,
                properties,
                "spring.data.redis.lettuce.cluster.refresh.period",
                environment.getProperty("CREST_REDIS_CLUSTER_REFRESH_PERIOD")
        );
        putIfMissing(
                environment,
                properties,
                "spring.data.redis.lettuce.cluster.refresh.dynamic-refresh-sources",
                environment.getProperty("CREST_REDIS_CLUSTER_DYNAMIC_REFRESH_SOURCES")
        );
    }

    private void putIfMissing(ConfigurableEnvironment environment, Map<String, Object> properties, String targetKey, String value) {
        if (!StringUtils.hasText(value) || StringUtils.hasText(environment.getProperty(targetKey))) {
            return;
        }
        properties.put(targetKey, value);
    }

    private boolean booleanProperty(ConfigurableEnvironment environment, String environmentKey, String propertyKey) {
        String value = firstText(environment.getProperty(environmentKey), environment.getProperty(propertyKey));
        return Boolean.parseBoolean(value);
    }

    private void requireText(ConfigurableEnvironment environment, String propertyName) {
        if (!StringUtils.hasText(environment.getProperty(propertyName))) {
            throw new IllegalStateException(propertyName + " is required when CREST_PRODUCTION_MODE=true");
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "false";
    }
}
