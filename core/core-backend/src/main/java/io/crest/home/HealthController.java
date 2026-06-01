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
public class HealthController {

    private static final int DATABASE_VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    public HealthController(@Autowired(required = false) DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<ResultMessage> health() {
        return readiness();
    }

    @GetMapping("/liveness")
    public ResponseEntity<ResultMessage> liveness() {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("jvm", "UP");
        return ResponseEntity.ok(ResultMessage.success(status("UP", checks)));
    }

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

    private Map<String, Object> status(String status, Map<String, Object> checks) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("checks", checks);
        return body;
    }
}
