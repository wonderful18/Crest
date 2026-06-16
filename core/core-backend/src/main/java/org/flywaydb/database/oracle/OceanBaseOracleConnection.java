package org.flywaydb.database.oracle;

import java.sql.Connection;

/**
 * Uses OceanBase-specific schema metadata while keeping Flyway's Oracle migration behavior.
 */
public class OceanBaseOracleConnection extends OracleConnection {

    public OceanBaseOracleConnection(OracleDatabase database, Connection connection) {
        super(database, connection);
    }

    @Override
    public org.flywaydb.core.internal.database.base.Schema getSchema(String name) {
        return new OceanBaseOracleSchema(jdbcTemplate, database, name);
    }
}
