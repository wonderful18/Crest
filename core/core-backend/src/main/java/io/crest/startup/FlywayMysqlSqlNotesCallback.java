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
public class FlywayMysqlSqlNotesCallback implements Callback {
    private static final String MYSQL_COMPATIBILITY_MIGRATION_VERSION = String.join(".", "1", "5", "8", "1");
    private static final Set<String> MYSQL_COMPATIBILITY_MIGRATIONS = Set.of(MYSQL_COMPATIBILITY_MIGRATION_VERSION);
    private static final String DATA_ASSET_MENU_IDS = "77,78";
    private static final String DATA_ASSET_PERMISSION_IDS = "7701,7702,7703,7801,7802,7803";

    @Override
    public boolean supports(Event event, Context context) {
        return isSqlNotesEvent(event) && isMysqlCompatibilityMigration(context) && isMySql(context);
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        if (event == Event.BEFORE_EACH_MIGRATE) {
            setSqlNotes(context, false);
            cleanDataAssetSeedRows(context);
            return;
        }
        setSqlNotes(context, true);
    }

    @Override
    public String getCallbackName() {
        return "mysql-sql-notes";
    }

    private boolean isSqlNotesEvent(Event event) {
        return event == Event.BEFORE_EACH_MIGRATE
                || event == Event.AFTER_EACH_MIGRATE
                || event == Event.AFTER_EACH_MIGRATE_ERROR;
    }

    private boolean isMysqlCompatibilityMigration(Context context) {
        if (context == null) {
            return false;
        }
        MigrationInfo migrationInfo = context.getMigrationInfo();
        return migrationInfo != null
                && migrationInfo.getVersion() != null
                && MYSQL_COMPATIBILITY_MIGRATIONS.contains(migrationInfo.getVersion().getVersion());
    }

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

    private void setSqlNotes(Context context, boolean enabled) {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("SET SESSION sql_notes = " + (enabled ? "1" : "0"));
        } catch (SQLException e) {
            LogUtil.debug("Flyway sql_notes update skipped: " + e.getMessage());
        }
    }

    private void cleanDataAssetSeedRows(Context context) {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("DELETE FROM `iam_role_menu_permission` WHERE `id` IN (" + DATA_ASSET_PERMISSION_IDS + ")");
            statement.execute("DELETE FROM `iam_menu` WHERE `id` IN (" + DATA_ASSET_MENU_IDS + ")");
        } catch (SQLException e) {
            LogUtil.debug("Flyway data asset seed cleanup skipped: " + e.getMessage());
        }
    }
}
