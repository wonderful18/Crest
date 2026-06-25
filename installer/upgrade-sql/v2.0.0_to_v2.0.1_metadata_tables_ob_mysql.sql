-- Crest 2.0 元数据表统一命名迁移。
-- 本脚本只重命名 Crest 自有元数据表，Quartz 的 QRTZ_* 表保持不变。
-- 执行前请备份系统库；如果同时存在旧表和新表，脚本会中断以避免覆盖数据。

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

CALL crest_rename_table_if_needed('api_traffic', 'core_api_traffic_limit');
CALL crest_rename_table_if_needed('ref_area', 'core_reference_area');
CALL crest_rename_table_if_needed('ref_custom_area', 'core_reference_custom_area');
CALL crest_rename_table_if_needed('geo_area', 'core_geo_area');
CALL crest_rename_table_if_needed('geo_sub_area', 'core_geo_sub_area');
CALL crest_rename_table_if_needed('copilot_config', 'core_query_assistant_config');
CALL crest_rename_table_if_needed('copilot_message', 'core_query_assistant_message');
CALL crest_rename_table_if_needed('copilot_token', 'core_query_assistant_token');
CALL crest_rename_table_if_needed('dataset_group', 'core_dataset');
CALL crest_rename_table_if_needed('dataset_table', 'core_dataset_table');
CALL crest_rename_table_if_needed('dataset_field', 'core_dataset_field');
CALL crest_rename_table_if_needed('dataset_sql_log', 'core_dataset_sql_log');
CALL crest_rename_table_if_needed('dataset_sync_task', 'core_dataset_sync_task');
CALL crest_rename_table_if_needed('dataset_sync_task_log', 'core_dataset_sync_task_log');
CALL crest_rename_table_if_needed('data_source', 'core_datasource');
CALL crest_rename_table_if_needed('data_source_sync_task', 'core_datasource_sync_task');
CALL crest_rename_table_if_needed('data_source_sync_task_log', 'core_datasource_sync_task_log');
CALL crest_rename_table_if_needed('data_engine', 'core_datasource_engine');
CALL crest_rename_table_if_needed('data_source_driver', 'core_datasource_driver');
CALL crest_rename_table_if_needed('data_source_driver_jar', 'core_datasource_driver_jar');
CALL crest_rename_table_if_needed('data_source_finish_page', 'core_datasource_finish_page');
CALL crest_rename_table_if_needed('data_asset_profile', 'core_asset_profile');
CALL crest_rename_table_if_needed('bi_chart', 'core_chart_view');
CALL crest_rename_table_if_needed('bi_chart_snapshot', 'core_chart_view_snapshot');
CALL crest_rename_table_if_needed('bi_visualization', 'core_visualization');
CALL crest_rename_table_if_needed('bi_visualization_snapshot', 'core_visualization_snapshot');
CALL crest_rename_table_if_needed('visualization_background', 'core_visualization_background');
CALL crest_rename_table_if_needed('visualization_background_image', 'core_visualization_background_image');
CALL crest_rename_table_if_needed('visualization_link_jump', 'core_visualization_jump');
CALL crest_rename_table_if_needed('visualization_link_jump_info', 'core_visualization_jump_action');
CALL crest_rename_table_if_needed('visualization_link_jump_target_view_info', 'core_visualization_jump_target');
CALL crest_rename_table_if_needed('snapshot_visualization_link_jump', 'core_visualization_jump_snapshot');
CALL crest_rename_table_if_needed('snapshot_visualization_link_jump_info', 'core_visualization_jump_action_snapshot');
CALL crest_rename_table_if_needed('snapshot_visualization_link_jump_target_view_info', 'core_visualization_jump_target_snapshot');
CALL crest_rename_table_if_needed('visualization_linkage', 'core_visualization_linkage');
CALL crest_rename_table_if_needed('visualization_linkage_field', 'core_visualization_linkage_field');
CALL crest_rename_table_if_needed('snapshot_visualization_linkage', 'core_visualization_linkage_snapshot');
CALL crest_rename_table_if_needed('snapshot_visualization_linkage_field', 'core_visualization_linkage_field_snapshot');
CALL crest_rename_table_if_needed('visualization_outer_params', 'core_visualization_parameter');
CALL crest_rename_table_if_needed('visualization_outer_params_info', 'core_visualization_parameter_item');
CALL crest_rename_table_if_needed('visualization_outer_params_target_view_info', 'core_visualization_parameter_target');
CALL crest_rename_table_if_needed('snapshot_visualization_outer_params', 'core_visualization_parameter_snapshot');
CALL crest_rename_table_if_needed('snapshot_visualization_outer_params_info', 'core_visualization_parameter_item_snapshot');
CALL crest_rename_table_if_needed('snapshot_visualization_outer_params_target_view_info', 'core_visualization_parameter_target_snapshot');
CALL crest_rename_table_if_needed('visualization_report_filter', 'core_visualization_report_filter');
CALL crest_rename_table_if_needed('visualization_subject', 'core_visualization_theme');
CALL crest_rename_table_if_needed('visualization_watermark', 'core_visualization_watermark');
CALL crest_rename_table_if_needed('visualization_template', 'core_template');
CALL crest_rename_table_if_needed('visualization_template_category', 'core_template_category');
CALL crest_rename_table_if_needed('visualization_template_category_map', 'core_template_category_rel');
CALL crest_rename_table_if_needed('visualization_template_extend_data', 'core_template_view_data');
CALL crest_rename_table_if_needed('template_version', 'core_template_init_history');
CALL crest_rename_table_if_needed('export_task', 'core_export_task');
CALL crest_rename_table_if_needed('export_download_task', 'core_export_download');
CALL crest_rename_table_if_needed('font_asset', 'core_font_asset');
CALL crest_rename_table_if_needed('iam_menu', 'core_iam_menu');
CALL crest_rename_table_if_needed('iam_user', 'core_iam_user');
CALL crest_rename_table_if_needed('iam_org', 'core_iam_org');
CALL crest_rename_table_if_needed('iam_role', 'core_iam_role');
CALL crest_rename_table_if_needed('iam_user_org', 'core_iam_user_org');
CALL crest_rename_table_if_needed('iam_user_role', 'core_iam_user_role');
CALL crest_rename_table_if_needed('iam_role_menu_permission', 'core_iam_role_menu_permission');
CALL crest_rename_table_if_needed('iam_resource_index', 'core_iam_resource_index');
CALL crest_rename_table_if_needed('iam_resource_permission', 'core_iam_resource_permission');
CALL crest_rename_table_if_needed('auth_setting', 'core_auth_setting');
CALL crest_rename_table_if_needed('auth_sso_provider', 'core_auth_sso_provider');
CALL crest_rename_table_if_needed('auth_sso_identity_binding', 'core_auth_sso_identity_binding');
CALL crest_rename_table_if_needed('platform_token', 'core_auth_token');
CALL crest_rename_table_if_needed('crypto_key', 'core_crypto_key');
CALL crest_rename_table_if_needed('share_link', 'core_share_link');
CALL crest_rename_table_if_needed('share_ticket', 'core_share_ticket');
CALL crest_rename_table_if_needed('workspace_recent_resource', 'core_workspace_recent_resource');
CALL crest_rename_table_if_needed('workspace_favorite_resource', 'core_workspace_favorite_resource');
CALL crest_rename_table_if_needed('sys_setting', 'core_system_setting');
CALL crest_rename_table_if_needed('startup_job', 'core_system_startup_job');
CALL crest_rename_table_if_needed('threshold_rule', 'core_alert_rule');
CALL crest_rename_table_if_needed('threshold_instance', 'core_alert_instance');
CALL crest_rename_table_if_needed('webhook_config', 'core_webhook_config');
CALL crest_rename_table_if_needed('plugin_registry', 'core_plugin_registry');
CALL crest_rename_table_if_needed('audit_log', 'core_audit_log');
CALL crest_rename_table_if_needed('scheduled_task_queue_state', 'core_scheduled_task_queue_state');

-- 兼容历史环境中可能出现的 typeface_asset 表。
DROP PROCEDURE IF EXISTS crest_merge_typeface_asset;
DELIMITER //
CREATE PROCEDURE crest_merge_typeface_asset()
BEGIN
  DECLARE typeface_count INT DEFAULT 0;
  DECLARE font_count INT DEFAULT 0;

  SELECT COUNT(1) INTO typeface_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'typeface_asset';

  SELECT COUNT(1) INTO font_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'core_font_asset';

  IF typeface_count = 1 AND font_count = 0 THEN
    RENAME TABLE `typeface_asset` TO `core_font_asset`;
  ELSEIF typeface_count = 1 AND font_count = 1 THEN
    INSERT INTO `core_font_asset` (`id`, `name`, `file_name`, `file_trans_name`, `is_default`)
    SELECT `id`, `name`, `file_name`, `file_trans_name`, `is_default`
    FROM `typeface_asset` t
    WHERE NOT EXISTS (SELECT 1 FROM `core_font_asset` f WHERE f.`id` = t.`id`);
    DROP TABLE `typeface_asset`;
  END IF;
END//
DELIMITER ;

CALL crest_merge_typeface_asset();
DROP PROCEDURE IF EXISTS crest_merge_typeface_asset;
DROP PROCEDURE IF EXISTS crest_rename_table_if_needed;
