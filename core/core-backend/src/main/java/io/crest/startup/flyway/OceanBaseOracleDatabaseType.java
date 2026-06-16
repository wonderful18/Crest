package io.crest.startup.flyway;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.database.oracle.OracleDatabaseType;

import java.sql.Connection;
import java.sql.Types;
import java.util.Properties;

/**
 * Lets Flyway run Oracle-compatible migrations against OceanBase Oracle tenants.
 */
public class OceanBaseOracleDatabaseType extends OracleDatabaseType {

    @Override
    public String getName() {
        return "OceanBase Oracle";
    }

    @Override
    public int getNullType() {
        return Types.VARCHAR;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return isOceanBaseUrl(url);
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        return "com.oceanbase.jdbc.Driver";
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        return OceanBaseFlywayDetector.isOceanBase(databaseProductName)
                && OceanBaseFlywayDetector.isOracleTenant(connection);
    }

    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        return new OceanBaseOracleDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public void setDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        // The OceanBase JDBC driver does not expose Oracle JDBC connection properties.
    }

    @Override
    public void setConfigConnectionProps(Configuration configuration, Properties props, ClassLoader classLoader) {
        // Oracle wallet and Kerberos settings do not apply to OceanBase JDBC connections.
    }

    @Override
    public Connection alterConnectionAsNeeded(Connection connection, Configuration configuration) {
        return connection;
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 1;
    }

    private boolean isOceanBaseUrl(String url) {
        return url != null && url.startsWith("jdbc:oceanbase:");
    }
}
