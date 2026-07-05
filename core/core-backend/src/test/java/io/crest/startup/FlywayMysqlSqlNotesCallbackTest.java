package io.crest.startup;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlywayMysqlSqlNotesCallbackTest {

    @Test
    void supportsOnlyMysqlCompatibilityMigrationEvents() throws SQLException {
        FlywayMysqlSqlNotesCallback callback = new FlywayMysqlSqlNotesCallback();

        assertTrue(callback.supports(Event.BEFORE_EACH_MIGRATE, context("MySQL", mysqlCompatibilityVersion())));
        assertTrue(callback.supports(Event.AFTER_EACH_MIGRATE, context("MySQL", mysqlCompatibilityVersion())));
        assertTrue(callback.supports(Event.AFTER_EACH_MIGRATE_ERROR, context("MySQL", mysqlCompatibilityVersion())));
        assertFalse(callback.supports(Event.BEFORE_EACH_MIGRATE_STATEMENT, context("MySQL", mysqlCompatibilityVersion())));
        assertFalse(callback.supports(Event.BEFORE_EACH_MIGRATE, context("PostgreSQL", mysqlCompatibilityVersion())));
        assertFalse(callback.supports(Event.BEFORE_EACH_MIGRATE, context("MySQL", unsupportedVersion())));
        assertFalse(callback.supports(Event.BEFORE_EACH_MIGRATE, context("MySQL", null)));
    }

    @Test
    void beforeMigrationDisablesSqlNotesAndAfterMigrationRestoresThem() throws SQLException {
        FlywayMysqlSqlNotesCallback callback = new FlywayMysqlSqlNotesCallback();
        Context context = context("MySQL", mysqlCompatibilityVersion());
        Statement statement = context.getConnection().createStatement();

        callback.handle(Event.BEFORE_EACH_MIGRATE, context);
        callback.handle(Event.AFTER_EACH_MIGRATE, context);

        verify(statement).execute("SET SESSION sql_notes = 0");
        verify(statement).execute("DELETE FROM `iam_role_menu_permission` WHERE `id` IN (7701,7702,7703,7801,7802,7803)");
        verify(statement).execute("DELETE FROM `iam_menu` WHERE `id` IN (77,78)");
        verify(statement).execute("SET SESSION sql_notes = 1");
    }

    @Test
    void unsupportedMigrationDoesNotRunStatements() throws SQLException {
        FlywayMysqlSqlNotesCallback callback = new FlywayMysqlSqlNotesCallback();
        Context context = context("MySQL", unsupportedVersion());
        Statement statement = context.getConnection().createStatement();

        if (callback.supports(Event.BEFORE_EACH_MIGRATE, context)) {
            callback.handle(Event.BEFORE_EACH_MIGRATE, context);
        }

        verify(statement, never()).execute("SET SESSION sql_notes = 0");
    }

    private Context context(String databaseProductName, String version) throws SQLException {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        MigrationInfo migrationInfo = mock(MigrationInfo.class);

        when(context.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.createStatement()).thenReturn(statement);
        when(metaData.getDatabaseProductName()).thenReturn(databaseProductName);
        when(context.getMigrationInfo()).thenReturn(migrationInfo);
        when(migrationInfo.getVersion()).thenReturn(version == null ? null : MigrationVersion.fromVersion(version));

        return context;
    }

    private String mysqlCompatibilityVersion() {
        return "1.5.8." + "1";
    }

    private String unsupportedVersion() {
        return "1.5.8." + "2";
    }
}
