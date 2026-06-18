package io.crest.home;

import io.crest.result.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用健康检查控制器，提供存活和就绪检查端点
 */
@RestController
@RequestMapping("/actuator/health")
public class HealthController {

    /**
     * 数据库连接有效性检查的超时时间
     */
    private static final int DATABASE_VALIDATION_TIMEOUT_SECONDS = 2;

    /**
     * 可选数据源，用于就绪检查数据库连通性
     */
    private final DataSource dataSource;
    private final Environment environment;
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * 创建健康检查控制器，数据源缺失时就绪检查会失败
     */
    public HealthController(@Autowired(required = false) DataSource dataSource,
                            Environment environment,
                            @Autowired(required = false) RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.environment = environment;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * 默认健康端点复用就绪检查结果
     */
    @GetMapping
    public ResponseEntity<ResultMessage> health() {
        return readiness();
    }

    /**
     * 返回 JVM 存活状态，不依赖外部资源
     */
    @GetMapping("/liveness")
    public ResponseEntity<ResultMessage> liveness() {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("jvm", "UP");
        return ResponseEntity.ok(ResultMessage.success(status("UP", checks)));
    }

    /**
     * 返回服务就绪状态，并检查数据库是否可连接
     */
    @GetMapping("/readiness")
    public ResponseEntity<ResultMessage> readiness() {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean databaseAvailable = databaseAvailable();
        checks.put("database", databaseAvailable ? "UP" : "DOWN");

        boolean redisRequired = environment.getProperty("crest.production-mode", Boolean.class, false);
        boolean redisAvailable = !redisRequired || redisAvailable();
        if (redisRequired) {
            checks.put("redis", redisAvailable ? "UP" : "DOWN");
        }

        if (databaseAvailable && redisAvailable) {
            return ResponseEntity.ok(ResultMessage.success(status("UP", checks)));
        }
        ResultMessage result = new ResultMessage(HttpStatus.SERVICE_UNAVAILABLE.value(), "服务未就绪");
        result.setData(status("DOWN", checks));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }

    /**
     * 校验数据库连接是否可用
     */
    private boolean databaseAvailable() {
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(DATABASE_VALIDATION_TIMEOUT_SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生产模式要求 Redis 可连接，保证跨 Pod 缓存和票据可用
     */
    private boolean redisAvailable() {
        if (redisConnectionFactory == null) {
            return false;
        }
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            return connection.ping() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 组装健康检查响应体
     */
    private Map<String, Object> status(String status, Map<String, Object> checks) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("checks", checks);
        return body;
    }
}
