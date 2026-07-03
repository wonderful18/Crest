package io.crest.startup;

import io.crest.utils.LogUtil;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

@Component
/**
 * Flyway MySQL 兼容迁移回调，用于临时关闭 sql_notes 并清理冲突种子数据
 */
public class FlywayMysqlSqlNotesCallback implements Callback {
    private static final Set<String> MYSQL_COMPATIBILITY_MIGRATIONS = Set.of("1.5.8.1");
    private static final String DELETE_CORE_ROLE_MENU_PERMISSION =
            "DELETE FROM `core_iam_role_menu_permission` WHERE `id` IN (7701,7702,7703,7801,7802,7803)";
    private static final String DELETE_LEGACY_ROLE_MENU_PERMISSION =
            "DELETE FROM `iam_role_menu_permission` WHERE `id` IN (7701,7702,7703,7801,7802,7803)";
    private static final String DELETE_CORE_MENU =
            "DELETE FROM `core_iam_menu` WHERE `id` IN (77,78)";
    private static final String DELETE_LEGACY_MENU =
            "DELETE FROM `iam_menu` WHERE `id` IN (77,78)";

    /**
     * 判断当前事件是否需要该回调处理
     */
    @Override
    public boolean supports(Event event, Context context) {
        return isSqlNotesEvent(event) && isMysqlCompatibilityMigration(context) && isMySql(context);
    }

    /**
     * 声明回调可在事务中执行
     */
    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    /**
     * 在迁移前关闭 sql_notes 并清理种子数据，迁移后恢复
     */
    @Override
    public void handle(Event event, Context context) {
        if (event == Event.BEFORE_EACH_MIGRATE) {
            setSqlNotes(context, false);
            cleanDataAssetSeedRows(context);
            return;
        }
        setSqlNotes(context, true);
    }

    /**
     * 返回 Flyway 回调名称
     */
    @Override
    public String getCallbackName() {
        return "mysql-sql-notes";
    }

    /**
     * 判断事件是否属于 sql_notes 处理窗口
     */
    private boolean isSqlNotesEvent(Event event) {
        return event == Event.BEFORE_EACH_MIGRATE
                || event == Event.AFTER_EACH_MIGRATE
                || event == Event.AFTER_EACH_MIGRATE_ERROR;
    }

    /**
     * 判断当前迁移版本是否需要 MySQL 兼容处理
     */
    private boolean isMysqlCompatibilityMigration(Context context) {
        if (context == null) {
            return false;
        }
        MigrationInfo migrationInfo = context.getMigrationInfo();
        return migrationInfo != null
                && migrationInfo.getVersion() != null
                && MYSQL_COMPATIBILITY_MIGRATIONS.contains(migrationInfo.getVersion().getVersion());
    }

    /**
     * 判断当前 Flyway 连接是否指向 MySQL 兼容数据库
     */
    private boolean isMySql(Context context) {
        try {
            Connection connection = context.getConnection();
            return connection != null
                    && connection.getMetaData() != null
                    && connection.getMetaData().getDatabaseProductName() != null
                    && connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        } catch (SQLException e) {
            LogUtil.debug("Flyway database product check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 设置当前会话的 sql_notes 开关
     */
    private void setSqlNotes(Context context, boolean enabled) {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("SET SESSION sql_notes = " + (enabled ? "1" : "0"));
        } catch (SQLException e) {
            LogUtil.debug("Flyway sql_notes update skipped: " + e.getMessage());
        }
    }

    /**
     * 清理数据资产种子菜单和权限，避免兼容迁移重复插入
     */
    private void cleanDataAssetSeedRows(Context context) {
        try (Statement statement = context.getConnection().createStatement()) {
            deleteRolePermissionSeedRows(statement);
            deleteMenuSeedRows(statement);
        } catch (SQLException e) {
            LogUtil.debug("Flyway data asset seed cleanup skipped: " + e.getMessage());
        }
    }

    /**
     * 清理当前权限表；历史迁移阶段新表尚不存在时回退到旧表
     */
    private void deleteRolePermissionSeedRows(Statement statement) throws SQLException {
        try {
            statement.execute(DELETE_CORE_ROLE_MENU_PERMISSION);
        } catch (SQLException e) {
            statement.execute(DELETE_LEGACY_ROLE_MENU_PERMISSION);
        }
    }

    /**
     * 清理当前菜单表；历史迁移阶段新表尚不存在时回退到旧表
     */
    private void deleteMenuSeedRows(Statement statement) throws SQLException {
        try {
            statement.execute(DELETE_CORE_MENU);
        } catch (SQLException e) {
            statement.execute(DELETE_LEGACY_MENU);
        }
    }
}
