package io.crest.metadata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crest.utils.LogUtil;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads large OceanBase Oracle seed data through prepared statements.
 */
@Component
@Order(1)
public class ObOracleMetadataSeedInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final TypeReference<List<Object>> ROW_TYPE = new TypeReference<>() {
    };

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ObOracleMetadataSeedInitializer(Environment environment, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.environment = environment;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (MetadataDbTypeResolver.resolve(environment) != MetadataDbType.OB_ORACLE) {
            return;
        }
        try {
            loadRefArea();
            loadVisualizationSubject();
        } catch (Exception e) {
            LogUtil.error("=====OceanBase Oracle metadata seed initialization failed=====", e);
            throw new IllegalStateException("OceanBase Oracle metadata seed initialization failed", e);
        }
    }

    private void loadRefArea() throws IOException {
        List<List<Object>> rows = loadRows("db/seed-ob-oracle/ref_area.jsonl", 4);
        Set<String> existingIds = existingIds("ref_area");
        List<List<Object>> missingRows = rows.stream()
                .filter(row -> !existingIds.contains(stringValue(row.get(0))))
                .toList();
        if (missingRows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO ref_area (id, "LEVEL", name, pid)
                VALUES (?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                List<Object> row = missingRows.get(i);
                ps.setString(1, stringValue(row.get(0)));
                ps.setString(2, stringValue(row.get(1)));
                ps.setString(3, stringValue(row.get(2)));
                ps.setString(4, stringValue(row.get(3)));
            }

            @Override
            public int getBatchSize() {
                return missingRows.size();
            }
        });
        LogUtil.info("=====OceanBase Oracle ref_area seed loaded: " + missingRows.size() + " rows=====");
    }

    private void loadVisualizationSubject() throws IOException {
        List<List<Object>> rows = loadRows("db/seed-ob-oracle/visualization_subject.jsonl", 13);
        Set<String> existingIds = existingIds("visualization_subject");
        List<List<Object>> missingRows = rows.stream()
                .filter(row -> !existingIds.contains(stringValue(row.get(0))))
                .toList();
        if (missingRows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO visualization_subject (
                    id, name, type, details, delete_flag, cover_url, create_num,
                    create_time, create_by, update_time, update_by, delete_time, delete_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                List<Object> row = missingRows.get(i);
                ps.setString(1, stringValue(row.get(0)));
                ps.setString(2, stringValue(row.get(1)));
                ps.setString(3, stringValue(row.get(2)));
                setClob(ps, 4, stringValue(row.get(3)));
                setInteger(ps, 5, integerValue(row.get(4)));
                setClob(ps, 6, stringValue(row.get(5)));
                setInteger(ps, 7, integerValue(row.get(6)));
                setLong(ps, 8, longValue(row.get(7)));
                ps.setString(9, stringValue(row.get(8)));
                setLong(ps, 10, longValue(row.get(9)));
                ps.setString(11, stringValue(row.get(10)));
                setLong(ps, 12, longValue(row.get(11)));
                setLong(ps, 13, longValue(row.get(12)));
            }

            @Override
            public int getBatchSize() {
                return missingRows.size();
            }
        });
        LogUtil.info("=====OceanBase Oracle visualization_subject seed loaded: " + missingRows.size() + " rows=====");
    }

    private List<List<Object>> loadRows(String path, int expectedColumns) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<List<Object>> rows = reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(this::readRow)
                    .toList();
            for (List<Object> row : rows) {
                if (row.size() != expectedColumns) {
                    throw new IllegalStateException(path + " contains a row with " + row.size() + " columns");
                }
            }
            return rows;
        }
    }

    private List<Object> readRow(String line) {
        try {
            return objectMapper.readValue(line, ROW_TYPE);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid OceanBase Oracle seed row", e);
        }
    }

    private Set<String> existingIds(String tableName) {
        return new HashSet<>(jdbcTemplate.queryForList("SELECT id FROM " + tableName, String.class));
    }

    private static void setClob(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.CLOB);
            return;
        }
        ps.setCharacterStream(index, new StringReader(value), value.length());
    }

    private static void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
            return;
        }
        ps.setInt(index, value);
    }

    private static void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
            return;
        }
        ps.setLong(index, value);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
