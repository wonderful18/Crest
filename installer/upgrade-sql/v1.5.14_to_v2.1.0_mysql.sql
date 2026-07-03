-- Crest v1.5.14 -> v2.1.0 upgrade entrypoint for MySQL.
-- Run this file with the mysql client from the extracted Crest package root:
-- mysql -u<user> -p <crest_database> < upgrade-sql/v1.5.14_to_v2.1.0_mysql.sql
--
-- This file intentionally reuses the reviewed incremental scripts in this
-- directory. Do not run a full-schema file against an existing v1.5.14 database.

DROP PROCEDURE IF EXISTS crest_assert_v1514_to_v210_ready;
DELIMITER //
CREATE PROCEDURE crest_assert_v1514_to_v210_ready()
BEGIN
  DECLARE legacy_setting_count INT DEFAULT 0;
  DECLARE core_setting_count INT DEFAULT 0;
  DECLARE visualization_count INT DEFAULT 0;

  SELECT COUNT(1) INTO legacy_setting_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'sys_setting';

  SELECT COUNT(1) INTO core_setting_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'core_system_setting';

  SELECT COUNT(1) INTO visualization_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'bi_visualization';

  IF legacy_setting_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v1.5.14 precheck failed: sys_setting table is missing';
  END IF;

  IF visualization_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v1.5.14 precheck failed: bi_visualization table is missing';
  END IF;

  IF core_setting_count = 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v1.5.14 precheck failed: core_* tables already exist; use the matching v2.x upgrade script';
  END IF;
END//
DELIMITER ;

CALL crest_assert_v1514_to_v210_ready();
DROP PROCEDURE IF EXISTS crest_assert_v1514_to_v210_ready;

SOURCE upgrade-sql/v2.0.0_export_task_queue_mysql.sql;
SOURCE upgrade-sql/v2.0.0_datasource_sync_task_queue_mysql.sql;
SOURCE upgrade-sql/v2.0.0_dataset_sync_task_queue_mysql.sql;
SOURCE upgrade-sql/v2.0.0_dataset_sync_task_queue_metadata_mysql.sql;
SOURCE upgrade-sql/v2.0.0_scheduled_task_queue_state_mysql.sql;
SOURCE upgrade-sql/v2.0.0_audit_log_filter_index_mysql.sql;
SOURCE upgrade-sql/v2.0.0_to_v2.0.1_metadata_tables_mysql.sql;
SOURCE upgrade-sql/v2.0.0_to_v2.0.1_runtime_identifiers_mysql.sql;
SOURCE upgrade-sql/v2.0.1_to_v2.0.2_metadata_names_mysql.sql;
SOURCE upgrade-sql/v2.0.1_to_v2.0.2_rotate_default_crypto_key_mysql.sql;
SOURCE upgrade-sql/v2.0.3_to_v2.1.0_mysql.sql;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'core_system_setting',
    'core_iam_user',
    'core_datasource',
    'core_dataset_table',
    'core_chart_view',
    'core_visualization',
    'core_share_link',
    'core_template',
    'core_export_task'
  )
ORDER BY table_name;

SELECT pkey, pval
FROM core_system_setting
WHERE pkey IN ('template.accessKey', 'basic.siteTitle')
ORDER BY pkey;
