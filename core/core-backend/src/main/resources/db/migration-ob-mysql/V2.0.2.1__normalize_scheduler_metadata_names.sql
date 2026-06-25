-- Crest 2.0.2 调度元数据命名统一迁移。
-- 执行前请备份系统库；如果旧名称和新名称同时存在，脚本会中断以避免覆盖数据。

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS=0;

DROP PROCEDURE IF EXISTS crest_rename_table_if_needed;
DELIMITER //
CREATE PROCEDURE crest_rename_table_if_needed(IN old_table_name VARCHAR(128), IN new_table_name VARCHAR(128))
BEGIN
  DECLARE old_count INT DEFAULT 0;
  DECLARE new_count INT DEFAULT 0;

  SELECT COUNT(1) INTO old_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = old_table_name;

  SELECT COUNT(1) INTO new_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = new_table_name;

  IF old_count = 1 AND new_count = 0 THEN
    SET @rename_sql = CONCAT('RENAME TABLE `', old_table_name, '` TO `', new_table_name, '`');
    PREPARE rename_stmt FROM @rename_sql;
    EXECUTE rename_stmt;
    DEALLOCATE PREPARE rename_stmt;
  ELSEIF old_count = 1 AND new_count = 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '元数据表改名冲突：旧表和新表同时存在，请先人工确认数据';
  END IF;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS crest_rename_column_if_needed;
DELIMITER //
CREATE PROCEDURE crest_rename_column_if_needed(
  IN target_table_name VARCHAR(128),
  IN old_column_name VARCHAR(128),
  IN new_column_name VARCHAR(128),
  IN column_definition VARCHAR(512)
)
BEGIN
  DECLARE old_count INT DEFAULT 0;
  DECLARE new_count INT DEFAULT 0;

  SELECT COUNT(1) INTO old_count
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = target_table_name AND column_name = old_column_name;

  SELECT COUNT(1) INTO new_count
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = target_table_name AND column_name = new_column_name;

  IF old_count = 1 AND new_count = 0 THEN
    SET @rename_column_sql = CONCAT(
      'ALTER TABLE `', target_table_name, '` CHANGE COLUMN `', old_column_name, '` `', new_column_name, '` ', column_definition
    );
    PREPARE rename_column_stmt FROM @rename_column_sql;
    EXECUTE rename_column_stmt;
    DEALLOCATE PREPARE rename_column_stmt;
  ELSEIF old_count = 1 AND new_count = 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '元数据列改名冲突：旧列和新列同时存在，请先人工确认数据';
  END IF;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS crest_assert_table_rename_safe;
DELIMITER //
CREATE PROCEDURE crest_assert_table_rename_safe(IN old_table_name VARCHAR(128), IN new_table_name VARCHAR(128))
BEGIN
  DECLARE old_count INT DEFAULT 0;
  DECLARE new_count INT DEFAULT 0;

  SELECT COUNT(1) INTO old_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = old_table_name;

  SELECT COUNT(1) INTO new_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = new_table_name;

  IF old_count = 1 AND new_count = 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '元数据表改名冲突：旧表和新表同时存在，请先人工确认数据';
  END IF;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS crest_assert_column_rename_safe;
DELIMITER //
CREATE PROCEDURE crest_assert_column_rename_safe(IN target_table_name VARCHAR(128), IN old_column_name VARCHAR(128), IN new_column_name VARCHAR(128))
BEGIN
  DECLARE old_count INT DEFAULT 0;
  DECLARE new_count INT DEFAULT 0;

  SELECT COUNT(1) INTO old_count
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = target_table_name AND column_name = old_column_name;

  SELECT COUNT(1) INTO new_count
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = target_table_name AND column_name = new_column_name;

  IF old_count = 1 AND new_count = 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '元数据列改名冲突：旧列和新列同时存在，请先人工确认数据';
  END IF;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS crest_drop_foreign_key_if_exists;
DELIMITER //
CREATE PROCEDURE crest_drop_foreign_key_if_exists(IN target_table_name VARCHAR(128), IN target_constraint_name VARCHAR(128))
BEGIN
  DECLARE constraint_count INT DEFAULT 0;

  SELECT COUNT(1) INTO constraint_count
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = target_table_name
    AND constraint_name = target_constraint_name
    AND constraint_type = 'FOREIGN KEY';

  IF constraint_count = 1 THEN
    SET @drop_fk_sql = CONCAT('ALTER TABLE `', target_table_name, '` DROP FOREIGN KEY `', target_constraint_name, '`');
    PREPARE drop_fk_stmt FROM @drop_fk_sql;
    EXECUTE drop_fk_stmt;
    DEALLOCATE PREPARE drop_fk_stmt;
  END IF;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS crest_drop_index_if_exists;
DELIMITER //
CREATE PROCEDURE crest_drop_index_if_exists(IN target_table_name VARCHAR(128), IN target_index_name VARCHAR(128))
BEGIN
  DECLARE index_count INT DEFAULT 0;

  SELECT COUNT(1) INTO index_count
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = target_table_name
    AND index_name = target_index_name;

  IF index_count > 0 THEN
    SET @drop_index_sql = CONCAT('ALTER TABLE `', target_table_name, '` DROP INDEX `', target_index_name, '`');
    PREPARE drop_index_stmt FROM @drop_index_sql;
    EXECUTE drop_index_stmt;
    DEALLOCATE PREPARE drop_index_stmt;
  END IF;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS crest_add_index_if_needed;
DELIMITER //
CREATE PROCEDURE crest_add_index_if_needed(IN target_table_name VARCHAR(128), IN target_index_name VARCHAR(128), IN index_columns VARCHAR(512))
BEGIN
  DECLARE table_count INT DEFAULT 0;
  DECLARE index_count INT DEFAULT 0;

  SELECT COUNT(1) INTO table_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = target_table_name;

  SELECT COUNT(1) INTO index_count
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = target_table_name
    AND index_name = target_index_name;

  IF table_count = 1 AND index_count = 0 THEN
    SET @add_index_sql = CONCAT('CREATE INDEX `', target_index_name, '` ON `', target_table_name, '` (', index_columns, ')');
    PREPARE add_index_stmt FROM @add_index_sql;
    EXECUTE add_index_stmt;
    DEALLOCATE PREPARE add_index_stmt;
  END IF;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS crest_add_foreign_key_if_needed;
DELIMITER //
CREATE PROCEDURE crest_add_foreign_key_if_needed(
  IN target_table_name VARCHAR(128),
  IN target_constraint_name VARCHAR(128),
  IN target_columns VARCHAR(512),
  IN reference_table_name VARCHAR(128),
  IN reference_columns VARCHAR(512)
)
BEGIN
  DECLARE table_count INT DEFAULT 0;
  DECLARE reference_table_count INT DEFAULT 0;
  DECLARE constraint_count INT DEFAULT 0;

  SELECT COUNT(1) INTO table_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = target_table_name;

  SELECT COUNT(1) INTO reference_table_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = reference_table_name;

  SELECT COUNT(1) INTO constraint_count
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = target_table_name
    AND constraint_name = target_constraint_name
    AND constraint_type = 'FOREIGN KEY';

  IF table_count = 1 AND reference_table_count = 1 AND constraint_count = 0 THEN
    SET @add_fk_sql = CONCAT(
      'ALTER TABLE `', target_table_name, '` ADD CONSTRAINT `', target_constraint_name, '` FOREIGN KEY (',
      target_columns, ') REFERENCES `', reference_table_name, '` (', reference_columns, ')'
    );
    PREPARE add_fk_stmt FROM @add_fk_sql;
    EXECUTE add_fk_stmt;
    DEALLOCATE PREPARE add_fk_stmt;
  END IF;
END//
DELIMITER ;

CALL crest_assert_table_rename_safe('QRTZ_JOB_DETAILS', 'core_schedule_job_details');
CALL crest_assert_table_rename_safe('QRTZ_TRIGGERS', 'core_schedule_triggers');
CALL crest_assert_table_rename_safe('QRTZ_BLOB_TRIGGERS', 'core_schedule_blob_triggers');
CALL crest_assert_table_rename_safe('QRTZ_CALENDARS', 'core_schedule_calendars');
CALL crest_assert_table_rename_safe('QRTZ_CRON_TRIGGERS', 'core_schedule_cron_triggers');
CALL crest_assert_table_rename_safe('QRTZ_FIRED_TRIGGERS', 'core_schedule_fired_triggers');
CALL crest_assert_table_rename_safe('QRTZ_LOCKS', 'core_schedule_locks');
CALL crest_assert_table_rename_safe('QRTZ_PAUSED_TRIGGER_GRPS', 'core_schedule_paused_trigger_groups');
CALL crest_assert_table_rename_safe('QRTZ_SCHEDULER_STATE', 'core_schedule_scheduler_state');
CALL crest_assert_table_rename_safe('QRTZ_SIMPLE_TRIGGERS', 'core_schedule_simple_triggers');
CALL crest_assert_table_rename_safe('QRTZ_SIMPROP_TRIGGERS', 'core_schedule_simprop_triggers');
CALL crest_assert_table_rename_safe('core_template_category_rel', 'core_template_category_relation');
CALL crest_assert_table_rename_safe('core_export_download', 'core_export_download_task');
CALL crest_assert_column_rename_safe('core_dataset', 'qrtz_instance', 'scheduler_fire_instance_id');
CALL crest_assert_column_rename_safe('core_datasource', 'qrtz_instance', 'scheduler_fire_instance_id');

CALL crest_drop_foreign_key_if_exists('QRTZ_BLOB_TRIGGERS', 'QRTZ_BLOB_TRIGGERS_ibfk_1');
CALL crest_drop_foreign_key_if_exists('QRTZ_CRON_TRIGGERS', 'QRTZ_CRON_TRIGGERS_ibfk_1');
CALL crest_drop_foreign_key_if_exists('QRTZ_SIMPLE_TRIGGERS', 'QRTZ_SIMPLE_TRIGGERS_ibfk_1');
CALL crest_drop_foreign_key_if_exists('QRTZ_SIMPROP_TRIGGERS', 'QRTZ_SIMPROP_TRIGGERS_ibfk_1');
CALL crest_drop_foreign_key_if_exists('QRTZ_TRIGGERS', 'QRTZ_TRIGGERS_ibfk_1');
CALL crest_drop_foreign_key_if_exists('core_schedule_blob_triggers', 'QRTZ_BLOB_TRIGGERS_ibfk_1');
CALL crest_drop_foreign_key_if_exists('core_schedule_cron_triggers', 'QRTZ_CRON_TRIGGERS_ibfk_1');
CALL crest_drop_foreign_key_if_exists('core_schedule_simple_triggers', 'QRTZ_SIMPLE_TRIGGERS_ibfk_1');
CALL crest_drop_foreign_key_if_exists('core_schedule_simprop_triggers', 'QRTZ_SIMPROP_TRIGGERS_ibfk_1');
CALL crest_drop_foreign_key_if_exists('core_schedule_triggers', 'QRTZ_TRIGGERS_ibfk_1');

CALL crest_drop_index_if_exists('QRTZ_TRIGGERS', 'SCHED_NAME');
CALL crest_drop_index_if_exists('core_schedule_triggers', 'SCHED_NAME');

CALL crest_rename_table_if_needed('QRTZ_JOB_DETAILS', 'core_schedule_job_details');
CALL crest_rename_table_if_needed('QRTZ_TRIGGERS', 'core_schedule_triggers');
CALL crest_rename_table_if_needed('QRTZ_BLOB_TRIGGERS', 'core_schedule_blob_triggers');
CALL crest_rename_table_if_needed('QRTZ_CALENDARS', 'core_schedule_calendars');
CALL crest_rename_table_if_needed('QRTZ_CRON_TRIGGERS', 'core_schedule_cron_triggers');
CALL crest_rename_table_if_needed('QRTZ_FIRED_TRIGGERS', 'core_schedule_fired_triggers');
CALL crest_rename_table_if_needed('QRTZ_LOCKS', 'core_schedule_locks');
CALL crest_rename_table_if_needed('QRTZ_PAUSED_TRIGGER_GRPS', 'core_schedule_paused_trigger_groups');
CALL crest_rename_table_if_needed('QRTZ_SCHEDULER_STATE', 'core_schedule_scheduler_state');
CALL crest_rename_table_if_needed('QRTZ_SIMPLE_TRIGGERS', 'core_schedule_simple_triggers');
CALL crest_rename_table_if_needed('QRTZ_SIMPROP_TRIGGERS', 'core_schedule_simprop_triggers');
CALL crest_rename_table_if_needed('core_template_category_rel', 'core_template_category_relation');
CALL crest_rename_table_if_needed('core_export_download', 'core_export_download_task');

CALL crest_rename_column_if_needed(
  'core_dataset',
  'qrtz_instance',
  'scheduler_fire_instance_id',
  'varchar(1024) DEFAULT NULL COMMENT ''调度触发实例编号'''
);
CALL crest_rename_column_if_needed(
  'core_datasource',
  'qrtz_instance',
  'scheduler_fire_instance_id',
  'longtext COMMENT ''调度触发实例编号'''
);

CALL crest_add_index_if_needed(
  'core_schedule_triggers',
  'idx_core_schedule_triggers_job',
  '`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`'
);

CALL crest_add_foreign_key_if_needed(
  'core_schedule_blob_triggers',
  'fk_csch_blob_trigger',
  '`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`',
  'core_schedule_triggers',
  '`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`'
);
CALL crest_add_foreign_key_if_needed(
  'core_schedule_cron_triggers',
  'fk_csch_cron_trigger',
  '`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`',
  'core_schedule_triggers',
  '`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`'
);
CALL crest_add_foreign_key_if_needed(
  'core_schedule_simple_triggers',
  'fk_csch_simple_trigger',
  '`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`',
  'core_schedule_triggers',
  '`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`'
);
CALL crest_add_foreign_key_if_needed(
  'core_schedule_simprop_triggers',
  'fk_csch_simprop_trigger',
  '`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`',
  'core_schedule_triggers',
  '`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`'
);
CALL crest_add_foreign_key_if_needed(
  'core_schedule_triggers',
  'fk_csch_trigger_job',
  '`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`',
  'core_schedule_job_details',
  '`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`'
);

DROP PROCEDURE IF EXISTS crest_add_foreign_key_if_needed;
DROP PROCEDURE IF EXISTS crest_add_index_if_needed;
DROP PROCEDURE IF EXISTS crest_drop_index_if_exists;
DROP PROCEDURE IF EXISTS crest_drop_foreign_key_if_exists;
DROP PROCEDURE IF EXISTS crest_assert_column_rename_safe;
DROP PROCEDURE IF EXISTS crest_assert_table_rename_safe;
DROP PROCEDURE IF EXISTS crest_rename_column_if_needed;
DROP PROCEDURE IF EXISTS crest_rename_table_if_needed;

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
