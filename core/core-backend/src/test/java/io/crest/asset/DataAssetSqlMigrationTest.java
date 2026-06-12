package io.crest.asset;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataAssetSqlMigrationTest {

    @Test
    void dataAssetProfileUsesPortableCollationInInstallAndUpgradeScripts() throws IOException {
        for (String file : new String[]{
                "src/main/resources/db/migration/V1.5.0.1__initial_schema.sql",
                "src/main/resources/db/migration/V1.5.8.1__data_asset_center.sql",
                "../../installer/upgrade-sql/v1.5.7_to_v1.5.8.sql"
        }) {
            String sql = Files.readString(Path.of(file));
            int tableIndex = sql.indexOf("CREATE TABLE IF NOT EXISTS `data_asset_profile`");
            assertTrue(tableIndex >= 0, file + " should create data_asset_profile");
            int endIndex = sql.indexOf("COMMENT='数据资产治理扩展信息'", tableIndex);
            String tableDdl = sql.substring(tableIndex, endIndex);

            assertTrue(tableDdl.contains("COLLATE=utf8mb4_unicode_ci"), file);
            assertFalse(tableDdl.contains("utf8mb4_0900_ai_ci"), file);
            assertTrue(sql.contains(
                    "UPDATE `data_asset_profile` SET `certified` = 0, `recommended` = 0 WHERE `deprecated` = 1"),
                    file + " should clean conflicting governance flags");
        }
        String collationFix = Files.readString(Path.of(
                "src/main/resources/db/migration/V1.5.8.2__data_asset_profile_collation.sql"));
        assertTrue(collationFix.contains(
                "UPDATE `data_asset_profile` SET `certified` = 0, `recommended` = 0 WHERE `deprecated` = 1"),
                "V1.5.8.2 should clean conflicting governance flags for environments that already ran V1.5.8.1");
    }
}
