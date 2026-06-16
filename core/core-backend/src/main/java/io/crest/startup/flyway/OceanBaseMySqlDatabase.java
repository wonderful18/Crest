package io.crest.startup.flyway;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.database.mysql.MySQLConnection;
import org.flywaydb.database.mysql.MySQLDatabase;

import java.sql.Connection;

/**
 * OceanBase MySQL 兼容 MySQL DDL，但不完整支持 Flyway 默认 MySQL 命名锁
 */
public class OceanBaseMySqlDatabase extends MySQLDatabase {

    public OceanBaseMySqlDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                  StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    // 版本兼容性由 Crest 的 OceanBase 集成验证兜底，不使用 Flyway 内置 MySQL 版本门槛
    @Override
    public void ensureSupported(Configuration configuration) {
    }

    // 返回禁用命名锁的连接实现，避免 GET_LOCK/RELEASE_LOCK 在 OceanBase 上不稳定
    @Override
    protected MySQLConnection doGetConnection(Connection connection) {
        return new OceanBaseMySqlConnection(this, connection);
    }
}
