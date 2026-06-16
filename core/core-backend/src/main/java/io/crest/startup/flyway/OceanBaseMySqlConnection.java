package io.crest.startup.flyway;

import org.flywaydb.database.mysql.MySQLConnection;
import org.flywaydb.database.mysql.MySQLDatabase;

import java.sql.Connection;

/**
 * OceanBase MySQL tenants do not support GET_LOCK/RELEASE_LOCK reliably.
 */
public class OceanBaseMySqlConnection extends MySQLConnection {

    public OceanBaseMySqlConnection(MySQLDatabase database, Connection connection) {
        super(database, connection);
    }

    @Override
    protected boolean canUseNamedLockTemplate() {
        return false;
    }
}
