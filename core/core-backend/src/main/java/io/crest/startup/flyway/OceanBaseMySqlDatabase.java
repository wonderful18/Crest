package io.crest.startup.flyway;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.database.mysql.MySQLConnection;
import org.flywaydb.database.mysql.MySQLDatabase;

import java.sql.Connection;

/**
 * OceanBase MySQL compatibility keeps MySQL DDL but does not support all MySQL
 * advisory-lock functions used by Flyway's default MySQL integration.
 */
public class OceanBaseMySqlDatabase extends MySQLDatabase {

    public OceanBaseMySqlDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                  StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public void ensureSupported(Configuration configuration) {
        // OceanBase MySQL compatibility is validated by Crest's own integration tests.
    }

    @Override
    protected MySQLConnection doGetConnection(Connection connection) {
        return new OceanBaseMySqlConnection(this, connection);
    }
}
