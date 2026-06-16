package io.crest.startup.flyway;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.database.oracle.OracleDatabase;
import org.flywaydb.database.oracle.OracleConnection;
import org.flywaydb.database.oracle.OceanBaseOracleConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * OceanBase reports its own version, not an Oracle version.
 */
public class OceanBaseOracleDatabase extends OracleDatabase {

    public OceanBaseOracleDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public void ensureSupported(Configuration configuration) {
        // OceanBase Oracle compatibility is validated by Crest's own integration tests.
    }

    @Override
    protected OracleConnection doGetConnection(Connection connection) {
        return new OceanBaseOracleConnection(this, connection);
    }

    @Override
    protected String doGetCatalog() throws SQLException {
        return jdbcTemplate.queryForString("SELECT SYS_CONTEXT('USERENV','DB_NAME') FROM DUAL");
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return jdbcTemplate.queryForString("SELECT USER FROM DUAL");
    }
}
