package io.crest.runtime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CrestRuntimeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "crestRuntimeDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        CrestRuntimeRole role = CrestRuntimeRole.from(environment);
        boolean productionMode = productionMode(environment);

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("CREST_RUNTIME_ROLE", role.getCode());
        defaults.put("crest.runtime.role", role.getCode());
        defaults.put("CREST_PRODUCTION_MODE", String.valueOf(productionMode));
        defaults.put("crest.production-mode", String.valueOf(productionMode));
        applyRedisDefaults(environment, defaults);

        Map<String, Object> enforced = new LinkedHashMap<>();
        if (!role.runsScheduler()) {
            enforced.put("spring.quartz.auto-startup", "false");
        }

        if (productionMode) {
            enforced.put("spring.cache.type", "redis");
            enforced.put("management.health.redis.enabled", "true");
            enforced.put("CREST_FLYWAY_ENABLED", "false");
            enforced.put("spring.flyway.enabled", "false");
            enforced.put("CREST_LOAD_DEMO", "false");
            enforced.put("crest.demo.enabled", "false");
        }

        if (!enforced.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME + "Enforced", enforced));
        }
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
        validateRuntimeRole(environment, role);
        validateProductionMode(environment);
        validateRedisCluster(environment);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 20;
    }

    private boolean productionMode(ConfigurableEnvironment environment) {
        return environment.getProperty("crest.production-mode", Boolean.class,
                environment.getProperty("CREST_PRODUCTION_MODE", Boolean.class, false));
    }

    // 将 Crest 环境变量映射为 Spring Redis 配置，避免旧单节点部署被空值切换到 Cluster。
    private void applyRedisDefaults(ConfigurableEnvironment environment, Map<String, Object> defaults) {
        putIfNotBlank(defaults, "spring.data.redis.username", environment.getProperty("CREST_REDIS_USERNAME"));
        putIfNotBlank(defaults, "spring.data.redis.ssl.enabled", environment.getProperty("CREST_REDIS_SSL_ENABLED"));
        putIfNotBlank(defaults, "spring.data.redis.connect-timeout", environment.getProperty("CREST_REDIS_CONNECT_TIMEOUT"));
        putIfNotBlank(defaults, "spring.data.redis.timeout", environment.getProperty("CREST_REDIS_TIMEOUT"));
        String clusterNodes = environment.getProperty("CREST_REDIS_CLUSTER_NODES");
        if (StringUtils.isNotBlank(clusterNodes)) {
            putIfNotBlank(defaults, "spring.data.redis.cluster.nodes", clusterNodes);
            putIfNotBlank(defaults, "spring.data.redis.cluster.max-redirects",
                    environment.getProperty("CREST_REDIS_CLUSTER_MAX_REDIRECTS"));
            putIfNotBlank(defaults, "spring.data.redis.lettuce.cluster.refresh.adaptive",
                    environment.getProperty("CREST_REDIS_CLUSTER_REFRESH_ADAPTIVE"));
            putIfNotBlank(defaults, "spring.data.redis.lettuce.cluster.refresh.period",
                    environment.getProperty("CREST_REDIS_CLUSTER_REFRESH_PERIOD"));
            putIfNotBlank(defaults, "spring.data.redis.lettuce.cluster.refresh.dynamic-refresh-sources",
                    environment.getProperty("CREST_REDIS_CLUSTER_DYNAMIC_REFRESH_SOURCES"));
        }
    }

    private void validateRuntimeRole(ConfigurableEnvironment environment, CrestRuntimeRole role) {
        if (role.runsScheduler()) {
            return;
        }
        requireEquals(environment, "spring.quartz.auto-startup", "false",
                "when CREST_RUNTIME_ROLE does not run the scheduler");
    }

    private void validateProductionMode(ConfigurableEnvironment environment) {
        if (!productionMode(environment)) {
            return;
        }
        String productionReason = "when CREST_PRODUCTION_MODE=true";
        requireEquals(environment, "spring.cache.type", "redis", productionReason);
        requireEquals(environment, "spring.flyway.enabled", "false", productionReason);
        requireEquals(environment, "CREST_FLYWAY_ENABLED", "false", productionReason);
        requireEquals(environment, "crest.demo.enabled", "false", productionReason);
        requireEquals(environment, "CREST_LOAD_DEMO", "false", productionReason);
        requireEquals(environment, "management.health.redis.enabled", "true", productionReason);
        if (StringUtils.isBlank(environment.getProperty("crest.security.token-secret",
                environment.getProperty("CREST_TOKEN_SECRET")))) {
            throw new IllegalStateException("CREST_TOKEN_SECRET is required when CREST_PRODUCTION_MODE=true");
        }
    }

    // Redis Cluster 不支持业务侧切换逻辑库，配置错误时启动阶段直接失败。
    private void validateRedisCluster(ConfigurableEnvironment environment) {
        if (StringUtils.isBlank(environment.getProperty("spring.data.redis.cluster.nodes"))) {
            return;
        }
        String database = StringUtils.defaultIfBlank(environment.getProperty("spring.data.redis.database"),
                environment.getProperty("CREST_REDIS_DATABASE", "0"));
        if (!"0".equals(database.trim())) {
            throw new IllegalStateException("CREST_REDIS_DATABASE must be 0 when CREST_REDIS_CLUSTER_NODES is configured");
        }
    }

    private void requireEquals(ConfigurableEnvironment environment, String key, String expected, String reason) {
        String actual = environment.getProperty(key);
        if (!expected.equalsIgnoreCase(StringUtils.defaultString(actual).trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(key + " must be " + expected + " " + reason);
        }
    }

    // 空值不写入 Spring 配置，保留框架默认行为。
    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value.trim());
        }
    }
}
