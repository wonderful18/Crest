-- Crest 2.0.1 运行配置标识迁移。
-- 本脚本在元数据表统一命名后执行，处理已保存的数据集、图表、可视化、模板和外部参数配置。

DROP PROCEDURE IF EXISTS crest_replace_text_if_needed;
DELIMITER //
CREATE PROCEDURE crest_replace_text_if_needed(
  IN target_table_name VARCHAR(128),
  IN target_column_name VARCHAR(128),
  IN old_value VARCHAR(64),
  IN new_value VARCHAR(64)
)
BEGIN
  DECLARE column_count INT DEFAULT 0;

  SELECT COUNT(1) INTO column_count
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = target_table_name
    AND column_name = target_column_name;

  IF column_count = 1 THEN
    SET @update_sql = CONCAT(
      'UPDATE `', target_table_name, '` ',
      'SET `', target_column_name, '` = REPLACE(`', target_column_name, '`, ', QUOTE(old_value), ', ', QUOTE(new_value), ') ',
      'WHERE `', target_column_name, '` LIKE ', QUOTE(CONCAT('%', old_value, '%'))
    );
    PREPARE update_stmt FROM @update_sql;
    EXECUTE update_stmt;
    DEALLOCATE PREPARE update_stmt;
  END IF;
END//

DROP PROCEDURE IF EXISTS crest_normalize_config_column//
CREATE PROCEDURE crest_normalize_config_column(
  IN target_table_name VARCHAR(128),
  IN target_column_name VARCHAR(128)
)
BEGIN
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, '|DE|', '|DATASET_PARAM|');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, '-de-', ' > ');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'DE_CAST_DATE_FORMAT', 'CREST_CAST_DATE_FORMAT');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'DE_UNIX_TIMESTAMP', 'CREST_UNIX_TIMESTAMP');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'DE_STR_TO_DATE', 'CREST_STR_TO_DATE');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'DE_FROM_UNIXTIME', 'CREST_FROM_UNIXTIME');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'DE_DATE_FORMAT', 'CREST_DATE_FORMAT');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'de-rich-text', 'rich-text');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'de-reset-button', 'crest-reset-button');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'de-button', 'crest-button');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'de-template-data', 'crest-template-data');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'de-report-filter-', 'crest-report-filter-');
  CALL crest_replace_text_if_needed(target_table_name, target_column_name, 'de-canvas-', 'crest-canvas-');
END//
DELIMITER ;

-- 外部参数和跳转目标字段只保存字段 ID。
CALL crest_replace_text_if_needed('core_visualization_parameter_target', 'target_field_id', '|DE|', '|DATASET_PARAM|');
CALL crest_replace_text_if_needed('core_visualization_parameter_target_snapshot', 'target_field_id', '|DE|', '|DATASET_PARAM|');
CALL crest_replace_text_if_needed('core_visualization_jump_target', 'target_field_id', '|DE|', '|DATASET_PARAM|');
CALL crest_replace_text_if_needed('core_visualization_jump_target_snapshot', 'target_field_id', '|DE|', '|DATASET_PARAM|');

-- 外部参数默认值可能保存树层级值。
CALL crest_normalize_config_column('core_visualization_parameter_item', 'default_value');
CALL crest_normalize_config_column('core_visualization_parameter_item_snapshot', 'default_value');

-- 数据集计算字段和数据表配置可能保存内置日期函数。
CALL crest_normalize_config_column('core_dataset_field', 'origin_name');
CALL crest_normalize_config_column('core_dataset_field', 'group_list');
CALL crest_normalize_config_column('core_dataset_field', 'other_group');
CALL crest_normalize_config_column('core_dataset_field', 'params');
CALL crest_normalize_config_column('core_dataset_table', 'info');
CALL crest_normalize_config_column('core_dataset_table', 'sql_variable_details');

-- 图表字段、过滤、高级配置和样式配置。
CALL crest_normalize_config_column('core_chart_view', 'x_axis');
CALL crest_normalize_config_column('core_chart_view', 'x_axis_ext');
CALL crest_normalize_config_column('core_chart_view', 'y_axis');
CALL crest_normalize_config_column('core_chart_view', 'y_axis_ext');
CALL crest_normalize_config_column('core_chart_view', 'ext_stack');
CALL crest_normalize_config_column('core_chart_view', 'ext_bubble');
CALL crest_normalize_config_column('core_chart_view', 'ext_label');
CALL crest_normalize_config_column('core_chart_view', 'ext_tooltip');
CALL crest_normalize_config_column('core_chart_view', 'custom_attr');
CALL crest_normalize_config_column('core_chart_view', 'custom_attr_mobile');
CALL crest_normalize_config_column('core_chart_view', 'custom_style');
CALL crest_normalize_config_column('core_chart_view', 'custom_style_mobile');
CALL crest_normalize_config_column('core_chart_view', 'custom_filter');
CALL crest_normalize_config_column('core_chart_view', 'drill_fields');
CALL crest_normalize_config_column('core_chart_view', 'senior');
CALL crest_normalize_config_column('core_chart_view', 'view_fields');
CALL crest_normalize_config_column('core_chart_view', 'flow_map_start_name');
CALL crest_normalize_config_column('core_chart_view', 'flow_map_end_name');
CALL crest_normalize_config_column('core_chart_view', 'ext_color');
CALL crest_normalize_config_column('core_chart_view', 'sort_priority');

CALL crest_normalize_config_column('core_chart_view_snapshot', 'x_axis');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'x_axis_ext');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'y_axis');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'y_axis_ext');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'ext_stack');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'ext_bubble');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'ext_label');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'ext_tooltip');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'custom_attr');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'custom_attr_mobile');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'custom_style');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'custom_style_mobile');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'custom_filter');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'drill_fields');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'senior');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'view_fields');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'flow_map_start_name');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'flow_map_end_name');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'ext_color');
CALL crest_normalize_config_column('core_chart_view_snapshot', 'sort_priority');

-- 大屏、仪表盘、定时报告和模板配置。
CALL crest_normalize_config_column('core_visualization', 'canvas_style_data');
CALL crest_normalize_config_column('core_visualization', 'component_data');
CALL crest_normalize_config_column('core_visualization_snapshot', 'canvas_style_data');
CALL crest_normalize_config_column('core_visualization_snapshot', 'component_data');
CALL crest_normalize_config_column('core_visualization_report_filter', 'filter_info');

CALL crest_normalize_config_column('core_template', 'template_style');
CALL crest_normalize_config_column('core_template', 'template_data');
CALL crest_normalize_config_column('core_template', 'dynamic_data');
CALL crest_normalize_config_column('core_template', 'app_data');
CALL crest_normalize_config_column('core_template_view_data', 'view_details');

DROP PROCEDURE IF EXISTS crest_normalize_config_column;
DROP PROCEDURE IF EXISTS crest_replace_text_if_needed;
