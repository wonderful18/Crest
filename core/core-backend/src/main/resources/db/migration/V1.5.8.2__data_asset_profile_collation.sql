-- Keep data asset metadata text comparisons stable on MySQL installations that use utf8mb4_unicode_ci connections.
ALTER TABLE `data_asset_profile` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
UPDATE `data_asset_profile` SET `certified` = 0, `recommended` = 0 WHERE `deprecated` = 1;
