package io.crest.startup.flyway;

import org.flywaydb.database.mysql.MySQLDatabaseType;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;

import java.sql.Connection;

/**
 * 让 Flyway 以 MySQL 兼容路径执行 OceanBase MySQL 租户迁移
 */
public class OceanBaseMySqlDatabaseType extends MySQLDatabaseType {

    // Flyway 日志和调试输出中展示的数据库类型名称
    @Override
    public String getName() {
        return "OceanBase MySQL";
    }

    // OceanBase MySQL 与 Oracle 租户共享 JDBC 协议前缀，后续通过连接探测区分
    @Override
    public boolean handlesJDBCUrl(String url) {
        return isOceanBaseUrl(url);
    }

    // OceanBase MySQL 元库必须使用 OceanBase 官方 JDBC 驱动
    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        return "com.oceanbase.jdbc.Driver";
    }

    // 连接建立后通过 MySQL 租户可执行的探测语句确认兼容模式
    @Override
    public boolean handlesDatabaseProductNameAndVersion(String databaseProductName, String databaseProductVersion,
                                                        Connection connection) {
        return OceanBaseFlywayDetector.isOceanBase(databaseProductName)
                && OceanBaseFlywayDetector.isMySqlTenant(connection);
    }

    // 返回定制数据库实现，关闭不可靠的 MySQL 命名锁逻辑
    @Override
    public Database createDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                                   StatementInterceptor statementInterceptor) {
        return new OceanBaseMySqlDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    // 优先级略高于默认 MySQL 类型，确保 jdbc:oceanbase 前缀由本类型接管
    @Override
    public int getPriority() {
        return super.getPriority() + 1;
    }

    // OceanBase 驱动使用独立 JDBC scheme
    private boolean isOceanBaseUrl(String url) {
        return url != null && url.startsWith("jdbc:oceanbase:");
    }
}
