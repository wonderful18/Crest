package org.flywaydb.database.oracle;

import org.flywaydb.core.internal.jdbc.JdbcTemplate;

import java.sql.SQLException;
import java.util.List;

/**
 * Avoids Oracle data dictionary views that are not available in OceanBase Oracle tenants.
 */
public class OceanBaseOracleSchema extends OracleSchema {

    OceanBaseOracleSchema(JdbcTemplate jdbcTemplate, OracleDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doEmpty() throws SQLException {
        return jdbcTemplate.queryForInt("""
                SELECT COUNT(*)
                FROM USER_OBJECTS
                WHERE OBJECT_TYPE IN (
                    'TABLE', 'VIEW', 'SEQUENCE', 'TRIGGER', 'SYNONYM',
                    'PROCEDURE', 'FUNCTION', 'PACKAGE', 'PACKAGE BODY',
                    'TYPE', 'TYPE BODY', 'MATERIALIZED VIEW'
                )
                """) == 0;
    }

    @Override
    protected OracleTable[] doAllTables() throws SQLException {
        List<String> tableNames = jdbcTemplate.queryForStringList("""
                SELECT TABLE_NAME
                FROM USER_TABLES
                ORDER BY TABLE_NAME
                """);
        return tableNames.stream()
                .map(tableName -> new OracleTable(jdbcTemplate, database, this, tableName))
                .toArray(OracleTable[]::new);
    }
}
