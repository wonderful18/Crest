package io.crest.startup.flyway;

import org.flywaydb.database.mysql.MySQLConnection;
import org.flywaydb.database.mysql.MySQLDatabase;

import java.sql.Connection;

/**
 * OceanBase MySQL 租户的命名锁兼容连接
 */
public class OceanBaseMySqlConnection extends MySQLConnection {

    public OceanBaseMySqlConnection(MySQLDatabase database, Connection connection) {
        super(database, connection);
    }

    // 禁用 GET_LOCK/RELEASE_LOCK，避免 OceanBase 命名锁行为影响迁移启动
    @Override
    protected boolean canUseNamedLockTemplate() {
        return false;
    }
}
