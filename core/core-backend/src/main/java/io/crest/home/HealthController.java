package io.crest.home;

import io.crest.result.ResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/actuator/health")
/**
 * 应用健康检查控制器，提供存活和就绪检查端点
 */
public class HealthController {

    /**
     * 数据库连接有效性检查的超时时间
     */
    private static final int DATABASE_VALIDATION_TIMEOUT_SECONDS = 2;

    /**
     * 可选数据源，用于就绪检查数据库连通性
     */
    private final DataSource dataSource;

    /**
     * 创建健康检查控制器，数据源缺失时就绪检查会失败
     */
    public HealthController(@Autowired(required = false) DataSource dataSource) {
        this.dataSource = dataSource;
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
        if (databaseAvailable()) {
            checks.put("database", "UP");
            return ResponseEntity.ok(ResultMessage.success(status("UP", checks)));
        }
        checks.put("database", "DOWN");
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
     * 组装健康检查响应体
     */
    private Map<String, Object> status(String status, Map<String, Object> checks) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("checks", checks);
        return body;
    }
}
