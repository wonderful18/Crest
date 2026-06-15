package io.crest.startup.flyway;

import org.flywaydb.database.mysql.MySQLDatabaseType;

import java.sql.Connection;

/**
 * Lets Flyway run MySQL-compatible migrations against OceanBase MySQL tenants.
 */
public class OceanBaseMySqlDatabaseType extends MySQLDatabaseType {

    @Override
    public String getName() {
        return "OceanBase MySQL";
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
                && OceanBaseFlywayDetector.isMySqlTenant(connection);
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 1;
    }

    private boolean isOceanBaseUrl(String url) {
        return url != null && url.startsWith("jdbc:oceanbase:");
    }
}
