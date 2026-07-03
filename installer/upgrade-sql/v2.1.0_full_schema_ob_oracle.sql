-- Crest v2.1.0 OceanBase Oracle 空库一次性初始化 SQL。
-- 仅适用于新建 Crest 系统库；已有系统库请执行对应增量升级 SQL。
-- 本文件直接使用 v2.1.0 最终命名，不包含升级过程中的重命名 SQL。
-- 本文件由 installer/upgrade-sql/generate-full-schema.mjs 生成。

-- ----------------------------------------------------------------------
-- Source: core/core-backend/src/main/resources/db/migration-ob-oracle/V1.5.0.1__initial_schema.sql
-- ----------------------------------------------------------------------
-- Crest v1.5.0 fresh-install baseline for OceanBase Oracle tenants.
-- MySQL baseline semantics are preserved while using Oracle-compatible DDL and DML.

CREATE TABLE core_schedule_blob_triggers (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  TRIGGER_NAME VARCHAR2(200) NOT NULL,
  TRIGGER_GROUP VARCHAR2(200) NOT NULL,
  BLOB_DATA BLOB,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE core_schedule_calendars (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  CALENDAR_NAME VARCHAR2(200) NOT NULL,
  CALENDAR BLOB NOT NULL,
  PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);

CREATE TABLE core_schedule_cron_triggers (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  TRIGGER_NAME VARCHAR2(200) NOT NULL,
  TRIGGER_GROUP VARCHAR2(200) NOT NULL,
  CRON_EXPRESSION VARCHAR2(200) NOT NULL,
  TIME_ZONE_ID VARCHAR2(80) DEFAULT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE core_schedule_fired_triggers (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  ENTRY_ID VARCHAR2(95) NOT NULL,
  TRIGGER_NAME VARCHAR2(200) NOT NULL,
  TRIGGER_GROUP VARCHAR2(200) NOT NULL,
  INSTANCE_NAME VARCHAR2(200) NOT NULL,
  FIRED_TIME NUMBER(19,0) NOT NULL,
  SCHED_TIME NUMBER(19,0) NOT NULL,
  PRIORITY NUMBER(10,0) NOT NULL,
  STATE VARCHAR2(16) NOT NULL,
  JOB_NAME VARCHAR2(200) DEFAULT NULL,
  JOB_GROUP VARCHAR2(200) DEFAULT NULL,
  IS_NONCONCURRENT VARCHAR2(1) DEFAULT NULL,
  REQUESTS_RECOVERY VARCHAR2(1) DEFAULT NULL,
  PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);

CREATE TABLE core_schedule_job_details (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  JOB_NAME VARCHAR2(200) NOT NULL,
  JOB_GROUP VARCHAR2(200) NOT NULL,
  DESCRIPTION VARCHAR2(250) DEFAULT NULL,
  JOB_CLASS_NAME VARCHAR2(250) NOT NULL,
  IS_DURABLE VARCHAR2(1) NOT NULL,
  IS_NONCONCURRENT VARCHAR2(1) NOT NULL,
  IS_UPDATE_DATA VARCHAR2(1) NOT NULL,
  REQUESTS_RECOVERY VARCHAR2(1) NOT NULL,
  JOB_DATA BLOB,
  PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE core_schedule_locks (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  LOCK_NAME VARCHAR2(40) NOT NULL,
  PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);

CREATE TABLE core_schedule_paused_trigger_groups (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  TRIGGER_GROUP VARCHAR2(200) NOT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);

CREATE TABLE core_schedule_scheduler_state (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  INSTANCE_NAME VARCHAR2(200) NOT NULL,
  LAST_CHECKIN_TIME NUMBER(19,0) NOT NULL,
  CHECKIN_INTERVAL NUMBER(19,0) NOT NULL,
  PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);

CREATE TABLE core_schedule_simple_triggers (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  TRIGGER_NAME VARCHAR2(200) NOT NULL,
  TRIGGER_GROUP VARCHAR2(200) NOT NULL,
  REPEAT_COUNT NUMBER(19,0) NOT NULL,
  REPEAT_INTERVAL NUMBER(19,0) NOT NULL,
  TIMES_TRIGGERED NUMBER(19,0) NOT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE core_schedule_simprop_triggers (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  TRIGGER_NAME VARCHAR2(200) NOT NULL,
  TRIGGER_GROUP VARCHAR2(200) NOT NULL,
  STR_PROP_1 VARCHAR2(512) DEFAULT NULL,
  STR_PROP_2 VARCHAR2(512) DEFAULT NULL,
  STR_PROP_3 VARCHAR2(512) DEFAULT NULL,
  INT_PROP_1 NUMBER(10,0) DEFAULT NULL,
  INT_PROP_2 NUMBER(10,0) DEFAULT NULL,
  LONG_PROP_1 NUMBER(19,0) DEFAULT NULL,
  LONG_PROP_2 NUMBER(19,0) DEFAULT NULL,
  DEC_PROP_1 NUMBER(13,4) DEFAULT NULL,
  DEC_PROP_2 NUMBER(13,4) DEFAULT NULL,
  BOOL_PROP_1 VARCHAR2(1) DEFAULT NULL,
  BOOL_PROP_2 VARCHAR2(1) DEFAULT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE core_schedule_triggers (
  SCHED_NAME VARCHAR2(120) NOT NULL,
  TRIGGER_NAME VARCHAR2(200) NOT NULL,
  TRIGGER_GROUP VARCHAR2(200) NOT NULL,
  JOB_NAME VARCHAR2(200) NOT NULL,
  JOB_GROUP VARCHAR2(200) NOT NULL,
  DESCRIPTION VARCHAR2(250) DEFAULT NULL,
  NEXT_FIRE_TIME NUMBER(19,0) DEFAULT NULL,
  PREV_FIRE_TIME NUMBER(19,0) DEFAULT NULL,
  PRIORITY NUMBER(10,0) DEFAULT NULL,
  TRIGGER_STATE VARCHAR2(16) NOT NULL,
  TRIGGER_TYPE VARCHAR2(8) NOT NULL,
  START_TIME NUMBER(19,0) NOT NULL,
  END_TIME NUMBER(19,0) DEFAULT NULL,
  CALENDAR_NAME VARCHAR2(200) DEFAULT NULL,
  MISFIRE_INSTR NUMBER(5,0) DEFAULT NULL,
  JOB_DATA BLOB,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE core_reference_area (
  id VARCHAR2(255) NOT NULL,
  "LEVEL" VARCHAR2(255) DEFAULT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  pid VARCHAR2(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_api_traffic_limit (
  id NUMBER(19,0) NOT NULL,
  api VARCHAR2(255) NOT NULL,
  threshold NUMBER(10,0) NOT NULL DEFAULT 2,
  alive NUMBER(10,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE core_reference_custom_area (
  id VARCHAR2(255) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  pid VARCHAR2(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_chart_view (
  id NUMBER(19,0) NOT NULL,
  title VARCHAR2(1024) DEFAULT NULL,
  scene_id NUMBER(19,0) NOT NULL,
  table_id NUMBER(19,0) DEFAULT NULL,
  type VARCHAR2(50) DEFAULT NULL,
  render VARCHAR2(50) DEFAULT NULL,
  result_count NUMBER(10,0) DEFAULT NULL,
  result_mode VARCHAR2(50) DEFAULT NULL,
  x_axis CLOB,
  x_axis_ext CLOB,
  y_axis CLOB,
  y_axis_ext CLOB,
  ext_stack CLOB,
  ext_bubble CLOB,
  ext_label CLOB,
  ext_tooltip CLOB,
  custom_attr CLOB,
  custom_attr_mobile CLOB,
  custom_style CLOB,
  custom_style_mobile CLOB,
  custom_filter CLOB,
  drill_fields CLOB,
  senior CLOB,
  create_by VARCHAR2(50) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  snapshot CLOB,
  style_priority VARCHAR2(255) DEFAULT 'panel',
  chart_type VARCHAR2(255) DEFAULT 'private',
  is_plugin NUMBER(1,0) DEFAULT NULL,
  data_from VARCHAR2(255) DEFAULT 'dataset',
  view_fields CLOB,
  refresh_view_enable NUMBER(3,0) DEFAULT 0,
  refresh_unit VARCHAR2(255) DEFAULT 'minute',
  refresh_time NUMBER(10,0) DEFAULT 5,
  linkage_active NUMBER(3,0) DEFAULT 0,
  jump_active NUMBER(3,0) DEFAULT 0,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  aggregate NUMBER(1,0) DEFAULT NULL,
  flow_map_start_name CLOB,
  flow_map_end_name CLOB,
  ext_color CLOB,
  sort_priority CLOB,
  PRIMARY KEY (id)
);

CREATE TABLE core_query_assistant_config (
  id NUMBER(19,0) NOT NULL,
  copilot_url VARCHAR2(255) DEFAULT NULL,
  username VARCHAR2(255) DEFAULT NULL,
  pwd VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_query_assistant_message (
  id NUMBER(19,0) NOT NULL,
  user_id NUMBER(19,0) DEFAULT NULL,
  dataset_group_id NUMBER(19,0) DEFAULT NULL,
  msg_type VARCHAR2(255) DEFAULT NULL,
  engine_type VARCHAR2(255) DEFAULT NULL,
  schema_sql CLOB,
  question CLOB,
  history CLOB,
  copilot_sql CLOB,
  api_msg CLOB,
  sql_ok NUMBER(10,0) DEFAULT NULL,
  chart_ok NUMBER(10,0) DEFAULT NULL,
  chart CLOB,
  chart_data CLOB,
  exec_sql CLOB,
  msg_status NUMBER(10,0) DEFAULT NULL,
  err_msg CLOB,
  create_time NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_query_assistant_token (
  id NUMBER(19,0) NOT NULL,
  type VARCHAR2(255) DEFAULT NULL,
  token CLOB,
  update_time NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_geo_area (
  id VARCHAR2(50) NOT NULL,
  name VARCHAR2(50) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_geo_sub_area (
  id NUMBER(19,0) NOT NULL,
  name VARCHAR2(50) NOT NULL,
  scope VARCHAR2(1024) DEFAULT NULL,
  geo_area_id VARCHAR2(50) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_dataset (
  id NUMBER(19,0) NOT NULL,
  name VARCHAR2(128) DEFAULT NULL,
  pid NUMBER(19,0) DEFAULT NULL,
  "LEVEL" NUMBER(10,0) DEFAULT 0,
  node_type VARCHAR2(50) NOT NULL,
  type VARCHAR2(50) DEFAULT NULL,
  "MODE" NUMBER(10,0) DEFAULT 0,
  info CLOB,
  create_by VARCHAR2(50) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  scheduler_fire_instance_id VARCHAR2(1024) DEFAULT NULL,
  sync_status VARCHAR2(45) DEFAULT NULL,
  update_by VARCHAR2(50) DEFAULT NULL,
  last_update_time NUMBER(19,0) DEFAULT 0,
  union_sql CLOB,
  is_cross NUMBER(1,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_dataset_sync_task (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  dataset_group_id NUMBER(19,0) NOT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  update_type VARCHAR2(50) DEFAULT 'all_scope',
  incremental_field_id NUMBER(19,0) DEFAULT NULL,
  incremental_last_value VARCHAR2(255) DEFAULT NULL,
  start_time NUMBER(19,0) DEFAULT NULL,
  sync_rate VARCHAR2(50) DEFAULT 'RIGHTNOW',
  cron VARCHAR2(255) DEFAULT NULL,
  simple_cron_value NUMBER(19,0) DEFAULT NULL,
  simple_cron_type VARCHAR2(50) DEFAULT NULL,
  end_time NUMBER(19,0) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  last_exec_time NUMBER(19,0) DEFAULT NULL,
  last_exec_status VARCHAR2(50) DEFAULT NULL,
  task_status VARCHAR2(50) DEFAULT 'WaitingForExecution',
  cache_ready NUMBER(3,0) DEFAULT 0,
  heartbeat_time NUMBER(19,0) DEFAULT NULL,
  schema_hash VARCHAR2(128) DEFAULT NULL,
  full_sync_interval_hours NUMBER(10,0) DEFAULT 24,
  last_full_sync_time NUMBER(19,0) DEFAULT NULL,
  verify_enabled NUMBER(3,0) DEFAULT 1,
  last_verify_time NUMBER(19,0) DEFAULT NULL,
  last_verify_status VARCHAR2(50) DEFAULT NULL,
  last_verify_message CLOB,
  last_source_row_count NUMBER(19,0) DEFAULT NULL,
  last_cache_row_count NUMBER(19,0) DEFAULT NULL,
  cache_expire_hours NUMBER(10,0) DEFAULT 26,
  task_timeout_minutes NUMBER(10,0) DEFAULT 360,
  consecutive_failures NUMBER(10,0) DEFAULT 0,
  failure_warn_threshold NUMBER(10,0) DEFAULT 1,
  PRIMARY KEY (id),
  CONSTRAINT idx_dataset_sync_task_dataset UNIQUE (dataset_group_id)
);

CREATE TABLE core_dataset_sync_task_log (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  dataset_group_id NUMBER(19,0) NOT NULL,
  task_id NUMBER(19,0) DEFAULT NULL,
  update_type VARCHAR2(50) DEFAULT NULL,
  table_name VARCHAR2(255) DEFAULT NULL,
  start_time NUMBER(19,0) DEFAULT NULL,
  end_time NUMBER(19,0) DEFAULT NULL,
  task_status VARCHAR2(50) DEFAULT NULL,
  row_count NUMBER(19,0) DEFAULT 0,
  info CLOB,
  create_time NUMBER(19,0) DEFAULT NULL,
  trigger_type VARCHAR2(50) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_asset_profile (
  id NUMBER(19,0) NOT NULL,
  asset_type VARCHAR2(32) NOT NULL,
  asset_id VARCHAR2(128) NOT NULL,
  description VARCHAR2(1024) DEFAULT NULL,
  owner_id NUMBER(19,0) DEFAULT NULL,
  certified NUMBER(3,0) NOT NULL DEFAULT 0,
  recommended NUMBER(3,0) NOT NULL DEFAULT 0,
  deprecated NUMBER(3,0) NOT NULL DEFAULT 0,
  tags VARCHAR2(1024) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  update_by NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT idx_data_asset_profile_asset UNIQUE (asset_type, asset_id)
);

CREATE TABLE core_dataset_table (
  id NUMBER(19,0) NOT NULL,
  name VARCHAR2(128) DEFAULT NULL,
  table_name VARCHAR2(128) DEFAULT NULL,
  datasource_id NUMBER(19,0) DEFAULT NULL,
  dataset_group_id NUMBER(19,0) NOT NULL,
  type VARCHAR2(50) DEFAULT NULL,
  info CLOB,
  sql_variable_details CLOB,
  PRIMARY KEY (id)
);

CREATE TABLE core_dataset_field (
  id NUMBER(19,0) NOT NULL,
  datasource_id NUMBER(19,0) DEFAULT NULL,
  dataset_table_id NUMBER(19,0) DEFAULT NULL,
  dataset_group_id NUMBER(19,0) DEFAULT NULL,
  chart_id NUMBER(19,0) DEFAULT NULL,
  origin_name VARCHAR2(1024) NOT NULL,
  name VARCHAR2(1024),
  description VARCHAR2(2048),
  engine_field_name VARCHAR2(255) DEFAULT NULL,
  field_short_name VARCHAR2(255) DEFAULT NULL,
  group_list CLOB,
  other_group CLOB,
  group_type VARCHAR2(50) DEFAULT NULL,
  type VARCHAR2(255) NOT NULL,
  "SIZE" NUMBER(10,0) DEFAULT NULL,
  field_type NUMBER(10,0) NOT NULL,
  extracted_field_type NUMBER(10,0) NOT NULL,
  ext_field NUMBER(10,0) DEFAULT NULL,
  checked NUMBER(3,0) DEFAULT 1,
  column_index NUMBER(10,0) DEFAULT NULL,
  last_sync_time NUMBER(19,0) DEFAULT NULL,
  accuracy NUMBER(10,0) DEFAULT 0,
  date_format VARCHAR2(255) DEFAULT NULL,
  date_format_type VARCHAR2(255) DEFAULT NULL,
  params CLOB,
  order_checked NUMBER(3,0) DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE core_dataset_sql_log (
  id VARCHAR2(50) NOT NULL DEFAULT '',
  table_id VARCHAR2(50) NOT NULL DEFAULT '',
  start_time NUMBER(19,0) DEFAULT NULL,
  end_time NUMBER(19,0) DEFAULT NULL,
  spend NUMBER(19,0) DEFAULT NULL,
  sql CLOB NOT NULL,
  status VARCHAR2(45) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_datasource (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  name VARCHAR2(255) NOT NULL,
  description VARCHAR2(255) DEFAULT NULL,
  type VARCHAR2(50) NOT NULL,
  pid NUMBER(19,0) DEFAULT NULL,
  edit_type VARCHAR2(50) DEFAULT NULL,
  configuration CLOB,
  create_time NUMBER(19,0) NOT NULL,
  update_time NUMBER(19,0) NOT NULL,
  update_by NUMBER(19,0) DEFAULT NULL,
  create_by VARCHAR2(50) DEFAULT NULL,
  status CLOB,
  scheduler_fire_instance_id CLOB,
  task_status VARCHAR2(50) DEFAULT NULL,
  enable_data_fill NUMBER(3,0) DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE core_datasource_sync_task (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  ds_id NUMBER(19,0) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  update_type VARCHAR2(50) NOT NULL,
  start_time NUMBER(19,0) DEFAULT NULL,
  sync_rate VARCHAR2(50) NOT NULL,
  cron VARCHAR2(255) DEFAULT NULL,
  simple_cron_value NUMBER(19,0) DEFAULT NULL,
  simple_cron_type VARCHAR2(50) DEFAULT NULL,
  end_limit VARCHAR2(50) DEFAULT NULL,
  end_time NUMBER(19,0) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  last_exec_time NUMBER(19,0) DEFAULT NULL,
  last_exec_status VARCHAR2(50) DEFAULT NULL,
  extra_data CLOB,
  task_status VARCHAR2(50) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_datasource_sync_task_log (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  ds_id NUMBER(19,0) NOT NULL,
  task_id NUMBER(19,0) DEFAULT NULL,
  start_time NUMBER(19,0) DEFAULT NULL,
  end_time NUMBER(19,0) DEFAULT NULL,
  task_status VARCHAR2(50) NOT NULL,
  table_name VARCHAR2(255) NOT NULL,
  info CLOB,
  create_time NUMBER(19,0) DEFAULT NULL,
  trigger_type VARCHAR2(45) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_datasource_engine (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  name VARCHAR2(50) DEFAULT NULL,
  description VARCHAR2(50) DEFAULT NULL,
  type VARCHAR2(50) NOT NULL,
  configuration CLOB,
  create_time NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  create_by VARCHAR2(50) DEFAULT NULL,
  status VARCHAR2(45) DEFAULT NULL,
  enable_data_fill NUMBER(3,0) DEFAULT 1,
  PRIMARY KEY (id)
);

CREATE TABLE core_datasource_driver (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  name VARCHAR2(50) NOT NULL,
  create_time NUMBER(19,0) NOT NULL,
  type VARCHAR2(255) DEFAULT NULL,
  driver_class VARCHAR2(255) DEFAULT NULL,
  description VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_datasource_driver_jar (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  driver_id VARCHAR2(50) NOT NULL,
  file_name VARCHAR2(255) DEFAULT NULL,
  version VARCHAR2(255) DEFAULT NULL,
  driver_class CLOB,
  trans_name VARCHAR2(255) DEFAULT NULL,
  is_trans_name NUMBER(3,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_datasource_finish_page (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_export_download_task (
  id VARCHAR2(255) NOT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  valid_time NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_export_task (
  id VARCHAR2(255) NOT NULL,
  user_id NUMBER(19,0) NOT NULL,
  file_name VARCHAR2(2048) DEFAULT NULL,
  file_size BINARY_DOUBLE DEFAULT NULL,
  file_size_unit VARCHAR2(255) DEFAULT NULL,
  export_from VARCHAR2(255) DEFAULT NULL,
  export_status VARCHAR2(255) DEFAULT NULL,
  export_from_type VARCHAR2(255) DEFAULT NULL,
  export_time NUMBER(19,0) DEFAULT NULL,
  export_progress VARCHAR2(255) DEFAULT NULL,
  export_machine_name VARCHAR2(512) DEFAULT NULL,
  params CLOB NOT NULL,
  msg CLOB,
  PRIMARY KEY (id)
);

CREATE TABLE core_font_asset (
  id NUMBER(19,0) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  file_name VARCHAR2(255) DEFAULT NULL,
  file_trans_name VARCHAR2(255) DEFAULT NULL,
  is_default NUMBER(3,0) DEFAULT 0,
  update_time NUMBER(19,0) NOT NULL,
  is_BuiltIn NUMBER(3,0) DEFAULT 0,
  "SIZE" BINARY_DOUBLE DEFAULT NULL,
  size_type VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_iam_menu (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  pid NUMBER(19,0) NOT NULL,
  type NUMBER(10,0) DEFAULT NULL,
  name VARCHAR2(45) DEFAULT NULL,
  component VARCHAR2(45) DEFAULT NULL,
  menu_sort NUMBER(10,0) DEFAULT NULL,
  icon VARCHAR2(45) DEFAULT NULL,
  path VARCHAR2(45) DEFAULT NULL,
  hidden NUMBER(3,0) NOT NULL DEFAULT 0,
  in_layout NUMBER(3,0) NOT NULL DEFAULT 1,
  auth NUMBER(3,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE core_workspace_recent_resource (
  id NUMBER(19,0) NOT NULL,
  resource_id NUMBER(19,0) DEFAULT NULL,
  resource_name VARCHAR2(255) DEFAULT NULL,
  "UID" NUMBER(19,0) NOT NULL,
  resource_type NUMBER(10,0) NOT NULL,
  opt_type NUMBER(10,0) DEFAULT NULL,
  time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_auth_token (
  id NUMBER(10,0) NOT NULL,
  token VARCHAR2(255) NOT NULL,
  create_time NUMBER(19,0) NOT NULL,
  exp_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_plugin_registry (
  id NUMBER(19,0) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  icon CLOB NOT NULL,
  version VARCHAR2(255) NOT NULL,
  install_time NUMBER(19,0) NOT NULL,
  flag VARCHAR2(255) NOT NULL,
  developer VARCHAR2(255) NOT NULL,
  config CLOB NOT NULL,
  require_version VARCHAR2(255) NOT NULL,
  module_name VARCHAR2(255) NOT NULL,
  jar_name VARCHAR2(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_crypto_key (
  id NUMBER(10,0) NOT NULL,
  private_key CLOB NOT NULL,
  public_key CLOB NOT NULL,
  create_time NUMBER(19,0) NOT NULL,
  aes_key CLOB NOT NULL,
  sm2_private_key CLOB DEFAULT NULL,
  sm2_public_key CLOB DEFAULT NULL,
  sm2_create_time NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_auth_setting (
  id NUMBER(19,0) NOT NULL,
  name VARCHAR2(100) NOT NULL,
  type VARCHAR2(10) NOT NULL,
  enable NUMBER(3,0) NOT NULL,
  sync_time NUMBER(19,0) NOT NULL,
  relational_ids VARCHAR2(255) DEFAULT NULL,
  plugin_json CLOB,
  synced NUMBER(3,0) NOT NULL DEFAULT 0,
  valid NUMBER(3,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE core_share_link (
  id NUMBER(19,0) NOT NULL,
  creator NUMBER(19,0) NOT NULL,
  time NUMBER(19,0) NOT NULL,
  exp NUMBER(19,0) DEFAULT NULL,
  uuid VARCHAR2(16) NOT NULL,
  pwd VARCHAR2(255) DEFAULT NULL,
  resource_id NUMBER(19,0) NOT NULL,
  oid NUMBER(19,0) NOT NULL,
  type NUMBER(10,0) NOT NULL,
  auto_pwd NUMBER(3,0) NOT NULL DEFAULT 1,
  ticket_require NUMBER(3,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE core_share_ticket (
  id NUMBER(19,0) NOT NULL,
  uuid VARCHAR2(255) NOT NULL,
  ticket VARCHAR2(255) NOT NULL,
  exp NUMBER(19,0) DEFAULT NULL,
  args CLOB,
  access_time NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_workspace_favorite_resource (
  id NUMBER(19,0) NOT NULL,
  resource_id NUMBER(19,0) NOT NULL,
  "UID" NUMBER(19,0) NOT NULL,
  resource_type NUMBER(10,0) NOT NULL,
  time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_system_setting (
  id NUMBER(19,0) NOT NULL,
  pkey VARCHAR2(255) NOT NULL,
  pval CLOB,
  type VARCHAR2(255) NOT NULL,
  sort NUMBER(10,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE core_system_startup_job (
  id VARCHAR2(64) NOT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  status VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_alert_rule (
  id NUMBER(19,0) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  enable NUMBER(3,0) NOT NULL,
  rate_type NUMBER(10,0) NOT NULL,
  rate_value VARCHAR2(255) NOT NULL,
  resource_type VARCHAR2(50) NOT NULL,
  resource_id NUMBER(19,0) NOT NULL,
  chart_type VARCHAR2(255) NOT NULL,
  chart_id NUMBER(19,0) NOT NULL,
  threshold_rules CLOB,
  recisetting VARCHAR2(50) NOT NULL DEFAULT 0,
  reci_users CLOB,
  reci_roles CLOB,
  reci_emails CLOB,
  reci_lark_groups CLOB,
  reci_larksuite_groups CLOB,
  reci_webhooks CLOB,
  msg_title VARCHAR2(255) NOT NULL,
  msg_type NUMBER(10,0) NOT NULL DEFAULT 0,
  msg_content CLOB,
  repeat_send NUMBER(3,0) NOT NULL DEFAULT 1,
  show_field_value NUMBER(3,0) NOT NULL DEFAULT 0,
  status NUMBER(3,0) NOT NULL DEFAULT 0,
  creator NUMBER(19,0) NOT NULL,
  creator_name VARCHAR2(255) NOT NULL,
  create_time NUMBER(19,0) NOT NULL,
  oid NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_alert_instance (
  id NUMBER(19,0) NOT NULL,
  task_id NUMBER(19,0) NOT NULL,
  exec_time NUMBER(19,0) NOT NULL,
  status NUMBER(3,0) NOT NULL DEFAULT 0,
  content CLOB,
  msg CLOB,
  PRIMARY KEY (id)
);

CREATE TABLE core_webhook_config (
  id NUMBER(19,0) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  url VARCHAR2(255) NOT NULL,
  content_type VARCHAR2(255) NOT NULL,
  secret VARCHAR2(255) DEFAULT NULL,
  ssl NUMBER(3,0) NOT NULL DEFAULT 0,
  oid NUMBER(19,0) NOT NULL,
  create_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_iam_user (
  id NUMBER(19,0) NOT NULL,
  account VARCHAR2(64) NOT NULL,
  name VARCHAR2(128) NOT NULL,
  email VARCHAR2(255) DEFAULT NULL,
  phone_prefix VARCHAR2(16) DEFAULT NULL,
  phone VARCHAR2(64) DEFAULT NULL,
  password_hash VARCHAR2(512) NOT NULL,
  enable NUMBER(3,0) NOT NULL DEFAULT 1,
  is_admin NUMBER(3,0) NOT NULL DEFAULT 0,
  origin NUMBER(10,0) NOT NULL DEFAULT 0,
  auth_type VARCHAR2(32) DEFAULT 'LOCAL' NOT NULL,
  external_id VARCHAR2(128) DEFAULT NULL,
  last_login_time NUMBER(19,0) DEFAULT NULL,
  create_time NUMBER(19,0) NOT NULL,
  update_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT idx_crest_user_account UNIQUE (account)
);

CREATE TABLE core_visualization (
  id VARCHAR2(50) NOT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  pid VARCHAR2(50) DEFAULT NULL,
  org_id VARCHAR2(50) DEFAULT NULL,
  "LEVEL" NUMBER(10,0) DEFAULT NULL,
  node_type VARCHAR2(255) DEFAULT NULL,
  type VARCHAR2(50) DEFAULT NULL,
  canvas_style_data CLOB,
  component_data CLOB,
  mobile_layout NUMBER(3,0) DEFAULT 0,
  status NUMBER(10,0) DEFAULT 1,
  self_watermark_status NUMBER(10,0) DEFAULT 0,
  sort NUMBER(10,0) DEFAULT 0,
  create_time NUMBER(19,0) DEFAULT NULL,
  create_by VARCHAR2(255) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  update_by VARCHAR2(255) DEFAULT NULL,
  remark VARCHAR2(255) DEFAULT NULL,
  source VARCHAR2(255) DEFAULT NULL,
  delete_flag NUMBER(3,0) DEFAULT 0,
  delete_time NUMBER(19,0) DEFAULT NULL,
  delete_by VARCHAR2(255) DEFAULT NULL,
  version NUMBER(10,0) DEFAULT 3,
  content_id VARCHAR2(50) DEFAULT 0,
  check_version VARCHAR2(50) DEFAULT 1,
  PRIMARY KEY (id)
);

CREATE TABLE core_template_init_history (
  installed_rank NUMBER(10,0) NOT NULL,
  version VARCHAR2(50) DEFAULT NULL,
  description VARCHAR2(200) DEFAULT NULL,
  type VARCHAR2(20) DEFAULT NULL,
  script VARCHAR2(1000) NOT NULL,
  checksum NUMBER(10,0) DEFAULT NULL,
  installed_by VARCHAR2(100) DEFAULT NULL,
  installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  execution_time NUMBER(10,0) DEFAULT NULL,
  success NUMBER(3,0) NOT NULL,
  PRIMARY KEY (installed_rank)
);

CREATE TABLE core_chart_view_snapshot (
  id NUMBER(19,0) NOT NULL,
  title VARCHAR2(1024) DEFAULT NULL,
  scene_id NUMBER(19,0) NOT NULL,
  table_id NUMBER(19,0) DEFAULT NULL,
  type VARCHAR2(50) DEFAULT NULL,
  render VARCHAR2(50) DEFAULT NULL,
  result_count NUMBER(10,0) DEFAULT NULL,
  result_mode VARCHAR2(50) DEFAULT NULL,
  x_axis CLOB,
  x_axis_ext CLOB,
  y_axis CLOB,
  y_axis_ext CLOB,
  ext_stack CLOB,
  ext_bubble CLOB,
  ext_label CLOB,
  ext_tooltip CLOB,
  custom_attr CLOB,
  custom_attr_mobile CLOB,
  custom_style CLOB,
  custom_style_mobile CLOB,
  custom_filter CLOB,
  drill_fields CLOB,
  senior CLOB,
  create_by VARCHAR2(50) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  snapshot CLOB,
  style_priority VARCHAR2(255) DEFAULT 'panel',
  chart_type VARCHAR2(255) DEFAULT 'private',
  is_plugin NUMBER(1,0) DEFAULT NULL,
  data_from VARCHAR2(255) DEFAULT 'dataset',
  view_fields CLOB,
  refresh_view_enable NUMBER(3,0) DEFAULT 0,
  refresh_unit VARCHAR2(255) DEFAULT 'minute',
  refresh_time NUMBER(10,0) DEFAULT 5,
  linkage_active NUMBER(3,0) DEFAULT 0,
  jump_active NUMBER(3,0) DEFAULT 0,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  aggregate NUMBER(1,0) DEFAULT NULL,
  flow_map_start_name CLOB,
  flow_map_end_name CLOB,
  ext_color CLOB,
  sort_priority CLOB,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_snapshot (
  id VARCHAR2(50) NOT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  pid VARCHAR2(50) DEFAULT NULL,
  org_id VARCHAR2(50) DEFAULT NULL,
  "LEVEL" NUMBER(10,0) DEFAULT NULL,
  node_type VARCHAR2(255) DEFAULT NULL,
  type VARCHAR2(50) DEFAULT NULL,
  canvas_style_data CLOB,
  component_data CLOB,
  mobile_layout NUMBER(3,0) DEFAULT 0,
  status NUMBER(10,0) DEFAULT 1,
  self_watermark_status NUMBER(10,0) DEFAULT 0,
  sort NUMBER(10,0) DEFAULT 0,
  create_time NUMBER(19,0) DEFAULT NULL,
  create_by VARCHAR2(255) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  update_by VARCHAR2(255) DEFAULT NULL,
  remark VARCHAR2(255) DEFAULT NULL,
  source VARCHAR2(255) DEFAULT NULL,
  delete_flag NUMBER(3,0) DEFAULT 0,
  delete_time NUMBER(19,0) DEFAULT NULL,
  delete_by VARCHAR2(255) DEFAULT NULL,
  version NUMBER(10,0) DEFAULT 3,
  content_id VARCHAR2(50) DEFAULT 0,
  check_version VARCHAR2(50) DEFAULT 1,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_jump_snapshot (
  id NUMBER(19,0) NOT NULL,
  source_dv_id NUMBER(19,0) DEFAULT NULL,
  source_view_id NUMBER(19,0) DEFAULT NULL,
  link_jump_info VARCHAR2(4000) DEFAULT NULL,
  checked NUMBER(3,0) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_jump_action_snapshot (
  id NUMBER(19,0) NOT NULL,
  link_jump_id NUMBER(19,0) DEFAULT NULL,
  link_type VARCHAR2(255) DEFAULT NULL,
  jump_type VARCHAR2(255) DEFAULT NULL,
  target_dv_id NUMBER(19,0) DEFAULT NULL,
  source_field_id NUMBER(19,0) DEFAULT NULL,
  content VARCHAR2(4000) DEFAULT NULL,
  checked NUMBER(3,0) DEFAULT NULL,
  attach_params NUMBER(3,0) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  window_size VARCHAR2(255) DEFAULT 'middle',
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_jump_target_snapshot (
  target_id NUMBER(19,0) NOT NULL,
  link_jump_info_id NUMBER(19,0) DEFAULT NULL,
  source_field_active_id NUMBER(19,0) DEFAULT NULL,
  target_view_id VARCHAR2(50) DEFAULT NULL,
  target_field_id VARCHAR2(50) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  target_type VARCHAR2(50) DEFAULT 'view',
  PRIMARY KEY (target_id)
);

CREATE TABLE core_visualization_linkage_snapshot (
  id NUMBER(19,0) NOT NULL,
  dv_id NUMBER(19,0) DEFAULT NULL,
  source_view_id NUMBER(19,0) DEFAULT NULL,
  target_view_id NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  update_people VARCHAR2(255) DEFAULT NULL,
  linkage_active NUMBER(3,0) DEFAULT 0,
  ext1 VARCHAR2(2000) DEFAULT NULL,
  ext2 VARCHAR2(2000) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_linkage_field_snapshot (
  id NUMBER(19,0) NOT NULL,
  linkage_id NUMBER(19,0) DEFAULT NULL,
  source_field NUMBER(19,0) DEFAULT NULL,
  target_field NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_parameter_snapshot (
  params_id VARCHAR2(50) NOT NULL,
  visualization_id VARCHAR2(50) DEFAULT NULL,
  checked NUMBER(3,0) DEFAULT NULL,
  remark VARCHAR2(255) DEFAULT NULL,
  copy_from VARCHAR2(50) DEFAULT NULL,
  copy_id VARCHAR2(50) DEFAULT NULL,
  PRIMARY KEY (params_id)
);

CREATE TABLE core_visualization_parameter_item_snapshot (
  params_info_id VARCHAR2(50) NOT NULL,
  params_id VARCHAR2(50) DEFAULT NULL,
  param_name VARCHAR2(255) DEFAULT NULL,
  checked NUMBER(3,0) DEFAULT NULL,
  copy_from VARCHAR2(255) DEFAULT NULL,
  copy_id VARCHAR2(50) DEFAULT NULL,
  required NUMBER(3,0) DEFAULT 0,
  default_value VARCHAR2(255) DEFAULT NULL,
  enabled_default NUMBER(3,0) DEFAULT 0,
  PRIMARY KEY (params_info_id)
);

CREATE TABLE core_visualization_parameter_target_snapshot (
  target_id VARCHAR2(50) NOT NULL,
  params_info_id VARCHAR2(50) DEFAULT NULL,
  target_view_id VARCHAR2(50) DEFAULT NULL,
  target_field_id VARCHAR2(50) DEFAULT NULL,
  copy_from VARCHAR2(255) DEFAULT NULL,
  copy_id VARCHAR2(50) DEFAULT NULL,
  target_ds_id VARCHAR2(50) DEFAULT NULL,
  match_mode VARCHAR2(255) DEFAULT 'self',
  PRIMARY KEY (target_id)
);

CREATE TABLE core_visualization_background (
  id VARCHAR2(64) NOT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  classification VARCHAR2(255) NOT NULL,
  content CLOB,
  remark VARCHAR2(255) DEFAULT NULL,
  sort NUMBER(10,0) DEFAULT NULL,
  upload_time NUMBER(19,0) DEFAULT NULL,
  base_url VARCHAR2(255) DEFAULT NULL,
  url VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_background_image (
  id VARCHAR2(64) NOT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  classification VARCHAR2(255) NOT NULL,
  content CLOB,
  remark VARCHAR2(255) DEFAULT NULL,
  sort NUMBER(10,0) DEFAULT NULL,
  upload_time NUMBER(19,0) DEFAULT NULL,
  base_url VARCHAR2(255) DEFAULT NULL,
  url VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_jump (
  id NUMBER(19,0) NOT NULL,
  source_dv_id NUMBER(19,0) DEFAULT NULL,
  source_view_id NUMBER(19,0) DEFAULT NULL,
  link_jump_info VARCHAR2(4000) DEFAULT NULL,
  checked NUMBER(3,0) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_jump_action (
  id NUMBER(19,0) NOT NULL,
  link_jump_id NUMBER(19,0) DEFAULT NULL,
  link_type VARCHAR2(255) DEFAULT NULL,
  jump_type VARCHAR2(255) DEFAULT NULL,
  target_dv_id NUMBER(19,0) DEFAULT NULL,
  source_field_id NUMBER(19,0) DEFAULT NULL,
  content VARCHAR2(4000) DEFAULT NULL,
  checked NUMBER(3,0) DEFAULT NULL,
  attach_params NUMBER(3,0) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  window_size VARCHAR2(255) DEFAULT 'middle',
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_jump_target (
  target_id NUMBER(19,0) NOT NULL,
  link_jump_info_id NUMBER(19,0) DEFAULT NULL,
  source_field_active_id NUMBER(19,0) DEFAULT NULL,
  target_view_id VARCHAR2(50) DEFAULT NULL,
  target_field_id VARCHAR2(50) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  target_type VARCHAR2(50) DEFAULT 'view',
  PRIMARY KEY (target_id)
);

CREATE TABLE core_visualization_linkage (
  id NUMBER(19,0) NOT NULL,
  dv_id NUMBER(19,0) DEFAULT NULL,
  source_view_id NUMBER(19,0) DEFAULT NULL,
  target_view_id NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  update_people VARCHAR2(255) DEFAULT NULL,
  linkage_active NUMBER(3,0) DEFAULT 0,
  ext1 VARCHAR2(2000) DEFAULT NULL,
  ext2 VARCHAR2(2000) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_linkage_field (
  id NUMBER(19,0) NOT NULL,
  linkage_id NUMBER(19,0) DEFAULT NULL,
  source_field NUMBER(19,0) DEFAULT NULL,
  target_field NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  copy_from NUMBER(19,0) DEFAULT NULL,
  copy_id NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_parameter (
  params_id VARCHAR2(50) NOT NULL,
  visualization_id VARCHAR2(50) DEFAULT NULL,
  checked NUMBER(3,0) DEFAULT NULL,
  remark VARCHAR2(255) DEFAULT NULL,
  copy_from VARCHAR2(50) DEFAULT NULL,
  copy_id VARCHAR2(50) DEFAULT NULL,
  PRIMARY KEY (params_id)
);

CREATE TABLE core_visualization_parameter_item (
  params_info_id VARCHAR2(50) NOT NULL,
  params_id VARCHAR2(50) DEFAULT NULL,
  param_name VARCHAR2(255) DEFAULT NULL,
  checked NUMBER(3,0) DEFAULT NULL,
  copy_from VARCHAR2(255) DEFAULT NULL,
  copy_id VARCHAR2(50) DEFAULT NULL,
  required NUMBER(3,0) DEFAULT 0,
  default_value VARCHAR2(255) DEFAULT NULL,
  enabled_default NUMBER(3,0) DEFAULT 0,
  PRIMARY KEY (params_info_id)
);

CREATE TABLE core_visualization_parameter_target (
  target_id VARCHAR2(50) NOT NULL,
  params_info_id VARCHAR2(50) DEFAULT NULL,
  target_view_id VARCHAR2(50) DEFAULT NULL,
  target_field_id VARCHAR2(50) DEFAULT NULL,
  copy_from VARCHAR2(255) DEFAULT NULL,
  copy_id VARCHAR2(50) DEFAULT NULL,
  target_ds_id VARCHAR2(50) DEFAULT NULL,
  match_mode VARCHAR2(255) DEFAULT 'self',
  PRIMARY KEY (target_id)
);

CREATE TABLE core_visualization_report_filter (
  id NUMBER(19,0) NOT NULL,
  report_id NUMBER(19,0) DEFAULT NULL,
  task_id NUMBER(19,0) DEFAULT NULL,
  resource_id NUMBER(19,0) DEFAULT NULL,
  dv_type VARCHAR2(255) DEFAULT NULL,
  component_id NUMBER(19,0) DEFAULT NULL,
  filter_id NUMBER(19,0) DEFAULT NULL,
  filter_info CLOB,
  filter_version NUMBER(10,0) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  create_user VARCHAR2(255) DEFAULT NULL
);

CREATE TABLE core_visualization_theme (
  id VARCHAR2(50) NOT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  type VARCHAR2(255) DEFAULT NULL,
  details CLOB,
  delete_flag NUMBER(3,0) DEFAULT 0,
  cover_url CLOB,
  create_num NUMBER(10,0) NOT NULL DEFAULT 0,
  create_time NUMBER(19,0) DEFAULT NULL,
  create_by VARCHAR2(255) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  update_by VARCHAR2(255) DEFAULT NULL,
  delete_time NUMBER(19,0) DEFAULT NULL,
  delete_by NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_template (
  id VARCHAR2(50) NOT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  pid VARCHAR2(255) DEFAULT NULL,
  "LEVEL" NUMBER(10,0) DEFAULT NULL,
  dv_type VARCHAR2(255) DEFAULT NULL,
  node_type VARCHAR2(255) DEFAULT NULL,
  create_by VARCHAR2(255) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  snapshot CLOB,
  template_type VARCHAR2(255) DEFAULT NULL,
  template_style CLOB,
  template_data CLOB,
  dynamic_data CLOB,
  app_data CLOB,
  use_count NUMBER(10,0) DEFAULT 0,
  version NUMBER(10,0) DEFAULT 3,
  PRIMARY KEY (id)
);

CREATE TABLE core_template_category (
  id VARCHAR2(50) NOT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  pid VARCHAR2(255) DEFAULT NULL,
  "LEVEL" NUMBER(10,0) DEFAULT NULL,
  dv_type VARCHAR2(255) DEFAULT NULL,
  node_type VARCHAR2(255) DEFAULT NULL,
  create_by VARCHAR2(255) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  snapshot CLOB,
  template_type VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_template_category_relation (
  id VARCHAR2(50) NOT NULL,
  category_id VARCHAR2(255) DEFAULT NULL,
  template_id VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_template_view_data (
  id NUMBER(19,0) NOT NULL,
  dv_id NUMBER(19,0) DEFAULT NULL,
  view_id NUMBER(19,0) DEFAULT NULL,
  view_details CLOB,
  copy_from VARCHAR2(255) DEFAULT NULL,
  copy_id VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_visualization_watermark (
  id VARCHAR2(50) NOT NULL,
  version VARCHAR2(255) DEFAULT NULL,
  setting_content CLOB,
  create_by VARCHAR2(255) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE core_auth_sso_provider (
  id NUMBER(19,0) NOT NULL,
  provider_key VARCHAR2(64) NOT NULL,
  provider_type VARCHAR2(32) NOT NULL,
  name VARCHAR2(128) NOT NULL,
  enabled NUMBER(3,0) NOT NULL DEFAULT 1,
  client_id VARCHAR2(191) DEFAULT NULL,
  client_secret CLOB DEFAULT NULL,
  authorization_endpoint CLOB DEFAULT NULL,
  token_endpoint CLOB DEFAULT NULL,
  user_info_endpoint CLOB DEFAULT NULL,
  issuer CLOB DEFAULT NULL,
  scope VARCHAR2(255) DEFAULT NULL,
  redirect_uri CLOB DEFAULT NULL,
  user_id_attribute VARCHAR2(128) DEFAULT NULL,
  account_attribute VARCHAR2(128) DEFAULT NULL,
  name_attribute VARCHAR2(128) DEFAULT NULL,
  email_attribute VARCHAR2(128) DEFAULT NULL,
  union_id_attribute VARCHAR2(128) DEFAULT NULL,
  auto_create_user NUMBER(3,0) DEFAULT 1 NOT NULL,
  require_https NUMBER(3,0) DEFAULT 1 NOT NULL,
  create_time NUMBER(19,0) NOT NULL,
  update_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_sso_provider_key UNIQUE (provider_key)
);

CREATE TABLE core_auth_sso_identity_binding (
  id NUMBER(19,0) NOT NULL,
  user_id NUMBER(19,0) NOT NULL,
  provider_id NUMBER(19,0) NOT NULL,
  provider_type VARCHAR2(32) NOT NULL,
  external_subject VARCHAR2(191) NOT NULL,
  account VARCHAR2(64) NOT NULL,
  display_name VARCHAR2(64) NOT NULL,
  email VARCHAR2(191) DEFAULT NULL,
  union_id VARCHAR2(191) DEFAULT NULL,
  last_login_time NUMBER(19,0) NOT NULL,
  create_time NUMBER(19,0) NOT NULL,
  update_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_sso_identity_subject UNIQUE (provider_id, external_subject),
  CONSTRAINT uk_sso_identity_union UNIQUE (provider_id, union_id)
);

CREATE TABLE core_audit_log (
  id NUMBER(19,0) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  operation_type VARCHAR2(50) NOT NULL,
  resource_type VARCHAR2(50) NOT NULL,
  resource_id VARCHAR2(100),
  resource_name VARCHAR2(200),
  operation_desc VARCHAR2(500),
  request_method VARCHAR2(10),
  request_url VARCHAR2(500),
  request_params CLOB,
  response_code NUMBER(10,0),
  response_msg VARCHAR2(500),
  operator_id NUMBER(19,0) NOT NULL,
  operator_name VARCHAR2(100),
  operator_account VARCHAR2(100),
  operator_ip VARCHAR2(50),
  operation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  duration NUMBER(19,0)
);

CREATE TABLE core_iam_org (
  id NUMBER(19,0) NOT NULL,
  pid NUMBER(19,0) DEFAULT NULL,
  name VARCHAR2(128) NOT NULL,
  code VARCHAR2(64) DEFAULT NULL,
  path VARCHAR2(1024) NOT NULL,
  "LEVEL" NUMBER(10,0) NOT NULL DEFAULT 0,
  sort NUMBER(10,0) NOT NULL DEFAULT 0,
  enable NUMBER(3,0) NOT NULL DEFAULT 1,
  readonly NUMBER(3,0) NOT NULL DEFAULT 0,
  create_time NUMBER(19,0) NOT NULL,
  update_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT idx_crest_org_code UNIQUE (code)
);

CREATE TABLE core_iam_role (
  id NUMBER(19,0) NOT NULL,
  oid NUMBER(19,0) NOT NULL,
  name VARCHAR2(128) NOT NULL,
  code VARCHAR2(64) DEFAULT NULL,
  description VARCHAR2(255) DEFAULT NULL,
  type_code NUMBER(10,0) NOT NULL DEFAULT 0,
  readonly NUMBER(3,0) NOT NULL DEFAULT 0,
  system_role NUMBER(3,0) NOT NULL DEFAULT 0,
  org_admin NUMBER(3,0) NOT NULL DEFAULT 0,
  create_time NUMBER(19,0) NOT NULL,
  update_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT idx_crest_role_oid_code UNIQUE (oid, code)
);

CREATE TABLE core_iam_user_org (
  id NUMBER(19,0) NOT NULL,
  "UID" NUMBER(19,0) NOT NULL,
  oid NUMBER(19,0) NOT NULL,
  default_org NUMBER(3,0) NOT NULL DEFAULT 0,
  create_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT idx_crest_user_org_uid_oid UNIQUE ("UID", oid)
);

CREATE TABLE core_iam_user_role (
  id NUMBER(19,0) NOT NULL,
  "UID" NUMBER(19,0) NOT NULL,
  oid NUMBER(19,0) NOT NULL,
  rid NUMBER(19,0) NOT NULL,
  create_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT idx_crest_user_role_uid_oid_rid UNIQUE ("UID", oid, rid)
);

CREATE TABLE core_iam_role_menu_permission (
  id NUMBER(19,0) NOT NULL,
  rid NUMBER(19,0) NOT NULL,
  menu_id NUMBER(19,0) NOT NULL,
  permission VARCHAR2(32) NOT NULL DEFAULT 'read',
  create_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT idx_crest_role_menu_perm UNIQUE (rid, menu_id, permission)
);

CREATE TABLE core_iam_resource_index (
  id NUMBER(19,0) NOT NULL,
  resource_id VARCHAR2(64) NOT NULL,
  resource_type VARCHAR2(32) NOT NULL,
  oid NUMBER(19,0) NOT NULL,
  creator NUMBER(19,0) DEFAULT NULL,
  name VARCHAR2(255) DEFAULT NULL,
  create_time NUMBER(19,0) DEFAULT NULL,
  update_time NUMBER(19,0) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT idx_crest_resource_unique UNIQUE (resource_type, resource_id)
);

CREATE TABLE core_iam_resource_permission (
  id NUMBER(19,0) NOT NULL,
  resource_type VARCHAR2(32) NOT NULL,
  resource_id VARCHAR2(64) NOT NULL,
  target_type VARCHAR2(16) NOT NULL,
  target_id NUMBER(19,0) NOT NULL,
  permission VARCHAR2(32) NOT NULL DEFAULT 'read',
  create_time NUMBER(19,0) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT idx_crest_resource_permission UNIQUE (resource_type, resource_id, target_type, target_id, permission)
);

-- Secondary indexes
CREATE INDEX idx_core_schedule_triggers_job ON core_schedule_triggers (SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX idx_dataset_sync_task_log_A ON core_dataset_sync_task_log (dataset_group_id,task_id,start_time);
CREATE INDEX idx_data_asset_profile_status ON core_asset_profile (asset_type, certified, recommended, deprecated);
CREATE INDEX idx_data_asset_profile_owner ON core_asset_profile (owner_id);
CREATE INDEX idx_dataset_table_task_log_ds_id ON core_datasource_sync_task_log (ds_id);
CREATE INDEX idx_dataset_table_task_log_task_id ON core_datasource_sync_task_log (task_id);
CREATE INDEX idx_dataset_table_task_log_A ON core_datasource_sync_task_log (ds_id,table_name,start_time);
CREATE INDEX idx_sso_provider_type ON core_auth_sso_provider (provider_type, enabled);
CREATE INDEX idx_sso_identity_user ON core_auth_sso_identity_binding (user_id, provider_id);
CREATE INDEX idx_sso_identity_account ON core_auth_sso_identity_binding (account);
CREATE INDEX idx_operation_time ON core_audit_log (operation_time);
CREATE INDEX idx_operator_id ON core_audit_log (operator_id);
CREATE INDEX idx_operation_type ON core_audit_log (operation_type);
CREATE INDEX idx_resource_type ON core_audit_log (resource_type);
CREATE INDEX idx_crest_org_pid ON core_iam_org (pid);
CREATE INDEX idx_crest_org_path ON core_iam_org (path);
CREATE INDEX idx_crest_role_oid ON core_iam_role (oid);
CREATE INDEX idx_crest_user_org_oid ON core_iam_user_org (oid);
CREATE INDEX idx_crest_user_role_rid ON core_iam_user_role (rid);
CREATE INDEX idx_crest_role_menu_id ON core_iam_role_menu_permission (menu_id);
CREATE INDEX idx_crest_resource_oid_type ON core_iam_resource_index (oid, resource_type);
CREATE INDEX idx_crest_resource_creator ON core_iam_resource_index (creator);
CREATE INDEX idx_crest_resource_permission_target ON core_iam_resource_permission (target_type, target_id);

-- Foreign keys
ALTER TABLE core_schedule_blob_triggers ADD CONSTRAINT fk_csch_blob_trigger FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES core_schedule_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
ALTER TABLE core_schedule_cron_triggers ADD CONSTRAINT fk_csch_cron_trigger FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES core_schedule_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
ALTER TABLE core_schedule_simple_triggers ADD CONSTRAINT fk_csch_simple_trigger FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES core_schedule_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
ALTER TABLE core_schedule_simprop_triggers ADD CONSTRAINT fk_csch_simprop_trigger FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES core_schedule_triggers (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
ALTER TABLE core_schedule_triggers ADD CONSTRAINT fk_csch_trigger_job FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP) REFERENCES core_schedule_job_details (SCHED_NAME, JOB_NAME, JOB_GROUP);

-- Baseline seed data
-- Seed for core_reference_area is loaded by ObOracleMetadataSeedInitializer.

UPDATE core_asset_profile SET certified = 0, recommended = 0 WHERE deprecated = 1;

INSERT INTO core_datasource_engine VALUES (1,'默认引擎','默认引擎','mysql','',NULL,NULL,NULL,NULL,1);

INSERT INTO core_font_asset VALUES (1,'PingFang',NULL,NULL,1,0,1,NULL,NULL);

INSERT INTO core_iam_menu VALUES (1,0,2,'workbranch','workbranch',1,NULL,'/workbranch',0,1,1);
INSERT INTO core_iam_menu VALUES (2,0,2,'panel','visualized/view/panel',2,NULL,'/panel',0,1,1);
INSERT INTO core_iam_menu VALUES (3,0,2,'screen','visualized/view/screen',3,NULL,'/screen',0,1,1);
INSERT INTO core_iam_menu VALUES (4,0,1,'data',NULL,5,NULL,'/data',0,1,0);
INSERT INTO core_iam_menu VALUES (5,4,2,'dataset','visualized/data/dataset',1,NULL,'/dataset',0,1,1);
INSERT INTO core_iam_menu VALUES (6,4,2,'datasource','visualized/data/datasource',2,NULL,'/datasource',0,1,1);
INSERT INTO core_iam_menu VALUES (11,0,2,'dataset-form','visualized/data/dataset/form',7,NULL,'/dataset-form',1,0,0);
INSERT INTO core_iam_menu VALUES (12,0,2,'datasource-form','visualized/data/datasource/form',7,NULL,'/ds-form',1,0,0);
INSERT INTO core_iam_menu VALUES (15,0,1,'sys-setting',NULL,6,NULL,'/sys-setting',1,1,0);
INSERT INTO core_iam_menu VALUES (16,15,2,'parameter','system/parameter',1,'sys-parameter','/parameter',0,1,0);
INSERT INTO core_iam_menu VALUES (19,0,2,'template-market','template-market',4,NULL,'/template-market',1,1,0);
INSERT INTO core_iam_menu VALUES (30,0,1,'toolbox',NULL,7,'icon_template','/toolbox',1,1,0);
INSERT INTO core_iam_menu VALUES (31,30,2,'template-setting','toolbox/template-setting',1,'icon_template','/template-setting',0,1,1);
INSERT INTO core_iam_menu VALUES (64,15,2,'font','system/font',10,'icon_font','/font',0,1,0);
INSERT INTO core_iam_menu VALUES (66,0,2,'association','visualized/data/lineage',4,'association','/lineage',0,1,1);
INSERT INTO core_iam_menu VALUES (67,15,2,'share-management','system/share',2,'icon_share-label_outlined','/share-management',0,1,1);
INSERT INTO core_iam_menu VALUES (68,15,2,'site-setting','system/site',3,'tab-title','/site-setting',0,1,1);
INSERT INTO core_iam_menu VALUES (69,15,2,'user-management','system/user',4,'icon_member_filled','/user-management',0,1,1);
INSERT INTO core_iam_menu VALUES (70,0,1,'msg',NULL,200,NULL,'/msg',1,1,0);


INSERT INTO core_system_setting VALUES (1,'basic.dsIntervalTime','6','text',11);
INSERT INTO core_system_setting VALUES (2,'basic.dsExecuteTime','minute','text',3);
INSERT INTO core_system_setting VALUES (3,'ai.baseUrl','','text',0);
INSERT INTO core_system_setting VALUES (7,'template.url','','text',0);
INSERT INTO core_system_setting VALUES (8,'template.accessKey','','text',1);
INSERT INTO core_system_setting VALUES (9,'basic.frontTimeOut','60','text',1);
INSERT INTO core_system_setting VALUES (10,'basic.exportFileLiveTime','30','text',2);
INSERT INTO core_system_setting VALUES (100102206800000001,'basic.siteTitle','Crest','text',9);
INSERT INTO core_system_setting VALUES (1048232869488627717,'basic.shareDisable','false','text',11);
INSERT INTO core_system_setting VALUES (1048232869488627718,'basic.sharePeRequire','false','text',12);
INSERT INTO core_system_setting VALUES (1048232869488627719,'basic.defaultSort','1','text',13);
INSERT INTO core_system_setting VALUES (1048232869488627720,'basic.defaultOpen','0','text',14);
INSERT INTO core_system_setting VALUES (1048232869488627721,'basic.initialPassword','','pwd',15);
INSERT INTO core_system_setting VALUES (1048232869488627722,'basic.pwdStrategy','true','text',16);
INSERT INTO core_system_setting VALUES (1048232869488627723,'basic.dip','false','text',17);
INSERT INTO core_system_setting VALUES (1048232869488627724,'basic.pvp','0','text',18);
INSERT INTO core_system_setting VALUES (1048232869488627725,'basic.loginLimit','false','text',19);
INSERT INTO core_system_setting VALUES (1048232869488627726,'basic.loginLimitRate','5','text',20);
INSERT INTO core_system_setting VALUES (1048232869488627727,'basic.loginLimitTime','30','text',21);

INSERT INTO core_system_startup_job VALUES ('chartFilterDynamic','chartFilterDynamic','done');
INSERT INTO core_system_startup_job VALUES ('chartFilterMerge','chartFilterMerge','done');
INSERT INTO core_system_startup_job VALUES ('datasetCrossListener','datasetCrossListener','done');

INSERT INTO core_iam_user (id, account, name, email, phone_prefix, phone, password_hash, enable, is_admin, origin, create_time, update_time)
VALUES (1,'admin','管理员',NULL,NULL,NULL,'21232f297a57a5a743894a0e4a801fc3',1,1,0,1779664240000,1779664240000);

INSERT INTO core_visualization_background VALUES ('board_1','1','default','',NULL,NULL,NULL,'img/board','board/board_1.svg');
INSERT INTO core_visualization_background VALUES ('board_2','2','default',NULL,NULL,NULL,NULL,'img/board','board/board_2.svg');
INSERT INTO core_visualization_background VALUES ('board_3','3','default',NULL,NULL,NULL,NULL,'img/board','board/board_3.svg');
INSERT INTO core_visualization_background VALUES ('board_4','4','default',NULL,NULL,NULL,NULL,'img/board','board/board_4.svg');
INSERT INTO core_visualization_background VALUES ('board_5','5','default',NULL,NULL,NULL,NULL,'img/board','board/board_5.svg');
INSERT INTO core_visualization_background VALUES ('board_6','6','default',NULL,NULL,NULL,NULL,'img/board','board/board_6.svg');
INSERT INTO core_visualization_background VALUES ('board_7','7','default',NULL,NULL,NULL,NULL,'img/board','board/board_7.svg');
INSERT INTO core_visualization_background VALUES ('board_8','8','default',NULL,NULL,NULL,NULL,'img/board','board/board_8.svg');
INSERT INTO core_visualization_background VALUES ('board_9','9','default',NULL,NULL,NULL,NULL,'img/board','board/board_9.svg');

-- Seed for core_visualization_theme is loaded by ObOracleMetadataSeedInitializer.

INSERT INTO core_visualization_watermark VALUES ('system_default','1.0','{"enable":false,"enablePanelCustom":true,"type":"custom","content":"水印","watermark_color":"#DD1010","watermark_x_space":12,"watermark_y_space":36,"watermark_fontsize":15}','admin',NULL);

INSERT INTO core_iam_menu VALUES (71,15,2,'single-sign-on','system/sso',5,'authentication','/single-sign-on',0,1,1);

INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000001,'sso.enabled','false','text',1);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000002,'sso.providerName','企业单点登录','text',2);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000003,'sso.clientId','','text',3);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000004,'sso.clientSecret','','text',4);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000005,'sso.authorizationEndpoint','','text',5);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000006,'sso.tokenEndpoint','','text',6);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000007,'sso.userInfoEndpoint','','text',7);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000008,'sso.issuer','','text',8);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000009,'sso.scope','openid profile email','text',9);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000010,'sso.redirectUri','','text',10);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000011,'sso.userIdAttribute','sub','text',11);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000012,'sso.accountAttribute','preferred_username','text',12);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000013,'sso.nameAttribute','name','text',13);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000014,'sso.emailAttribute','email','text',14);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000015,'sso.autoCreateUser','true','text',15);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000016,'sso.allowLocalLogin','true','text',16);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000017,'sso.requireHttps','true','text',17);
INSERT INTO core_system_setting (id, pkey, pval, type, sort) VALUES (100140000000000018,'sso.logoutRedirectUrl','','text',18);

UPDATE core_iam_user
SET name = '管理员'
WHERE id = 1
  AND (name LIKE '%<%' OR name LIKE '%>%');

UPDATE core_iam_user
SET name = account
WHERE id <> 1
  AND (name LIKE '%<%' OR name LIKE '%>%');

UPDATE core_system_setting
SET pval = '统一身份认证'
WHERE pkey = 'sso.providerName'
  AND DBMS_LOB.SUBSTR(pval, 4000, 1) = '企业单点登录';

INSERT INTO core_auth_sso_provider (id, provider_key, provider_type, name, enabled, create_time, update_time) VALUES (1, 'default', 'OIDC_GENERIC', '统一身份认证', 1, 0, 0);

INSERT INTO core_system_setting (id, pkey, pval, type, sort)
SELECT
    100140000000000019,
    'sso.providerType',
    CASE
        WHEN EXISTS (
            SELECT 1 FROM (
                SELECT pval FROM core_system_setting
                WHERE pkey IN ('sso.providerName', 'sso.issuer')
                  AND LOWER(DBMS_LOB.SUBSTR(pval, 4000, 1)) LIKE '%casdoor%'
            ) existing_provider
        ) THEN 'CASDOOR'
        ELSE 'OIDC_GENERIC'
    END,
    'text',
    19
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM core_system_setting WHERE pkey = 'sso.providerType'
    ) existing_setting
);

INSERT INTO core_system_setting (id, pkey, pval, type, sort)
SELECT 100140000000000020, 'sso.unionIdAttribute', '', 'text', 20
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM (
        SELECT id FROM core_system_setting WHERE pkey = 'sso.unionIdAttribute'
    ) existing_setting
);

UPDATE core_auth_sso_provider
SET
    provider_type = COALESCE((SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.providerType' FETCH FIRST 1 ROWS ONLY), provider_type),
    name = COALESCE((SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.providerName' FETCH FIRST 1 ROWS ONLY), name),
    enabled = COALESCE((SELECT CASE WHEN LOWER(DBMS_LOB.SUBSTR(pval, 4000, 1)) = 'true' THEN 1 ELSE 0 END FROM core_system_setting WHERE pkey = 'sso.enabled' FETCH FIRST 1 ROWS ONLY), enabled),
    client_id = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.clientId' FETCH FIRST 1 ROWS ONLY),
    client_secret = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.clientSecret' FETCH FIRST 1 ROWS ONLY),
    authorization_endpoint = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.authorizationEndpoint' FETCH FIRST 1 ROWS ONLY),
    token_endpoint = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.tokenEndpoint' FETCH FIRST 1 ROWS ONLY),
    user_info_endpoint = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.userInfoEndpoint' FETCH FIRST 1 ROWS ONLY),
    issuer = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.issuer' FETCH FIRST 1 ROWS ONLY),
    scope = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.scope' FETCH FIRST 1 ROWS ONLY),
    redirect_uri = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.redirectUri' FETCH FIRST 1 ROWS ONLY),
    user_id_attribute = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.userIdAttribute' FETCH FIRST 1 ROWS ONLY),
    account_attribute = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.accountAttribute' FETCH FIRST 1 ROWS ONLY),
    name_attribute = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.nameAttribute' FETCH FIRST 1 ROWS ONLY),
    email_attribute = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.emailAttribute' FETCH FIRST 1 ROWS ONLY),
    union_id_attribute = (SELECT DBMS_LOB.SUBSTR(pval, 4000, 1) FROM core_system_setting WHERE pkey = 'sso.unionIdAttribute' FETCH FIRST 1 ROWS ONLY),
    auto_create_user = COALESCE((SELECT CASE WHEN LOWER(DBMS_LOB.SUBSTR(pval, 4000, 1)) = 'true' THEN 1 ELSE 0 END FROM core_system_setting WHERE pkey = 'sso.autoCreateUser' FETCH FIRST 1 ROWS ONLY), auto_create_user),
    require_https = COALESCE((SELECT CASE WHEN LOWER(DBMS_LOB.SUBSTR(pval, 4000, 1)) = 'true' THEN 1 ELSE 0 END FROM core_system_setting WHERE pkey = 'sso.requireHttps' FETCH FIRST 1 ROWS ONLY), require_https),
    update_time = ROUND((CAST(SYSTIMESTAMP AS DATE) - DATE '1970-01-01') * 86400000)
WHERE provider_key = 'default';

INSERT INTO core_iam_org (id, pid, name, code, path, "LEVEL", sort, enable, readonly, create_time, update_time) VALUES (1, 0, '默认组织', 'default', '/1/', 0, 0, 1, 1, 1779664240000, 1779664240000);

INSERT INTO core_iam_role (id, oid, name, code, description, type_code, readonly, system_role, org_admin, create_time, update_time) VALUES (1, 1, '系统管理员', 'system_admin', '拥有全部系统管理和资源管理权限', 1, 1, 1, 1, 1779664240000, 1779664240000);
INSERT INTO core_iam_role (id, oid, name, code, description, type_code, readonly, system_role, org_admin, create_time, update_time) VALUES (2, 1, '普通用户', 'member', '默认业务使用角色', 2, 0, 1, 0, 1779664240000, 1779664240000);
INSERT INTO core_iam_role (id, oid, name, code, description, type_code, readonly, system_role, org_admin, create_time, update_time) VALUES (3, 1, '审计只读', 'auditor', '面向审计和巡检场景的只读角色', 3, 1, 1, 0, 1779664240000, 1779664240000);

INSERT INTO core_iam_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (73, 15, 2, 'org-management', 'system/org', 5, 'org', '/org-management', 0, 1, 1);
INSERT INTO core_iam_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (74, 15, 2, 'role-management', 'system/role', 6, 'peoples', '/role-management', 0, 1, 1);
INSERT INTO core_iam_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (75, 15, 2, 'permission-management', 'system/permission', 7, 'auth', '/permission-management', 0, 1, 1);
INSERT INTO core_iam_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (76, 15, 2, 'audit-log', 'system/audit-log', 8, 'log', '/audit-log', 0, 1, 1);
INSERT INTO core_iam_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (77, 4, 2, 'data-asset', 'visualized/data/asset', 3, 'association', '/asset', 0, 1, 1);
INSERT INTO core_iam_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (78, 4, 2, 'cache-task', 'visualized/data/cache-task', 4, 'task', '/cache-task', 0, 1, 1);

INSERT INTO core_iam_user_org (id, "UID", oid, default_org, create_time)
SELECT id, id, 1, 1, COALESCE(create_time, 1779664240000) FROM core_iam_user;

INSERT INTO core_iam_user_role (id, "UID", oid, rid, create_time)
SELECT 100000 + ROW_NUMBER() OVER (ORDER BY u.id),
       u.id, 1, CASE WHEN u.is_admin = 1 THEN 1 ELSE 2 END, COALESCE(u.create_time, 1779664240000)
FROM core_iam_user u;

INSERT INTO core_iam_role_menu_permission (id, rid, menu_id, permission, create_time)
SELECT id * 100 + 1, 1, id, 'manage', 1779664240000 FROM core_iam_menu WHERE auth = 1 OR id IN (15, 16, 64, 67, 68, 69, 71, 72);

INSERT INTO core_iam_role_menu_permission (id, rid, menu_id, permission, create_time)
SELECT id * 100 + 2, 2, id, 'read', 1779664240000 FROM core_iam_menu WHERE auth = 1 AND id NOT IN (15, 16, 64, 67, 68, 69, 71, 72, 73, 74, 75, 76);

INSERT INTO core_iam_role_menu_permission (id, rid, menu_id, permission, create_time)
SELECT id * 100 + 3, 3, id, 'read', 1779664240000 FROM core_iam_menu WHERE auth = 1 AND id NOT IN (15, 16, 64, 67, 68, 69, 71, 72, 73, 74, 75);

INSERT INTO core_iam_resource_index (id, resource_id, resource_type, oid, creator, name, create_time, update_time)
SELECT 1000000000000000 + ROW_NUMBER() OVER (ORDER BY ds.id), TO_CHAR(ds.id), 'datasource', 1, CAST(ds.create_by AS NUMBER(19,0)), ds.name, ds.create_time, ds.update_time
FROM core_datasource ds
WHERE ds.type <> 'folder';

INSERT INTO core_iam_resource_index (id, resource_id, resource_type, oid, creator, name, create_time, update_time)
SELECT 2000000000000000 + ROW_NUMBER() OVER (ORDER BY dg.id), TO_CHAR(dg.id), 'dataset', 1, CAST(dg.create_by AS NUMBER(19,0)), dg.name, dg.create_time, dg.last_update_time
FROM core_dataset dg
WHERE dg.node_type = 'dataset';

INSERT INTO core_iam_resource_index (id, resource_id, resource_type, oid, creator, name, create_time, update_time)
SELECT 3000000000000000 + ROW_NUMBER() OVER (ORDER BY v.id), TO_CHAR(v.id), CASE WHEN v.type = 'dataV' THEN 'screen' ELSE 'panel' END,
       COALESCE(CAST(NULLIF(v.org_id, '') AS NUMBER(19,0)), 1), CAST(v.create_by AS NUMBER(19,0)), v.name, v.create_time, v.update_time
FROM core_visualization v
WHERE v.delete_flag = 0 AND v.node_type = 'leaf';

UPDATE core_iam_menu
SET icon = CASE id
    WHEN 73 THEN 'org'
    WHEN 74 THEN 'peoples'
    WHEN 75 THEN 'auth'
    ELSE icon
END
WHERE id IN (73, 74, 75);

UPDATE core_iam_role
SET readonly = 0
WHERE id = 2 AND code = 'member';

DELETE FROM core_iam_role_menu_permission WHERE menu_id = 73;

DELETE FROM core_iam_menu WHERE id = 73;


-- ----------------------------------------------------------------------
-- Source: core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.1__export_task_queue.sql
-- ----------------------------------------------------------------------
ALTER TABLE core_export_task ADD (
  worker_id VARCHAR2(128) DEFAULT NULL,
  heartbeat_time NUMBER(19,0) DEFAULT NULL,
  retry_count NUMBER(10,0) DEFAULT 0,
  lock_version NUMBER(19,0) DEFAULT 0,
  next_fire_time NUMBER(19,0) DEFAULT NULL,
  last_enqueue_time NUMBER(19,0) DEFAULT NULL,
  last_error CLOB
);


-- ----------------------------------------------------------------------
-- Source: core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.2__datasource_sync_task_queue.sql
-- ----------------------------------------------------------------------
ALTER TABLE core_datasource_sync_task ADD (
  worker_id VARCHAR2(128) DEFAULT NULL,
  heartbeat_time NUMBER(19,0) DEFAULT NULL,
  retry_count NUMBER(10,0) DEFAULT 0,
  lock_version NUMBER(19,0) DEFAULT 0,
  next_fire_time NUMBER(19,0) DEFAULT NULL,
  last_enqueue_time NUMBER(19,0) DEFAULT NULL,
  last_error CLOB
);

CREATE INDEX idx_ds_sync_task_enqueue ON core_datasource_sync_task (task_status, last_enqueue_time);
CREATE INDEX idx_ds_sync_task_heartbeat ON core_datasource_sync_task (task_status, heartbeat_time);


-- ----------------------------------------------------------------------
-- Source: core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.3__dataset_sync_task_queue.sql
-- ----------------------------------------------------------------------
ALTER TABLE core_dataset_sync_task ADD (
  last_enqueue_time NUMBER(19,0) DEFAULT NULL
);

CREATE INDEX idx_dataset_sync_task_enqueue ON core_dataset_sync_task (task_status, last_enqueue_time);


-- ----------------------------------------------------------------------
-- Source: core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.4__dataset_sync_task_queue_metadata.sql
-- ----------------------------------------------------------------------
ALTER TABLE core_dataset_sync_task ADD (
  worker_id VARCHAR2(128) DEFAULT NULL,
  retry_count NUMBER(10,0) DEFAULT 0,
  lock_version NUMBER(19,0) DEFAULT 0,
  next_fire_time NUMBER(19,0) DEFAULT NULL,
  last_error CLOB
);

CREATE INDEX idx_dataset_sync_task_hb ON core_dataset_sync_task (task_status, heartbeat_time);


-- ----------------------------------------------------------------------
-- Source: core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.5__scheduled_task_queue_state.sql
-- ----------------------------------------------------------------------
CREATE TABLE core_scheduled_task_queue_state (
  state_id VARCHAR2(64) NOT NULL,
  task_type VARCHAR2(50) NOT NULL,
  task_id VARCHAR2(128) DEFAULT NULL,
  payload_hash VARCHAR2(64) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  worker_id VARCHAR2(128) DEFAULT NULL,
  enqueue_time NUMBER(19,0) NOT NULL,
  start_time NUMBER(19,0) DEFAULT NULL,
  end_time NUMBER(19,0) DEFAULT NULL,
  heartbeat_time NUMBER(19,0) DEFAULT NULL,
  retry_count NUMBER(10,0) DEFAULT 0,
  lock_version NUMBER(19,0) DEFAULT 0,
  last_error CLOB,
  CONSTRAINT pk_scheduled_task_queue_state PRIMARY KEY (state_id)
);

CREATE INDEX idx_sched_task_queue_status ON core_scheduled_task_queue_state (status, heartbeat_time);
CREATE INDEX idx_sched_task_queue_task ON core_scheduled_task_queue_state (task_type, task_id);


-- ----------------------------------------------------------------------
-- Source: core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.7__audit_log_filter_index.sql
-- ----------------------------------------------------------------------
CREATE INDEX idx_audit_log_filter_time ON core_audit_log (operation_type, resource_type, operation_time);


-- ----------------------------------------------------------------------
-- Source: core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.2.2__rotate_default_crypto_key.sql
-- ----------------------------------------------------------------------
-- 清理早期初始化脚本写入的固定系统密钥。服务启动时会自动生成部署专属密钥。
DELETE FROM core_crypto_key
WHERE id = 1
  AND create_time = 1779664249927;

-- 表和字段备注
COMMENT ON TABLE core_schedule_blob_triggers IS '自定义触发器存储（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_blob_triggers.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_blob_triggers.TRIGGER_NAME IS '触发器名称';
COMMENT ON COLUMN core_schedule_blob_triggers.TRIGGER_GROUP IS '触发器分组';
COMMENT ON COLUMN core_schedule_blob_triggers.BLOB_DATA IS '触发器二进制数据';
COMMENT ON TABLE core_schedule_calendars IS 'Quartz日历（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_calendars.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_calendars.CALENDAR_NAME IS '日历名称';
COMMENT ON COLUMN core_schedule_calendars.CALENDAR IS '日历数据';
COMMENT ON TABLE core_schedule_cron_triggers IS 'CronTrigger存储（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_cron_triggers.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_cron_triggers.TRIGGER_NAME IS '触发器名称';
COMMENT ON COLUMN core_schedule_cron_triggers.TRIGGER_GROUP IS '触发器分组';
COMMENT ON COLUMN core_schedule_cron_triggers.CRON_EXPRESSION IS 'Cron 表达式';
COMMENT ON COLUMN core_schedule_cron_triggers.TIME_ZONE_ID IS '时区 ID';
COMMENT ON TABLE core_schedule_fired_triggers IS '存储已经触发的trigger相关信息（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_fired_triggers.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_fired_triggers.ENTRY_ID IS '触发记录 ID';
COMMENT ON COLUMN core_schedule_fired_triggers.TRIGGER_NAME IS '触发器名称';
COMMENT ON COLUMN core_schedule_fired_triggers.TRIGGER_GROUP IS '触发器分组';
COMMENT ON COLUMN core_schedule_fired_triggers.INSTANCE_NAME IS '调度实例名称';
COMMENT ON COLUMN core_schedule_fired_triggers.FIRED_TIME IS '触发时间';
COMMENT ON COLUMN core_schedule_fired_triggers.SCHED_TIME IS '计划触发时间';
COMMENT ON COLUMN core_schedule_fired_triggers.PRIORITY IS '优先级';
COMMENT ON COLUMN core_schedule_fired_triggers.STATE IS '状态';
COMMENT ON COLUMN core_schedule_fired_triggers.JOB_NAME IS '作业名称';
COMMENT ON COLUMN core_schedule_fired_triggers.JOB_GROUP IS '作业分组';
COMMENT ON COLUMN core_schedule_fired_triggers.IS_NONCONCURRENT IS '是否禁止并发';
COMMENT ON COLUMN core_schedule_fired_triggers.REQUESTS_RECOVERY IS '是否请求恢复';
COMMENT ON TABLE core_schedule_job_details IS '存储jobDetails信息（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_job_details.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_job_details.JOB_NAME IS '作业名称';
COMMENT ON COLUMN core_schedule_job_details.JOB_GROUP IS '作业分组';
COMMENT ON COLUMN core_schedule_job_details.DESCRIPTION IS '描述';
COMMENT ON COLUMN core_schedule_job_details.JOB_CLASS_NAME IS '作业类名';
COMMENT ON COLUMN core_schedule_job_details.IS_DURABLE IS '是否持久化';
COMMENT ON COLUMN core_schedule_job_details.IS_NONCONCURRENT IS '是否禁止并发';
COMMENT ON COLUMN core_schedule_job_details.IS_UPDATE_DATA IS '是否更新作业数据';
COMMENT ON COLUMN core_schedule_job_details.REQUESTS_RECOVERY IS '是否请求恢复';
COMMENT ON COLUMN core_schedule_job_details.JOB_DATA IS '作业数据';
COMMENT ON TABLE core_schedule_locks IS 'Quartz锁表，为多个节点调度提供分布式锁（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_locks.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_locks.LOCK_NAME IS '锁名称';
COMMENT ON TABLE core_schedule_paused_trigger_groups IS '存放暂停掉的触发器（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_paused_trigger_groups.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_paused_trigger_groups.TRIGGER_GROUP IS '触发器分组';
COMMENT ON TABLE core_schedule_scheduler_state IS '存储所有节点的scheduler（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_scheduler_state.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_scheduler_state.INSTANCE_NAME IS '调度实例名称';
COMMENT ON COLUMN core_schedule_scheduler_state.LAST_CHECKIN_TIME IS '最近心跳时间';
COMMENT ON COLUMN core_schedule_scheduler_state.CHECKIN_INTERVAL IS '心跳间隔';
COMMENT ON TABLE core_schedule_simple_triggers IS 'SimpleTrigger存储（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_simple_triggers.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_simple_triggers.TRIGGER_NAME IS '触发器名称';
COMMENT ON COLUMN core_schedule_simple_triggers.TRIGGER_GROUP IS '触发器分组';
COMMENT ON COLUMN core_schedule_simple_triggers.REPEAT_COUNT IS '重复次数';
COMMENT ON COLUMN core_schedule_simple_triggers.REPEAT_INTERVAL IS '重复间隔';
COMMENT ON COLUMN core_schedule_simple_triggers.TIMES_TRIGGERED IS '已触发次数';
COMMENT ON TABLE core_schedule_simprop_triggers IS '存储CalendarIntervalTrigger和DailyTimeIntervalTrigger两种类型的触发器（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_simprop_triggers.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_simprop_triggers.TRIGGER_NAME IS '触发器名称';
COMMENT ON COLUMN core_schedule_simprop_triggers.TRIGGER_GROUP IS '触发器分组';
COMMENT ON COLUMN core_schedule_simprop_triggers.STR_PROP_1 IS '字符串属性 1';
COMMENT ON COLUMN core_schedule_simprop_triggers.STR_PROP_2 IS '字符串属性 2';
COMMENT ON COLUMN core_schedule_simprop_triggers.STR_PROP_3 IS '字符串属性 3';
COMMENT ON COLUMN core_schedule_simprop_triggers.INT_PROP_1 IS '整数属性 1';
COMMENT ON COLUMN core_schedule_simprop_triggers.INT_PROP_2 IS '整数属性 2';
COMMENT ON COLUMN core_schedule_simprop_triggers.LONG_PROP_1 IS '长整数属性 1';
COMMENT ON COLUMN core_schedule_simprop_triggers.LONG_PROP_2 IS '长整数属性 2';
COMMENT ON COLUMN core_schedule_simprop_triggers.DEC_PROP_1 IS '小数属性 1';
COMMENT ON COLUMN core_schedule_simprop_triggers.DEC_PROP_2 IS '小数属性 2';
COMMENT ON COLUMN core_schedule_simprop_triggers.BOOL_PROP_1 IS '布尔属性 1';
COMMENT ON COLUMN core_schedule_simprop_triggers.BOOL_PROP_2 IS '布尔属性 2';
COMMENT ON TABLE core_schedule_triggers IS '存储定义的trigger（开源作业调度框架Quartz）';
COMMENT ON COLUMN core_schedule_triggers.SCHED_NAME IS '调度器名称';
COMMENT ON COLUMN core_schedule_triggers.TRIGGER_NAME IS '触发器名称';
COMMENT ON COLUMN core_schedule_triggers.TRIGGER_GROUP IS '触发器分组';
COMMENT ON COLUMN core_schedule_triggers.JOB_NAME IS '作业名称';
COMMENT ON COLUMN core_schedule_triggers.JOB_GROUP IS '作业分组';
COMMENT ON COLUMN core_schedule_triggers.DESCRIPTION IS '描述';
COMMENT ON COLUMN core_schedule_triggers.NEXT_FIRE_TIME IS '下次触发时间';
COMMENT ON COLUMN core_schedule_triggers.PREV_FIRE_TIME IS '上次触发时间';
COMMENT ON COLUMN core_schedule_triggers.PRIORITY IS '优先级';
COMMENT ON COLUMN core_schedule_triggers.TRIGGER_STATE IS '触发器状态';
COMMENT ON COLUMN core_schedule_triggers.TRIGGER_TYPE IS '触发器类型';
COMMENT ON COLUMN core_schedule_triggers.START_TIME IS '开始时间';
COMMENT ON COLUMN core_schedule_triggers.END_TIME IS '结束时间';
COMMENT ON COLUMN core_schedule_triggers.CALENDAR_NAME IS '日历名称';
COMMENT ON COLUMN core_schedule_triggers.MISFIRE_INSTR IS '错过触发处理策略';
COMMENT ON COLUMN core_schedule_triggers.JOB_DATA IS '作业数据';
COMMENT ON TABLE core_reference_area IS '地图区域表';
COMMENT ON COLUMN core_reference_area.id IS '区域id,和文件对应';
COMMENT ON COLUMN core_reference_area."LEVEL" IS '区域级别，从高到低country,province,city,district,更细的待定';
COMMENT ON COLUMN core_reference_area.name IS '区域名称';
COMMENT ON COLUMN core_reference_area.pid IS '父级区域id';
COMMENT ON TABLE core_api_traffic_limit IS 'API并发限流阈值设置表';
COMMENT ON COLUMN core_api_traffic_limit.id IS 'ID';
COMMENT ON COLUMN core_api_traffic_limit.api IS '接口';
COMMENT ON COLUMN core_api_traffic_limit.threshold IS '阈值';
COMMENT ON COLUMN core_api_traffic_limit.alive IS '活动并发';
COMMENT ON TABLE core_reference_custom_area IS '自定义地图区域信息表';
COMMENT ON COLUMN core_reference_custom_area.id IS '主键';
COMMENT ON COLUMN core_reference_custom_area.name IS '自定义区域名称';
COMMENT ON COLUMN core_reference_custom_area.pid IS '父级ID';
COMMENT ON TABLE core_chart_view IS '图表组件';
COMMENT ON COLUMN core_chart_view.id IS 'ID';
COMMENT ON COLUMN core_chart_view.title IS '标题';
COMMENT ON COLUMN core_chart_view.scene_id IS '所属可视化资源 ID';
COMMENT ON COLUMN core_chart_view.table_id IS '数据集表ID';
COMMENT ON COLUMN core_chart_view.type IS '图表类型';
COMMENT ON COLUMN core_chart_view.render IS '图表渲染方式';
COMMENT ON COLUMN core_chart_view.result_count IS '展示结果';
COMMENT ON COLUMN core_chart_view.result_mode IS '展示模式';
COMMENT ON COLUMN core_chart_view.x_axis IS '横轴字段配置';
COMMENT ON COLUMN core_chart_view.x_axis_ext IS '表格行扩展配置';
COMMENT ON COLUMN core_chart_view.y_axis IS '纵轴字段配置';
COMMENT ON COLUMN core_chart_view.y_axis_ext IS '副轴';
COMMENT ON COLUMN core_chart_view.ext_stack IS '堆叠项';
COMMENT ON COLUMN core_chart_view.ext_bubble IS '气泡大小';
COMMENT ON COLUMN core_chart_view.ext_label IS '动态标签';
COMMENT ON COLUMN core_chart_view.ext_tooltip IS '动态提示';
COMMENT ON COLUMN core_chart_view.custom_attr IS '图形属性';
COMMENT ON COLUMN core_chart_view.custom_attr_mobile IS '图形属性_移动端';
COMMENT ON COLUMN core_chart_view.custom_style IS '组件样式';
COMMENT ON COLUMN core_chart_view.custom_style_mobile IS '组件样式_移动端';
COMMENT ON COLUMN core_chart_view.custom_filter IS '结果过滤';
COMMENT ON COLUMN core_chart_view.drill_fields IS '钻取字段';
COMMENT ON COLUMN core_chart_view.senior IS '高级';
COMMENT ON COLUMN core_chart_view.create_by IS '创建人ID';
COMMENT ON COLUMN core_chart_view.create_time IS '创建时间';
COMMENT ON COLUMN core_chart_view.update_time IS '更新时间';
COMMENT ON COLUMN core_chart_view.snapshot IS '缩略图 ';
COMMENT ON COLUMN core_chart_view.style_priority IS '样式优先级 panel 仪表板 view 图表';
COMMENT ON COLUMN core_chart_view.chart_type IS '图表类型 public 公共 历史可复用的图表，private 私有 专属某个仪表板';
COMMENT ON COLUMN core_chart_view.is_plugin IS '是否插件';
COMMENT ON COLUMN core_chart_view.data_from IS '数据来源 template 模板数据 dataset 数据集数据';
COMMENT ON COLUMN core_chart_view.view_fields IS '图表字段集合';
COMMENT ON COLUMN core_chart_view.refresh_view_enable IS '是否开启刷新';
COMMENT ON COLUMN core_chart_view.refresh_unit IS '刷新时间单位';
COMMENT ON COLUMN core_chart_view.refresh_time IS '刷新时间';
COMMENT ON COLUMN core_chart_view.linkage_active IS '是否开启联动';
COMMENT ON COLUMN core_chart_view.jump_active IS '是否开启跳转';
COMMENT ON COLUMN core_chart_view.copy_from IS '复制来源';
COMMENT ON COLUMN core_chart_view.copy_id IS '复制ID';
COMMENT ON COLUMN core_chart_view.aggregate IS '区间条形图开启时间纬度开启聚合';
COMMENT ON COLUMN core_chart_view.flow_map_start_name IS '流向地图起点字段';
COMMENT ON COLUMN core_chart_view.flow_map_end_name IS '流向地图终点字段';
COMMENT ON COLUMN core_chart_view.ext_color IS '颜色维度字段';
COMMENT ON COLUMN core_chart_view.sort_priority IS '字段排序优先级';
COMMENT ON TABLE core_query_assistant_config IS '查询助手配置';
COMMENT ON COLUMN core_query_assistant_config.id IS 'ID';
COMMENT ON COLUMN core_query_assistant_config.copilot_url IS '查询助手服务地址';
COMMENT ON COLUMN core_query_assistant_config.username IS '用户名';
COMMENT ON COLUMN core_query_assistant_config.pwd IS '密码';
COMMENT ON TABLE core_query_assistant_message IS '查询助手问答记录';
COMMENT ON COLUMN core_query_assistant_message.id IS 'ID';
COMMENT ON COLUMN core_query_assistant_message.user_id IS '用户ID';
COMMENT ON COLUMN core_query_assistant_message.dataset_group_id IS '数据集ID';
COMMENT ON COLUMN core_query_assistant_message.msg_type IS '消息来源类型';
COMMENT ON COLUMN core_query_assistant_message.engine_type IS '数据库引擎类型';
COMMENT ON COLUMN core_query_assistant_message.schema_sql IS '建表 SQL';
COMMENT ON COLUMN core_query_assistant_message.question IS '用户提问';
COMMENT ON COLUMN core_query_assistant_message.history IS '历史信息';
COMMENT ON COLUMN core_query_assistant_message.copilot_sql IS '查询助手返回 SQL';
COMMENT ON COLUMN core_query_assistant_message.api_msg IS '接口返回信息';
COMMENT ON COLUMN core_query_assistant_message.sql_ok IS 'sql 状态';
COMMENT ON COLUMN core_query_assistant_message.chart_ok IS '图表生成状态';
COMMENT ON COLUMN core_query_assistant_message.chart IS '图表配置';
COMMENT ON COLUMN core_query_assistant_message.chart_data IS '视图数据';
COMMENT ON COLUMN core_query_assistant_message.exec_sql IS '执行请求的SQL';
COMMENT ON COLUMN core_query_assistant_message.msg_status IS '消息处理状态';
COMMENT ON COLUMN core_query_assistant_message.err_msg IS '错误信息';
COMMENT ON COLUMN core_query_assistant_message.create_time IS '创建时间';
COMMENT ON TABLE core_query_assistant_token IS '查询助手令牌记录';
COMMENT ON COLUMN core_query_assistant_token.id IS 'ID';
COMMENT ON COLUMN core_query_assistant_token.type IS '令牌类型';
COMMENT ON COLUMN core_query_assistant_token.token IS '令牌值';
COMMENT ON COLUMN core_query_assistant_token.update_time IS '更新时间';
COMMENT ON TABLE core_geo_area IS '自定义地理区域';
COMMENT ON COLUMN core_geo_area.id IS 'id';
COMMENT ON COLUMN core_geo_area.name IS '区域名称';
COMMENT ON TABLE core_geo_sub_area IS '自定义地理区域分区详情';
COMMENT ON COLUMN core_geo_sub_area.id IS 'id';
COMMENT ON COLUMN core_geo_sub_area.name IS '名称';
COMMENT ON COLUMN core_geo_sub_area.scope IS '区域范围';
COMMENT ON COLUMN core_geo_sub_area.geo_area_id IS '自定义地理区域id';
COMMENT ON TABLE core_dataset IS '数据集分组表';
COMMENT ON COLUMN core_dataset.id IS 'ID';
COMMENT ON COLUMN core_dataset.name IS '名称';
COMMENT ON COLUMN core_dataset.pid IS '父级ID';
COMMENT ON COLUMN core_dataset."LEVEL" IS '当前分组处于第几级';
COMMENT ON COLUMN core_dataset.node_type IS 'node类型：folder or dataset';
COMMENT ON COLUMN core_dataset.type IS 'sql,union';
COMMENT ON COLUMN core_dataset."MODE" IS '连接模式：0 直连，1 同步';
COMMENT ON COLUMN core_dataset.info IS '关联关系树';
COMMENT ON COLUMN core_dataset.create_by IS '创建人ID';
COMMENT ON COLUMN core_dataset.create_time IS '创建时间';
COMMENT ON COLUMN core_dataset.scheduler_fire_instance_id IS '调度触发实例编号';
COMMENT ON COLUMN core_dataset.sync_status IS '同步状态';
COMMENT ON COLUMN core_dataset.update_by IS '更新人ID';
COMMENT ON COLUMN core_dataset.last_update_time IS '最后同步时间';
COMMENT ON COLUMN core_dataset.union_sql IS '关联sql';
COMMENT ON COLUMN core_dataset.is_cross IS '是否跨源';
COMMENT ON TABLE core_dataset_sync_task IS '数据集同步任务表';
COMMENT ON COLUMN core_dataset_sync_task.id IS '主键';
COMMENT ON COLUMN core_dataset_sync_task.dataset_group_id IS '数据集ID';
COMMENT ON COLUMN core_dataset_sync_task.name IS '任务名称';
COMMENT ON COLUMN core_dataset_sync_task.update_type IS '更新方式：all_scope 全量，add_scope 增量';
COMMENT ON COLUMN core_dataset_sync_task.incremental_field_id IS '增量字段ID';
COMMENT ON COLUMN core_dataset_sync_task.incremental_last_value IS '增量字段上次同步值';
COMMENT ON COLUMN core_dataset_sync_task.start_time IS '开始时间';
COMMENT ON COLUMN core_dataset_sync_task.sync_rate IS '执行频率';
COMMENT ON COLUMN core_dataset_sync_task.cron IS 'cron表达式';
COMMENT ON COLUMN core_dataset_sync_task.simple_cron_value IS '简单重复间隔';
COMMENT ON COLUMN core_dataset_sync_task.simple_cron_type IS '简单重复类型：分、时、天';
COMMENT ON COLUMN core_dataset_sync_task.end_time IS '结束时间';
COMMENT ON COLUMN core_dataset_sync_task.create_time IS '创建时间';
COMMENT ON COLUMN core_dataset_sync_task.update_time IS '更新时间';
COMMENT ON COLUMN core_dataset_sync_task.last_exec_time IS '上次执行时间';
COMMENT ON COLUMN core_dataset_sync_task.last_exec_status IS '上次执行结果';
COMMENT ON COLUMN core_dataset_sync_task.task_status IS '任务状态';
COMMENT ON COLUMN core_dataset_sync_task.cache_ready IS '缓存表是否已可用';
COMMENT ON COLUMN core_dataset_sync_task.heartbeat_time IS '运行中心跳时间';
COMMENT ON COLUMN core_dataset_sync_task.schema_hash IS '数据集同步字段结构签名';
COMMENT ON COLUMN core_dataset_sync_task.full_sync_interval_hours IS '增量任务周期性全量校准间隔小时，0表示关闭';
COMMENT ON COLUMN core_dataset_sync_task.last_full_sync_time IS '最近一次全量同步时间';
COMMENT ON COLUMN core_dataset_sync_task.verify_enabled IS '同步后是否执行源端与缓存对账';
COMMENT ON COLUMN core_dataset_sync_task.last_verify_time IS '最近一次对账时间';
COMMENT ON COLUMN core_dataset_sync_task.last_verify_status IS '最近一次对账状态';
COMMENT ON COLUMN core_dataset_sync_task.last_verify_message IS '最近一次对账信息';
COMMENT ON COLUMN core_dataset_sync_task.last_source_row_count IS '最近一次源端行数';
COMMENT ON COLUMN core_dataset_sync_task.last_cache_row_count IS '最近一次缓存行数';
COMMENT ON COLUMN core_dataset_sync_task.cache_expire_hours IS '缓存过期提示阈值小时，0表示关闭';
COMMENT ON COLUMN core_dataset_sync_task.task_timeout_minutes IS '单次同步任务超时分钟，0表示关闭';
COMMENT ON COLUMN core_dataset_sync_task.consecutive_failures IS '连续失败次数';
COMMENT ON COLUMN core_dataset_sync_task.failure_warn_threshold IS '连续失败告警阈值，0表示关闭';
COMMENT ON TABLE core_dataset_sync_task_log IS '数据集同步任务日志表';
COMMENT ON COLUMN core_dataset_sync_task_log.id IS '主键';
COMMENT ON COLUMN core_dataset_sync_task_log.dataset_group_id IS '数据集ID';
COMMENT ON COLUMN core_dataset_sync_task_log.task_id IS '任务ID';
COMMENT ON COLUMN core_dataset_sync_task_log.update_type IS '更新方式';
COMMENT ON COLUMN core_dataset_sync_task_log.table_name IS '引擎表名';
COMMENT ON COLUMN core_dataset_sync_task_log.start_time IS '开始时间';
COMMENT ON COLUMN core_dataset_sync_task_log.end_time IS '结束时间';
COMMENT ON COLUMN core_dataset_sync_task_log.task_status IS '执行状态';
COMMENT ON COLUMN core_dataset_sync_task_log.row_count IS '同步行数';
COMMENT ON COLUMN core_dataset_sync_task_log.info IS '执行信息';
COMMENT ON COLUMN core_dataset_sync_task_log.create_time IS '创建时间';
COMMENT ON COLUMN core_dataset_sync_task_log.trigger_type IS '触发类型';
COMMENT ON TABLE core_asset_profile IS '数据资产治理扩展信息';
COMMENT ON COLUMN core_asset_profile.id IS 'ID';
COMMENT ON COLUMN core_asset_profile.asset_type IS '资产类型';
COMMENT ON COLUMN core_asset_profile.asset_id IS '资产ID';
COMMENT ON COLUMN core_asset_profile.description IS '资产说明';
COMMENT ON COLUMN core_asset_profile.owner_id IS '负责人ID';
COMMENT ON COLUMN core_asset_profile.certified IS '是否已认证';
COMMENT ON COLUMN core_asset_profile.recommended IS '是否推荐使用';
COMMENT ON COLUMN core_asset_profile.deprecated IS '是否已废弃';
COMMENT ON COLUMN core_asset_profile.tags IS '标签，逗号分隔';
COMMENT ON COLUMN core_asset_profile.create_time IS '创建时间';
COMMENT ON COLUMN core_asset_profile.update_time IS '更新时间';
COMMENT ON COLUMN core_asset_profile.update_by IS '更新人';
COMMENT ON TABLE core_dataset_table IS 'table数据集';
COMMENT ON COLUMN core_dataset_table.id IS 'ID';
COMMENT ON COLUMN core_dataset_table.name IS '名称';
COMMENT ON COLUMN core_dataset_table.table_name IS '物理表名';
COMMENT ON COLUMN core_dataset_table.datasource_id IS '数据源ID';
COMMENT ON COLUMN core_dataset_table.dataset_group_id IS '数据集ID';
COMMENT ON COLUMN core_dataset_table.type IS '数据表类型';
COMMENT ON COLUMN core_dataset_table.info IS '表原始信息,表名,sql等';
COMMENT ON COLUMN core_dataset_table.sql_variable_details IS 'SQL参数';
COMMENT ON TABLE core_dataset_field IS 'table数据集表字段';
COMMENT ON COLUMN core_dataset_field.id IS 'ID';
COMMENT ON COLUMN core_dataset_field.datasource_id IS '数据源ID';
COMMENT ON COLUMN core_dataset_field.dataset_table_id IS '数据表ID';
COMMENT ON COLUMN core_dataset_field.dataset_group_id IS '数据集ID';
COMMENT ON COLUMN core_dataset_field.chart_id IS '图表ID';
COMMENT ON COLUMN core_dataset_field.origin_name IS '原始字段名';
COMMENT ON COLUMN core_dataset_field.name IS '字段名用于展示';
COMMENT ON COLUMN core_dataset_field.description IS '描述';
COMMENT ON COLUMN core_dataset_field.engine_field_name IS '字段唯一标识';
COMMENT ON COLUMN core_dataset_field.field_short_name IS '字段别名';
COMMENT ON COLUMN core_dataset_field.group_list IS '分组设置';
COMMENT ON COLUMN core_dataset_field.other_group IS '未分组的值';
COMMENT ON COLUMN core_dataset_field.group_type IS '维度/指标标识 d:维度，q:指标';
COMMENT ON COLUMN core_dataset_field.type IS '原始字段类型';
COMMENT ON COLUMN core_dataset_field."SIZE" IS '字段长度（允许为空，默认0）';
COMMENT ON COLUMN core_dataset_field.field_type IS 'Crest字段类型：0-文本，1-时间，2-整型数值，3-浮点数值，4-布尔，5-地理位置，6-二进制，7-URL';
COMMENT ON COLUMN core_dataset_field.extracted_field_type IS '数据源原始类型';
COMMENT ON COLUMN core_dataset_field.ext_field IS '是否扩展字段 0原始 1复制 2计算字段...';
COMMENT ON COLUMN core_dataset_field.checked IS '是否选中';
COMMENT ON COLUMN core_dataset_field.column_index IS '列位置';
COMMENT ON COLUMN core_dataset_field.last_sync_time IS '同步时间';
COMMENT ON COLUMN core_dataset_field.accuracy IS '精度';
COMMENT ON COLUMN core_dataset_field.date_format IS '时间字段类型';
COMMENT ON COLUMN core_dataset_field.date_format_type IS '时间格式类型';
COMMENT ON COLUMN core_dataset_field.params IS '计算字段参数';
COMMENT ON COLUMN core_dataset_field.order_checked IS '是否排序';
COMMENT ON TABLE core_dataset_sql_log IS 'table数据集查询sql日志';
COMMENT ON COLUMN core_dataset_sql_log.id IS 'ID';
COMMENT ON COLUMN core_dataset_sql_log.table_id IS '数据集SQL节点ID';
COMMENT ON COLUMN core_dataset_sql_log.start_time IS '开始时间';
COMMENT ON COLUMN core_dataset_sql_log.end_time IS '结束时间';
COMMENT ON COLUMN core_dataset_sql_log.spend IS '耗时(毫秒)';
COMMENT ON COLUMN core_dataset_sql_log.sql IS '详细信息';
COMMENT ON COLUMN core_dataset_sql_log.status IS '状态';
COMMENT ON TABLE core_datasource IS '数据源表';
COMMENT ON COLUMN core_datasource.id IS '主键';
COMMENT ON COLUMN core_datasource.name IS '名称';
COMMENT ON COLUMN core_datasource.description IS '描述';
COMMENT ON COLUMN core_datasource.type IS '类型';
COMMENT ON COLUMN core_datasource.pid IS '父级ID';
COMMENT ON COLUMN core_datasource.edit_type IS '更新方式：0：替换；1：追加';
COMMENT ON COLUMN core_datasource.configuration IS '详细信息';
COMMENT ON COLUMN core_datasource.create_time IS '创建时间';
COMMENT ON COLUMN core_datasource.update_time IS '更新时间';
COMMENT ON COLUMN core_datasource.update_by IS '变更人';
COMMENT ON COLUMN core_datasource.create_by IS '创建人ID';
COMMENT ON COLUMN core_datasource.status IS '状态';
COMMENT ON COLUMN core_datasource.scheduler_fire_instance_id IS '调度触发实例编号';
COMMENT ON COLUMN core_datasource.task_status IS '任务状态';
COMMENT ON COLUMN core_datasource.enable_data_fill IS '启用数据填报功能';
COMMENT ON TABLE core_datasource_sync_task IS '数据源定时同步任务';
COMMENT ON COLUMN core_datasource_sync_task.id IS '主键';
COMMENT ON COLUMN core_datasource_sync_task.ds_id IS '数据源ID';
COMMENT ON COLUMN core_datasource_sync_task.name IS '任务名称';
COMMENT ON COLUMN core_datasource_sync_task.update_type IS '更新方式';
COMMENT ON COLUMN core_datasource_sync_task.start_time IS '开始时间';
COMMENT ON COLUMN core_datasource_sync_task.sync_rate IS '执行频率：0 一次性 1 cron';
COMMENT ON COLUMN core_datasource_sync_task.cron IS 'cron表达式';
COMMENT ON COLUMN core_datasource_sync_task.simple_cron_value IS '简单重复间隔';
COMMENT ON COLUMN core_datasource_sync_task.simple_cron_type IS '简单重复类型：分、时、天';
COMMENT ON COLUMN core_datasource_sync_task.end_limit IS '结束限制 0 无限制 1 设定结束时间';
COMMENT ON COLUMN core_datasource_sync_task.end_time IS '结束时间';
COMMENT ON COLUMN core_datasource_sync_task.create_time IS '创建时间';
COMMENT ON COLUMN core_datasource_sync_task.last_exec_time IS '上次执行时间';
COMMENT ON COLUMN core_datasource_sync_task.last_exec_status IS '上次执行结果';
COMMENT ON COLUMN core_datasource_sync_task.extra_data IS '额外数据';
COMMENT ON COLUMN core_datasource_sync_task.task_status IS '任务状态';
COMMENT ON TABLE core_datasource_sync_task_log IS '数据源定时同步任务执行日志';
COMMENT ON COLUMN core_datasource_sync_task_log.id IS '主键';
COMMENT ON COLUMN core_datasource_sync_task_log.ds_id IS '数据源ID';
COMMENT ON COLUMN core_datasource_sync_task_log.task_id IS '任务ID';
COMMENT ON COLUMN core_datasource_sync_task_log.start_time IS '开始时间';
COMMENT ON COLUMN core_datasource_sync_task_log.end_time IS '结束时间';
COMMENT ON COLUMN core_datasource_sync_task_log.task_status IS '执行状态';
COMMENT ON COLUMN core_datasource_sync_task_log.table_name IS '表名';
COMMENT ON COLUMN core_datasource_sync_task_log.info IS '错误信息';
COMMENT ON COLUMN core_datasource_sync_task_log.create_time IS '创建时间';
COMMENT ON COLUMN core_datasource_sync_task_log.trigger_type IS '更新频率类型';
COMMENT ON TABLE core_datasource_engine IS '数据引擎';
COMMENT ON COLUMN core_datasource_engine.id IS '主键';
COMMENT ON COLUMN core_datasource_engine.name IS '名称';
COMMENT ON COLUMN core_datasource_engine.description IS '描述';
COMMENT ON COLUMN core_datasource_engine.type IS '类型';
COMMENT ON COLUMN core_datasource_engine.configuration IS '详细信息';
COMMENT ON COLUMN core_datasource_engine.create_time IS '创建时间';
COMMENT ON COLUMN core_datasource_engine.update_time IS '更新时间';
COMMENT ON COLUMN core_datasource_engine.create_by IS '创建人ID';
COMMENT ON COLUMN core_datasource_engine.status IS '状态';
COMMENT ON COLUMN core_datasource_engine.enable_data_fill IS '启用数据填报功能';
COMMENT ON TABLE core_datasource_driver IS '驱动';
COMMENT ON COLUMN core_datasource_driver.id IS '主键';
COMMENT ON COLUMN core_datasource_driver.name IS '名称';
COMMENT ON COLUMN core_datasource_driver.create_time IS '创建时间';
COMMENT ON COLUMN core_datasource_driver.type IS '数据源类型';
COMMENT ON COLUMN core_datasource_driver.driver_class IS '驱动类';
COMMENT ON COLUMN core_datasource_driver.description IS '描述';
COMMENT ON TABLE core_datasource_driver_jar IS '驱动详情';
COMMENT ON COLUMN core_datasource_driver_jar.id IS '主键';
COMMENT ON COLUMN core_datasource_driver_jar.driver_id IS '驱动主键';
COMMENT ON COLUMN core_datasource_driver_jar.file_name IS '名称';
COMMENT ON COLUMN core_datasource_driver_jar.version IS '版本';
COMMENT ON COLUMN core_datasource_driver_jar.driver_class IS '驱动类';
COMMENT ON COLUMN core_datasource_driver_jar.trans_name IS '替换后的 jar 包名称';
COMMENT ON COLUMN core_datasource_driver_jar.is_trans_name IS '是否将上传 jar 包替换了名称（1-是，0-否）';
COMMENT ON TABLE core_datasource_finish_page IS '是否显示完成页面记录表';
COMMENT ON COLUMN core_datasource_finish_page.id IS '主键';
COMMENT ON TABLE core_export_download_task IS '下载任务列表';
COMMENT ON COLUMN core_export_download_task.id IS '下载任务 ID';
COMMENT ON COLUMN core_export_download_task.create_time IS '创建时间';
COMMENT ON COLUMN core_export_download_task.valid_time IS '有效截止时间';
COMMENT ON TABLE core_export_task IS '导出任务表';
COMMENT ON COLUMN core_export_task.id IS '主键';
COMMENT ON COLUMN core_export_task.user_id IS '用户ID';
COMMENT ON COLUMN core_export_task.file_name IS '文件名称';
COMMENT ON COLUMN core_export_task.file_size IS '文件大小';
COMMENT ON COLUMN core_export_task.file_size_unit IS '单位';
COMMENT ON COLUMN core_export_task.export_from IS '导出来源ID';
COMMENT ON COLUMN core_export_task.export_status IS '导出状态';
COMMENT ON COLUMN core_export_task.export_from_type IS '导出来源类型';
COMMENT ON COLUMN core_export_task.export_time IS '导出时间';
COMMENT ON COLUMN core_export_task.export_progress IS '导出进度';
COMMENT ON COLUMN core_export_task.export_machine_name IS '导出机器名称';
COMMENT ON COLUMN core_export_task.params IS '过滤参数';
COMMENT ON COLUMN core_export_task.msg IS '错误信息';
COMMENT ON TABLE core_font_asset IS '字体表';
COMMENT ON COLUMN core_font_asset.id IS 'ID';
COMMENT ON COLUMN core_font_asset.name IS '字体名称';
COMMENT ON COLUMN core_font_asset.file_name IS '文件名称';
COMMENT ON COLUMN core_font_asset.file_trans_name IS '文件转换名称';
COMMENT ON COLUMN core_font_asset.is_default IS '是否默认';
COMMENT ON COLUMN core_font_asset.update_time IS '更新时间';
COMMENT ON COLUMN core_font_asset.is_BuiltIn IS '是否内置';
COMMENT ON COLUMN core_font_asset."SIZE" IS '文件大小';
COMMENT ON COLUMN core_font_asset.size_type IS '存储单位';
COMMENT ON TABLE core_iam_menu IS '路由菜单';
COMMENT ON COLUMN core_iam_menu.id IS '主键';
COMMENT ON COLUMN core_iam_menu.pid IS '父ID';
COMMENT ON COLUMN core_iam_menu.type IS '类型';
COMMENT ON COLUMN core_iam_menu.name IS '名称';
COMMENT ON COLUMN core_iam_menu.component IS '组件';
COMMENT ON COLUMN core_iam_menu.menu_sort IS '排序';
COMMENT ON COLUMN core_iam_menu.icon IS '图标';
COMMENT ON COLUMN core_iam_menu.path IS '路径';
COMMENT ON COLUMN core_iam_menu.hidden IS '隐藏';
COMMENT ON COLUMN core_iam_menu.in_layout IS '是否内部';
COMMENT ON COLUMN core_iam_menu.auth IS '参与授权';
COMMENT ON TABLE core_workspace_recent_resource IS '可视化资源表';
COMMENT ON COLUMN core_workspace_recent_resource.id IS 'ID';
COMMENT ON COLUMN core_workspace_recent_resource.resource_id IS '资源ID';
COMMENT ON COLUMN core_workspace_recent_resource.resource_name IS '资源名称';
COMMENT ON COLUMN core_workspace_recent_resource."UID" IS '用户ID';
COMMENT ON COLUMN core_workspace_recent_resource.resource_type IS '资源类型 1-可视化资源 2-仪表板 3-数据大屏 4-数据集 5-数据源 6-模板';
COMMENT ON COLUMN core_workspace_recent_resource.opt_type IS '1 新建 2 修改';
COMMENT ON COLUMN core_workspace_recent_resource.time IS '收藏时间';
COMMENT ON TABLE core_auth_token IS '认证令牌信息';
COMMENT ON COLUMN core_auth_token.id IS '主键';
COMMENT ON COLUMN core_auth_token.token IS '认证令牌';
COMMENT ON COLUMN core_auth_token.create_time IS '创建时间';
COMMENT ON COLUMN core_auth_token.exp_time IS '过期时间';
COMMENT ON TABLE core_plugin_registry IS '插件表';
COMMENT ON COLUMN core_plugin_registry.id IS 'ID';
COMMENT ON COLUMN core_plugin_registry.name IS '插件名称';
COMMENT ON COLUMN core_plugin_registry.icon IS '图标';
COMMENT ON COLUMN core_plugin_registry.version IS '版本';
COMMENT ON COLUMN core_plugin_registry.install_time IS '安装时间';
COMMENT ON COLUMN core_plugin_registry.flag IS '类型';
COMMENT ON COLUMN core_plugin_registry.developer IS '开发者';
COMMENT ON COLUMN core_plugin_registry.config IS '插件配置';
COMMENT ON COLUMN core_plugin_registry.require_version IS 'Crest 最低版本';
COMMENT ON COLUMN core_plugin_registry.module_name IS '模块名称';
COMMENT ON COLUMN core_plugin_registry.jar_name IS 'Jar包名称';
COMMENT ON TABLE core_crypto_key IS 'rsa 密钥表';
COMMENT ON COLUMN core_crypto_key.id IS '主键';
COMMENT ON COLUMN core_crypto_key.private_key IS '私钥';
COMMENT ON COLUMN core_crypto_key.public_key IS '公钥';
COMMENT ON COLUMN core_crypto_key.create_time IS '生成时间';
COMMENT ON COLUMN core_crypto_key.aes_key IS 'AES 加密算法的 key';
COMMENT ON COLUMN core_crypto_key.sm2_private_key IS 'SM2 私钥';
COMMENT ON COLUMN core_crypto_key.sm2_public_key IS 'SM2 公钥';
COMMENT ON COLUMN core_crypto_key.sm2_create_time IS 'SM2 密钥生成时间';
COMMENT ON TABLE core_auth_setting IS '认证设置';
COMMENT ON COLUMN core_auth_setting.id IS '主键';
COMMENT ON COLUMN core_auth_setting.name IS '名称';
COMMENT ON COLUMN core_auth_setting.type IS '类型';
COMMENT ON COLUMN core_auth_setting.enable IS '是否启用';
COMMENT ON COLUMN core_auth_setting.sync_time IS '同步时间';
COMMENT ON COLUMN core_auth_setting.relational_ids IS '相关的ID';
COMMENT ON COLUMN core_auth_setting.plugin_json IS '插件配置';
COMMENT ON COLUMN core_auth_setting.synced IS '已同步';
COMMENT ON COLUMN core_auth_setting.valid IS '有效';
COMMENT ON TABLE core_share_link IS '公共链接';
COMMENT ON COLUMN core_share_link.id IS 'ID';
COMMENT ON COLUMN core_share_link.creator IS '创建人';
COMMENT ON COLUMN core_share_link.time IS '创建时间';
COMMENT ON COLUMN core_share_link.exp IS '过期时间';
COMMENT ON COLUMN core_share_link.uuid IS 'UUID';
COMMENT ON COLUMN core_share_link.pwd IS '密码';
COMMENT ON COLUMN core_share_link.resource_id IS '资源ID';
COMMENT ON COLUMN core_share_link.oid IS '组织ID';
COMMENT ON COLUMN core_share_link.type IS '业务类型';
COMMENT ON COLUMN core_share_link.auto_pwd IS '自动生成密码';
COMMENT ON COLUMN core_share_link.ticket_require IS 'ticket必须';
COMMENT ON TABLE core_share_ticket IS '分享Ticket表';
COMMENT ON COLUMN core_share_ticket.id IS 'ID';
COMMENT ON COLUMN core_share_ticket.uuid IS '分享 UUID';
COMMENT ON COLUMN core_share_ticket.ticket IS '分享票据';
COMMENT ON COLUMN core_share_ticket.exp IS 'ticket有效期';
COMMENT ON COLUMN core_share_ticket.args IS 'ticket参数';
COMMENT ON COLUMN core_share_ticket.access_time IS '首次访问时间';
COMMENT ON TABLE core_workspace_favorite_resource IS '用户收藏表';
COMMENT ON COLUMN core_workspace_favorite_resource.id IS 'ID';
COMMENT ON COLUMN core_workspace_favorite_resource.resource_id IS '资源ID';
COMMENT ON COLUMN core_workspace_favorite_resource."UID" IS '用户ID';
COMMENT ON COLUMN core_workspace_favorite_resource.resource_type IS '资源类型';
COMMENT ON COLUMN core_workspace_favorite_resource.time IS '收藏时间';
COMMENT ON TABLE core_system_setting IS '系统设置表';
COMMENT ON COLUMN core_system_setting.id IS 'ID';
COMMENT ON COLUMN core_system_setting.pkey IS '键';
COMMENT ON COLUMN core_system_setting.pval IS '值';
COMMENT ON COLUMN core_system_setting.type IS '类型';
COMMENT ON COLUMN core_system_setting.sort IS '顺序';
COMMENT ON TABLE core_system_startup_job IS '项目启动任务';
COMMENT ON COLUMN core_system_startup_job.id IS 'ID';
COMMENT ON COLUMN core_system_startup_job.name IS '任务名称';
COMMENT ON COLUMN core_system_startup_job.status IS '任务状态';
COMMENT ON TABLE core_alert_rule IS '告警信息表';
COMMENT ON COLUMN core_alert_rule.id IS '主键';
COMMENT ON COLUMN core_alert_rule.name IS '告警名称';
COMMENT ON COLUMN core_alert_rule.enable IS '是否启用';
COMMENT ON COLUMN core_alert_rule.rate_type IS '频率类型';
COMMENT ON COLUMN core_alert_rule.rate_value IS '频率值';
COMMENT ON COLUMN core_alert_rule.resource_type IS '资源类型';
COMMENT ON COLUMN core_alert_rule.resource_id IS '资源ID';
COMMENT ON COLUMN core_alert_rule.chart_type IS '图表类型';
COMMENT ON COLUMN core_alert_rule.chart_id IS '图表ID';
COMMENT ON COLUMN core_alert_rule.threshold_rules IS '告警规则';
COMMENT ON COLUMN core_alert_rule.recisetting IS '消息渠道';
COMMENT ON COLUMN core_alert_rule.reci_users IS '接收人';
COMMENT ON COLUMN core_alert_rule.reci_roles IS '接收角色';
COMMENT ON COLUMN core_alert_rule.reci_emails IS '接收邮箱';
COMMENT ON COLUMN core_alert_rule.reci_lark_groups IS '飞书群聊';
COMMENT ON COLUMN core_alert_rule.reci_larksuite_groups IS '国际飞书群';
COMMENT ON COLUMN core_alert_rule.reci_webhooks IS 'Webhook 接收人';
COMMENT ON COLUMN core_alert_rule.msg_title IS '消息标题';
COMMENT ON COLUMN core_alert_rule.msg_type IS '消息类型';
COMMENT ON COLUMN core_alert_rule.msg_content IS '消息内容';
COMMENT ON COLUMN core_alert_rule.repeat_send IS '是否重复发送';
COMMENT ON COLUMN core_alert_rule.show_field_value IS '显示字段值';
COMMENT ON COLUMN core_alert_rule.status IS '数据状态';
COMMENT ON COLUMN core_alert_rule.creator IS '创建者ID';
COMMENT ON COLUMN core_alert_rule.creator_name IS '创建人名称';
COMMENT ON COLUMN core_alert_rule.create_time IS '创建时间';
COMMENT ON COLUMN core_alert_rule.oid IS '所属组织';
COMMENT ON TABLE core_alert_instance IS '告警实例表';
COMMENT ON COLUMN core_alert_instance.id IS '主键';
COMMENT ON COLUMN core_alert_instance.task_id IS '阈值信息ID';
COMMENT ON COLUMN core_alert_instance.exec_time IS '检测时间';
COMMENT ON COLUMN core_alert_instance.status IS '数据状态';
COMMENT ON COLUMN core_alert_instance.content IS '通知内容';
COMMENT ON COLUMN core_alert_instance.msg IS '报错信息';
COMMENT ON TABLE core_webhook_config IS 'Webhook 配置';
COMMENT ON COLUMN core_webhook_config.id IS 'ID';
COMMENT ON COLUMN core_webhook_config.name IS '名称';
COMMENT ON COLUMN core_webhook_config.url IS '地址';
COMMENT ON COLUMN core_webhook_config.content_type IS '内容类型';
COMMENT ON COLUMN core_webhook_config.secret IS '密钥';
COMMENT ON COLUMN core_webhook_config.ssl IS '开启ssl';
COMMENT ON COLUMN core_webhook_config.oid IS '组织ID';
COMMENT ON COLUMN core_webhook_config.create_time IS '创建时间';
COMMENT ON TABLE core_iam_user IS 'Crest用户表';
COMMENT ON COLUMN core_iam_user.id IS '用户ID';
COMMENT ON COLUMN core_iam_user.account IS '账号';
COMMENT ON COLUMN core_iam_user.name IS '姓名';
COMMENT ON COLUMN core_iam_user.email IS '邮箱';
COMMENT ON COLUMN core_iam_user.phone_prefix IS '电话区号';
COMMENT ON COLUMN core_iam_user.phone IS '电话';
COMMENT ON COLUMN core_iam_user.password_hash IS '密码摘要';
COMMENT ON COLUMN core_iam_user.enable IS '是否启用';
COMMENT ON COLUMN core_iam_user.is_admin IS '是否管理员';
COMMENT ON COLUMN core_iam_user.origin IS '用户来源';
COMMENT ON COLUMN core_iam_user.auth_type IS '认证类型';
COMMENT ON COLUMN core_iam_user.external_id IS '外部身份 ID';
COMMENT ON COLUMN core_iam_user.last_login_time IS '最近登录时间';
COMMENT ON COLUMN core_iam_user.create_time IS '创建时间';
COMMENT ON COLUMN core_iam_user.update_time IS '更新时间';
COMMENT ON TABLE core_visualization IS '可视化大屏信息表';
COMMENT ON COLUMN core_visualization.id IS '主键';
COMMENT ON COLUMN core_visualization.name IS '名称';
COMMENT ON COLUMN core_visualization.pid IS '父id';
COMMENT ON COLUMN core_visualization.org_id IS '所属组织id';
COMMENT ON COLUMN core_visualization."LEVEL" IS '层级';
COMMENT ON COLUMN core_visualization.node_type IS '节点类型  folder or panel 目录或者文件夹';
COMMENT ON COLUMN core_visualization.type IS '类型';
COMMENT ON COLUMN core_visualization.canvas_style_data IS '样式数据';
COMMENT ON COLUMN core_visualization.component_data IS '组件数据';
COMMENT ON COLUMN core_visualization.mobile_layout IS '移动端布局0-关闭 1-开启';
COMMENT ON COLUMN core_visualization.status IS '状态 0-未发布 1-已发布';
COMMENT ON COLUMN core_visualization.self_watermark_status IS '是否单独打开水印 0-关闭 1-开启';
COMMENT ON COLUMN core_visualization.sort IS '排序';
COMMENT ON COLUMN core_visualization.create_time IS '创建时间';
COMMENT ON COLUMN core_visualization.create_by IS '创建人';
COMMENT ON COLUMN core_visualization.update_time IS '更新时间';
COMMENT ON COLUMN core_visualization.update_by IS '更新人';
COMMENT ON COLUMN core_visualization.remark IS '备注';
COMMENT ON COLUMN core_visualization.source IS '数据来源';
COMMENT ON COLUMN core_visualization.delete_flag IS '删除标志';
COMMENT ON COLUMN core_visualization.delete_time IS '删除时间';
COMMENT ON COLUMN core_visualization.delete_by IS '删除人';
COMMENT ON COLUMN core_visualization.version IS '可视化资源版本';
COMMENT ON COLUMN core_visualization.content_id IS '内容标识';
COMMENT ON COLUMN core_visualization.check_version IS '内容检查标识';
COMMENT ON TABLE core_template_init_history IS '模板配置版本记录表';
COMMENT ON COLUMN core_template_init_history.installed_rank IS '主键';
COMMENT ON COLUMN core_template_init_history.version IS '版本';
COMMENT ON COLUMN core_template_init_history.description IS '描述';
COMMENT ON COLUMN core_template_init_history.type IS '类型';
COMMENT ON COLUMN core_template_init_history.script IS '脚本';
COMMENT ON COLUMN core_template_init_history.checksum IS 'CheckSum校验码';
COMMENT ON COLUMN core_template_init_history.installed_by IS '安装人';
COMMENT ON COLUMN core_template_init_history.installed_on IS '安装时间';
COMMENT ON COLUMN core_template_init_history.execution_time IS '执行时间';
COMMENT ON COLUMN core_template_init_history.success IS '执行状态';
COMMENT ON TABLE core_chart_view_snapshot IS '图表快照';
COMMENT ON COLUMN core_chart_view_snapshot.id IS 'ID';
COMMENT ON COLUMN core_chart_view_snapshot.title IS '标题';
COMMENT ON COLUMN core_chart_view_snapshot.scene_id IS '所属可视化资源 ID';
COMMENT ON COLUMN core_chart_view_snapshot.table_id IS '数据集表ID';
COMMENT ON COLUMN core_chart_view_snapshot.type IS '图表类型';
COMMENT ON COLUMN core_chart_view_snapshot.render IS '图表渲染方式';
COMMENT ON COLUMN core_chart_view_snapshot.result_count IS '展示结果';
COMMENT ON COLUMN core_chart_view_snapshot.result_mode IS '展示模式';
COMMENT ON COLUMN core_chart_view_snapshot.x_axis IS '横轴字段配置';
COMMENT ON COLUMN core_chart_view_snapshot.x_axis_ext IS '表格行扩展配置';
COMMENT ON COLUMN core_chart_view_snapshot.y_axis IS '纵轴字段配置';
COMMENT ON COLUMN core_chart_view_snapshot.y_axis_ext IS '副轴';
COMMENT ON COLUMN core_chart_view_snapshot.ext_stack IS '堆叠项';
COMMENT ON COLUMN core_chart_view_snapshot.ext_bubble IS '气泡大小';
COMMENT ON COLUMN core_chart_view_snapshot.ext_label IS '动态标签';
COMMENT ON COLUMN core_chart_view_snapshot.ext_tooltip IS '动态提示';
COMMENT ON COLUMN core_chart_view_snapshot.custom_attr IS '图形属性';
COMMENT ON COLUMN core_chart_view_snapshot.custom_attr_mobile IS '图形属性_移动端';
COMMENT ON COLUMN core_chart_view_snapshot.custom_style IS '组件样式';
COMMENT ON COLUMN core_chart_view_snapshot.custom_style_mobile IS '组件样式_移动端';
COMMENT ON COLUMN core_chart_view_snapshot.custom_filter IS '结果过滤';
COMMENT ON COLUMN core_chart_view_snapshot.drill_fields IS '钻取字段';
COMMENT ON COLUMN core_chart_view_snapshot.senior IS '高级';
COMMENT ON COLUMN core_chart_view_snapshot.create_by IS '创建人ID';
COMMENT ON COLUMN core_chart_view_snapshot.create_time IS '创建时间';
COMMENT ON COLUMN core_chart_view_snapshot.update_time IS '更新时间';
COMMENT ON COLUMN core_chart_view_snapshot.snapshot IS '缩略图 ';
COMMENT ON COLUMN core_chart_view_snapshot.style_priority IS '样式优先级 panel 仪表板 view 图表';
COMMENT ON COLUMN core_chart_view_snapshot.chart_type IS '图表类型 public 公共 历史可复用的图表，private 私有 专属某个仪表板';
COMMENT ON COLUMN core_chart_view_snapshot.is_plugin IS '是否插件';
COMMENT ON COLUMN core_chart_view_snapshot.data_from IS '数据来源 template 模板数据 dataset 数据集数据';
COMMENT ON COLUMN core_chart_view_snapshot.view_fields IS '图表字段集合';
COMMENT ON COLUMN core_chart_view_snapshot.refresh_view_enable IS '是否开启刷新';
COMMENT ON COLUMN core_chart_view_snapshot.refresh_unit IS '刷新时间单位';
COMMENT ON COLUMN core_chart_view_snapshot.refresh_time IS '刷新时间';
COMMENT ON COLUMN core_chart_view_snapshot.linkage_active IS '是否开启联动';
COMMENT ON COLUMN core_chart_view_snapshot.jump_active IS '是否开启跳转';
COMMENT ON COLUMN core_chart_view_snapshot.copy_from IS '复制来源';
COMMENT ON COLUMN core_chart_view_snapshot.copy_id IS '复制ID';
COMMENT ON COLUMN core_chart_view_snapshot.aggregate IS '区间条形图开启时间纬度开启聚合';
COMMENT ON COLUMN core_chart_view_snapshot.flow_map_start_name IS '流向地图起点字段';
COMMENT ON COLUMN core_chart_view_snapshot.flow_map_end_name IS '流向地图终点字段';
COMMENT ON COLUMN core_chart_view_snapshot.ext_color IS '颜色维度字段';
COMMENT ON COLUMN core_chart_view_snapshot.sort_priority IS '字段排序优先级';
COMMENT ON TABLE core_visualization_snapshot IS '可视化资源快照';
COMMENT ON COLUMN core_visualization_snapshot.id IS '主键';
COMMENT ON COLUMN core_visualization_snapshot.name IS '名称';
COMMENT ON COLUMN core_visualization_snapshot.pid IS '父id';
COMMENT ON COLUMN core_visualization_snapshot.org_id IS '所属组织id';
COMMENT ON COLUMN core_visualization_snapshot."LEVEL" IS '层级';
COMMENT ON COLUMN core_visualization_snapshot.node_type IS '节点类型  folder or panel 目录或者文件夹';
COMMENT ON COLUMN core_visualization_snapshot.type IS '类型';
COMMENT ON COLUMN core_visualization_snapshot.canvas_style_data IS '样式数据';
COMMENT ON COLUMN core_visualization_snapshot.component_data IS '组件数据';
COMMENT ON COLUMN core_visualization_snapshot.mobile_layout IS '移动端布局0-关闭 1-开启';
COMMENT ON COLUMN core_visualization_snapshot.status IS '状态 0-未发布 1-已发布';
COMMENT ON COLUMN core_visualization_snapshot.self_watermark_status IS '是否单独打开水印 0-关闭 1-开启';
COMMENT ON COLUMN core_visualization_snapshot.sort IS '排序';
COMMENT ON COLUMN core_visualization_snapshot.create_time IS '创建时间';
COMMENT ON COLUMN core_visualization_snapshot.create_by IS '创建人';
COMMENT ON COLUMN core_visualization_snapshot.update_time IS '更新时间';
COMMENT ON COLUMN core_visualization_snapshot.update_by IS '更新人';
COMMENT ON COLUMN core_visualization_snapshot.remark IS '备注';
COMMENT ON COLUMN core_visualization_snapshot.source IS '数据来源';
COMMENT ON COLUMN core_visualization_snapshot.delete_flag IS '删除标志';
COMMENT ON COLUMN core_visualization_snapshot.delete_time IS '删除时间';
COMMENT ON COLUMN core_visualization_snapshot.delete_by IS '删除人';
COMMENT ON COLUMN core_visualization_snapshot.version IS '可视化资源版本';
COMMENT ON COLUMN core_visualization_snapshot.content_id IS '内容标识';
COMMENT ON COLUMN core_visualization_snapshot.check_version IS '内容检查标识';
COMMENT ON TABLE core_visualization_jump_snapshot IS '跳转记录快照';
COMMENT ON COLUMN core_visualization_jump_snapshot.id IS '主键';
COMMENT ON COLUMN core_visualization_jump_snapshot.source_dv_id IS '源仪表板ID';
COMMENT ON COLUMN core_visualization_jump_snapshot.source_view_id IS '源图表ID';
COMMENT ON COLUMN core_visualization_jump_snapshot.link_jump_info IS '跳转信息';
COMMENT ON COLUMN core_visualization_jump_snapshot.checked IS '是否启用';
COMMENT ON COLUMN core_visualization_jump_snapshot.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_jump_snapshot.copy_id IS '复制来源ID';
COMMENT ON TABLE core_visualization_jump_action_snapshot IS '跳转配置快照';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.id IS '主键';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.link_jump_id IS '跳转配置ID';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.link_type IS '关联类型 inner 内部仪表板，outer 外部链接';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.jump_type IS '跳转类型 _blank 新开页面 _self 当前窗口';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.target_dv_id IS '关联仪表板ID';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.source_field_id IS '字段ID';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.content IS '内容 linkType = outer时使用';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.checked IS '是否可用';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.attach_params IS '是否附加点击参数';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.copy_id IS '复制来源ID';
COMMENT ON COLUMN core_visualization_jump_action_snapshot.window_size IS '窗口大小large middle small';
COMMENT ON TABLE core_visualization_jump_target_snapshot IS '跳转目标配置快照';
COMMENT ON COLUMN core_visualization_jump_target_snapshot.target_id IS '主键';
COMMENT ON COLUMN core_visualization_jump_target_snapshot.link_jump_info_id IS 'core_visualization_jump_action 表的 ID';
COMMENT ON COLUMN core_visualization_jump_target_snapshot.source_field_active_id IS '勾选字段设置的匹配字段，也可以不是勾选字段本身';
COMMENT ON COLUMN core_visualization_jump_target_snapshot.target_view_id IS '目标图表ID';
COMMENT ON COLUMN core_visualization_jump_target_snapshot.target_field_id IS '目标字段ID';
COMMENT ON COLUMN core_visualization_jump_target_snapshot.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_jump_target_snapshot.copy_id IS '复制来源ID';
COMMENT ON COLUMN core_visualization_jump_target_snapshot.target_type IS '联动目标类型 view 图表 filter 过滤组件 outParams 外部参数';
COMMENT ON TABLE core_visualization_linkage_snapshot IS '联动记录快照';
COMMENT ON COLUMN core_visualization_linkage_snapshot.id IS '主键';
COMMENT ON COLUMN core_visualization_linkage_snapshot.dv_id IS '联动大屏/仪表板ID';
COMMENT ON COLUMN core_visualization_linkage_snapshot.source_view_id IS '源图表id';
COMMENT ON COLUMN core_visualization_linkage_snapshot.target_view_id IS '联动图表id';
COMMENT ON COLUMN core_visualization_linkage_snapshot.update_time IS '更新时间';
COMMENT ON COLUMN core_visualization_linkage_snapshot.update_people IS '更新人';
COMMENT ON COLUMN core_visualization_linkage_snapshot.linkage_active IS '是否启用关联';
COMMENT ON COLUMN core_visualization_linkage_snapshot.ext1 IS '扩展字段1';
COMMENT ON COLUMN core_visualization_linkage_snapshot.ext2 IS '扩展字段2';
COMMENT ON COLUMN core_visualization_linkage_snapshot.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_linkage_snapshot.copy_id IS '复制来源ID';
COMMENT ON TABLE core_visualization_linkage_field_snapshot IS '联动字段快照';
COMMENT ON COLUMN core_visualization_linkage_field_snapshot.id IS '主键';
COMMENT ON COLUMN core_visualization_linkage_field_snapshot.linkage_id IS '联动ID';
COMMENT ON COLUMN core_visualization_linkage_field_snapshot.source_field IS '源图表字段';
COMMENT ON COLUMN core_visualization_linkage_field_snapshot.target_field IS '目标图表字段';
COMMENT ON COLUMN core_visualization_linkage_field_snapshot.update_time IS '更新时间';
COMMENT ON COLUMN core_visualization_linkage_field_snapshot.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_linkage_field_snapshot.copy_id IS '复制来源ID';
COMMENT ON TABLE core_visualization_parameter_snapshot IS '外部参数关联快照';
COMMENT ON COLUMN core_visualization_parameter_snapshot.params_id IS '主键';
COMMENT ON COLUMN core_visualization_parameter_snapshot.visualization_id IS '可视化资源ID';
COMMENT ON COLUMN core_visualization_parameter_snapshot.checked IS '是否启用外部参数标识（1-是，0-否）';
COMMENT ON COLUMN core_visualization_parameter_snapshot.remark IS '备注';
COMMENT ON COLUMN core_visualization_parameter_snapshot.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_parameter_snapshot.copy_id IS '复制来源ID';
COMMENT ON TABLE core_visualization_parameter_item_snapshot IS '外部参数配置快照';
COMMENT ON COLUMN core_visualization_parameter_item_snapshot.params_info_id IS '主键';
COMMENT ON COLUMN core_visualization_parameter_item_snapshot.params_id IS 'core_visualization_parameter 表的 ID';
COMMENT ON COLUMN core_visualization_parameter_item_snapshot.param_name IS '参数名';
COMMENT ON COLUMN core_visualization_parameter_item_snapshot.checked IS '是否启用';
COMMENT ON COLUMN core_visualization_parameter_item_snapshot.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_parameter_item_snapshot.copy_id IS '复制来源ID';
COMMENT ON COLUMN core_visualization_parameter_item_snapshot.required IS '是否必填';
COMMENT ON COLUMN core_visualization_parameter_item_snapshot.default_value IS '默认值 JSON格式';
COMMENT ON COLUMN core_visualization_parameter_item_snapshot.enabled_default IS '是否启用默认值';
COMMENT ON TABLE core_visualization_parameter_target_snapshot IS '外部参数目标字段快照';
COMMENT ON COLUMN core_visualization_parameter_target_snapshot.target_id IS '主键';
COMMENT ON COLUMN core_visualization_parameter_target_snapshot.params_info_id IS 'core_visualization_parameter_item 表的 ID';
COMMENT ON COLUMN core_visualization_parameter_target_snapshot.target_view_id IS '联动视图ID/联动过滤项ID';
COMMENT ON COLUMN core_visualization_parameter_target_snapshot.target_field_id IS '联动字段ID';
COMMENT ON COLUMN core_visualization_parameter_target_snapshot.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_parameter_target_snapshot.copy_id IS '复制来源ID';
COMMENT ON COLUMN core_visualization_parameter_target_snapshot.target_ds_id IS '联动数据集id/联动过滤组件id';
COMMENT ON COLUMN core_visualization_parameter_target_snapshot.match_mode IS '匹配方式';
COMMENT ON TABLE core_visualization_background IS '边框背景表';
COMMENT ON COLUMN core_visualization_background.id IS '主键';
COMMENT ON COLUMN core_visualization_background.name IS '名称';
COMMENT ON COLUMN core_visualization_background.classification IS '分类名';
COMMENT ON COLUMN core_visualization_background.content IS '内容';
COMMENT ON COLUMN core_visualization_background.remark IS '备注';
COMMENT ON COLUMN core_visualization_background.sort IS '排序';
COMMENT ON COLUMN core_visualization_background.upload_time IS '上传时间';
COMMENT ON COLUMN core_visualization_background.base_url IS '所在目录地址';
COMMENT ON COLUMN core_visualization_background.url IS '图片地址';
COMMENT ON TABLE core_visualization_background_image IS '背景图';
COMMENT ON COLUMN core_visualization_background_image.id IS '主键';
COMMENT ON COLUMN core_visualization_background_image.name IS '名称';
COMMENT ON COLUMN core_visualization_background_image.classification IS '分类名';
COMMENT ON COLUMN core_visualization_background_image.content IS '内容';
COMMENT ON COLUMN core_visualization_background_image.remark IS '备注';
COMMENT ON COLUMN core_visualization_background_image.sort IS '排序';
COMMENT ON COLUMN core_visualization_background_image.upload_time IS '上传时间';
COMMENT ON COLUMN core_visualization_background_image.base_url IS '所在目录地址';
COMMENT ON COLUMN core_visualization_background_image.url IS '图片地址';
COMMENT ON TABLE core_visualization_jump IS '跳转记录表';
COMMENT ON COLUMN core_visualization_jump.id IS '主键';
COMMENT ON COLUMN core_visualization_jump.source_dv_id IS '源仪表板ID';
COMMENT ON COLUMN core_visualization_jump.source_view_id IS '源图表ID';
COMMENT ON COLUMN core_visualization_jump.link_jump_info IS '跳转信息';
COMMENT ON COLUMN core_visualization_jump.checked IS '是否启用';
COMMENT ON COLUMN core_visualization_jump.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_jump.copy_id IS '复制来源ID';
COMMENT ON TABLE core_visualization_jump_action IS '跳转配置表';
COMMENT ON COLUMN core_visualization_jump_action.id IS '主键';
COMMENT ON COLUMN core_visualization_jump_action.link_jump_id IS '跳转配置ID';
COMMENT ON COLUMN core_visualization_jump_action.link_type IS '关联类型 inner 内部仪表板，outer 外部链接';
COMMENT ON COLUMN core_visualization_jump_action.jump_type IS '跳转类型 _blank 新开页面 _self 当前窗口';
COMMENT ON COLUMN core_visualization_jump_action.target_dv_id IS '关联仪表板ID';
COMMENT ON COLUMN core_visualization_jump_action.source_field_id IS '字段ID';
COMMENT ON COLUMN core_visualization_jump_action.content IS '内容 linkType = outer时使用';
COMMENT ON COLUMN core_visualization_jump_action.checked IS '是否可用';
COMMENT ON COLUMN core_visualization_jump_action.attach_params IS '是否附加点击参数';
COMMENT ON COLUMN core_visualization_jump_action.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_jump_action.copy_id IS '复制来源ID';
COMMENT ON COLUMN core_visualization_jump_action.window_size IS '窗口大小large middle small';
COMMENT ON TABLE core_visualization_jump_target IS '跳转目标仪表板图表字段配置表';
COMMENT ON COLUMN core_visualization_jump_target.target_id IS '主键';
COMMENT ON COLUMN core_visualization_jump_target.link_jump_info_id IS 'core_visualization_jump_action 表的 ID';
COMMENT ON COLUMN core_visualization_jump_target.source_field_active_id IS '勾选字段设置的匹配字段，也可以不是勾选字段本身';
COMMENT ON COLUMN core_visualization_jump_target.target_view_id IS '目标图表ID';
COMMENT ON COLUMN core_visualization_jump_target.target_field_id IS '目标字段ID';
COMMENT ON COLUMN core_visualization_jump_target.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_jump_target.copy_id IS '复制来源ID';
COMMENT ON COLUMN core_visualization_jump_target.target_type IS '联动目标类型 view 图表 filter 过滤组件 outParams 外部参数';
COMMENT ON TABLE core_visualization_linkage IS '联动记录表';
COMMENT ON COLUMN core_visualization_linkage.id IS '主键';
COMMENT ON COLUMN core_visualization_linkage.dv_id IS '联动大屏/仪表板ID';
COMMENT ON COLUMN core_visualization_linkage.source_view_id IS '源图表id';
COMMENT ON COLUMN core_visualization_linkage.target_view_id IS '联动图表id';
COMMENT ON COLUMN core_visualization_linkage.update_time IS '更新时间';
COMMENT ON COLUMN core_visualization_linkage.update_people IS '更新人';
COMMENT ON COLUMN core_visualization_linkage.linkage_active IS '是否启用关联';
COMMENT ON COLUMN core_visualization_linkage.ext1 IS '扩展字段1';
COMMENT ON COLUMN core_visualization_linkage.ext2 IS '扩展字段2';
COMMENT ON COLUMN core_visualization_linkage.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_linkage.copy_id IS '复制来源ID';
COMMENT ON TABLE core_visualization_linkage_field IS '联动字段';
COMMENT ON COLUMN core_visualization_linkage_field.id IS '主键';
COMMENT ON COLUMN core_visualization_linkage_field.linkage_id IS '联动ID';
COMMENT ON COLUMN core_visualization_linkage_field.source_field IS '源图表字段';
COMMENT ON COLUMN core_visualization_linkage_field.target_field IS '目标图表字段';
COMMENT ON COLUMN core_visualization_linkage_field.update_time IS '更新时间';
COMMENT ON COLUMN core_visualization_linkage_field.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_linkage_field.copy_id IS '复制来源ID';
COMMENT ON TABLE core_visualization_parameter IS '外部参数关联关系表';
COMMENT ON COLUMN core_visualization_parameter.params_id IS '主键';
COMMENT ON COLUMN core_visualization_parameter.visualization_id IS '可视化资源ID';
COMMENT ON COLUMN core_visualization_parameter.checked IS '是否启用外部参数标识（1-是，0-否）';
COMMENT ON COLUMN core_visualization_parameter.remark IS '备注';
COMMENT ON COLUMN core_visualization_parameter.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_parameter.copy_id IS '复制来源ID';
COMMENT ON TABLE core_visualization_parameter_item IS '外部参数配置表';
COMMENT ON COLUMN core_visualization_parameter_item.params_info_id IS '主键';
COMMENT ON COLUMN core_visualization_parameter_item.params_id IS 'core_visualization_parameter 表的 ID';
COMMENT ON COLUMN core_visualization_parameter_item.param_name IS '参数名';
COMMENT ON COLUMN core_visualization_parameter_item.checked IS '是否启用';
COMMENT ON COLUMN core_visualization_parameter_item.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_parameter_item.copy_id IS '复制来源ID';
COMMENT ON COLUMN core_visualization_parameter_item.required IS '是否必填';
COMMENT ON COLUMN core_visualization_parameter_item.default_value IS '默认值 JSON格式';
COMMENT ON COLUMN core_visualization_parameter_item.enabled_default IS '是否启用默认值';
COMMENT ON TABLE core_visualization_parameter_target IS '外部参数联动视图字段信息表';
COMMENT ON COLUMN core_visualization_parameter_target.target_id IS '主键';
COMMENT ON COLUMN core_visualization_parameter_target.params_info_id IS 'core_visualization_parameter_item 表的 ID';
COMMENT ON COLUMN core_visualization_parameter_target.target_view_id IS '联动视图ID/联动过滤项ID';
COMMENT ON COLUMN core_visualization_parameter_target.target_field_id IS '联动字段ID';
COMMENT ON COLUMN core_visualization_parameter_target.copy_from IS '复制来源';
COMMENT ON COLUMN core_visualization_parameter_target.copy_id IS '复制来源ID';
COMMENT ON COLUMN core_visualization_parameter_target.target_ds_id IS '联动数据集id/联动过滤组件id';
COMMENT ON COLUMN core_visualization_parameter_target.match_mode IS '匹配方式';
COMMENT ON TABLE core_visualization_report_filter IS '定时报告过自定义过滤组件信息';
COMMENT ON COLUMN core_visualization_report_filter.id IS 'id';
COMMENT ON COLUMN core_visualization_report_filter.report_id IS '定时报告id';
COMMENT ON COLUMN core_visualization_report_filter.task_id IS '任务id';
COMMENT ON COLUMN core_visualization_report_filter.resource_id IS '资源id';
COMMENT ON COLUMN core_visualization_report_filter.dv_type IS '资源类型';
COMMENT ON COLUMN core_visualization_report_filter.component_id IS '组件id';
COMMENT ON COLUMN core_visualization_report_filter.filter_id IS '过滤项id';
COMMENT ON COLUMN core_visualization_report_filter.filter_info IS '过滤组件内容';
COMMENT ON COLUMN core_visualization_report_filter.filter_version IS '过滤组件版本';
COMMENT ON COLUMN core_visualization_report_filter.create_time IS '创建时间';
COMMENT ON COLUMN core_visualization_report_filter.create_user IS '创建人';
COMMENT ON TABLE core_visualization_theme IS '主题表';
COMMENT ON COLUMN core_visualization_theme.id IS '主键';
COMMENT ON COLUMN core_visualization_theme.name IS '主题名称';
COMMENT ON COLUMN core_visualization_theme.type IS '主题类型 system 系统主题，self 自定义主题';
COMMENT ON COLUMN core_visualization_theme.details IS '主题内容';
COMMENT ON COLUMN core_visualization_theme.delete_flag IS '删除标记';
COMMENT ON COLUMN core_visualization_theme.cover_url IS '封面信息';
COMMENT ON COLUMN core_visualization_theme.create_num IS '创建序号';
COMMENT ON COLUMN core_visualization_theme.create_time IS '创建时间';
COMMENT ON COLUMN core_visualization_theme.create_by IS '创建人';
COMMENT ON COLUMN core_visualization_theme.update_time IS '更新时间';
COMMENT ON COLUMN core_visualization_theme.update_by IS '更新人';
COMMENT ON COLUMN core_visualization_theme.delete_time IS '删除时间';
COMMENT ON COLUMN core_visualization_theme.delete_by IS '删除人';
COMMENT ON TABLE core_template IS '模板';
COMMENT ON COLUMN core_template.id IS '主键';
COMMENT ON COLUMN core_template.name IS '名称';
COMMENT ON COLUMN core_template.pid IS '父级id';
COMMENT ON COLUMN core_template."LEVEL" IS '层级';
COMMENT ON COLUMN core_template.dv_type IS '模板种类  dataV or dashboard 目录或者文件夹';
COMMENT ON COLUMN core_template.node_type IS '节点类型  app or template 应用 或者 模板';
COMMENT ON COLUMN core_template.create_by IS '创建人';
COMMENT ON COLUMN core_template.create_time IS '创建时间';
COMMENT ON COLUMN core_template.snapshot IS '缩略图';
COMMENT ON COLUMN core_template.template_type IS '模板类型 system 系统内置 self 用户自建 ';
COMMENT ON COLUMN core_template.template_style IS 'template 样式';
COMMENT ON COLUMN core_template.template_data IS 'template 数据';
COMMENT ON COLUMN core_template.dynamic_data IS '预存数据';
COMMENT ON COLUMN core_template.app_data IS 'app数据';
COMMENT ON COLUMN core_template.use_count IS '使用次数';
COMMENT ON COLUMN core_template.version IS '使用资源的版本';
COMMENT ON TABLE core_template_category IS '模板分类';
COMMENT ON COLUMN core_template_category.id IS '主键';
COMMENT ON COLUMN core_template_category.name IS '名称';
COMMENT ON COLUMN core_template_category.pid IS '父级id';
COMMENT ON COLUMN core_template_category."LEVEL" IS '层级';
COMMENT ON COLUMN core_template_category.dv_type IS '模板种类  dataV or dashboard 目录或者文件夹';
COMMENT ON COLUMN core_template_category.node_type IS '节点类型  folder or panel 目录或者文件夹';
COMMENT ON COLUMN core_template_category.create_by IS '创建人';
COMMENT ON COLUMN core_template_category.create_time IS '创建时间';
COMMENT ON COLUMN core_template_category.snapshot IS '缩略图';
COMMENT ON COLUMN core_template_category.template_type IS '模版类型 system 系统内置 self 用户自建';
COMMENT ON TABLE core_template_category_relation IS '模板分类关系';
COMMENT ON COLUMN core_template_category_relation.id IS '主键';
COMMENT ON COLUMN core_template_category_relation.category_id IS '名称';
COMMENT ON COLUMN core_template_category_relation.template_id IS '父级id';
COMMENT ON TABLE core_template_view_data IS '模板图表明细信息表';
COMMENT ON COLUMN core_template_view_data.id IS '主键';
COMMENT ON COLUMN core_template_view_data.dv_id IS '模板ID';
COMMENT ON COLUMN core_template_view_data.view_id IS '图表ID';
COMMENT ON COLUMN core_template_view_data.view_details IS '图表详情';
COMMENT ON COLUMN core_template_view_data.copy_from IS '复制来源';
COMMENT ON COLUMN core_template_view_data.copy_id IS '复制来源ID';
COMMENT ON TABLE core_visualization_watermark IS '仪表板水印设置表';
COMMENT ON COLUMN core_visualization_watermark.id IS '主键';
COMMENT ON COLUMN core_visualization_watermark.version IS '版本号';
COMMENT ON COLUMN core_visualization_watermark.setting_content IS '设置内容';
COMMENT ON COLUMN core_visualization_watermark.create_by IS '创建人';
COMMENT ON COLUMN core_visualization_watermark.create_time IS '创建时间';
COMMENT ON TABLE core_auth_sso_provider IS '单点登录提供方配置';
COMMENT ON COLUMN core_auth_sso_provider.id IS '主键';
COMMENT ON COLUMN core_auth_sso_provider.provider_key IS '提供方标识';
COMMENT ON COLUMN core_auth_sso_provider.provider_type IS '提供方类型';
COMMENT ON COLUMN core_auth_sso_provider.name IS '提供方名称';
COMMENT ON COLUMN core_auth_sso_provider.enabled IS '是否启用';
COMMENT ON COLUMN core_auth_sso_provider.client_id IS '客户端 ID';
COMMENT ON COLUMN core_auth_sso_provider.client_secret IS '客户端密钥';
COMMENT ON COLUMN core_auth_sso_provider.authorization_endpoint IS '授权端点';
COMMENT ON COLUMN core_auth_sso_provider.token_endpoint IS '令牌端点';
COMMENT ON COLUMN core_auth_sso_provider.user_info_endpoint IS '用户信息端点';
COMMENT ON COLUMN core_auth_sso_provider.issuer IS '签发方';
COMMENT ON COLUMN core_auth_sso_provider.scope IS '授权范围';
COMMENT ON COLUMN core_auth_sso_provider.redirect_uri IS '回调地址';
COMMENT ON COLUMN core_auth_sso_provider.user_id_attribute IS '用户 ID 属性';
COMMENT ON COLUMN core_auth_sso_provider.account_attribute IS '账号属性';
COMMENT ON COLUMN core_auth_sso_provider.name_attribute IS '姓名属性';
COMMENT ON COLUMN core_auth_sso_provider.email_attribute IS '邮箱属性';
COMMENT ON COLUMN core_auth_sso_provider.union_id_attribute IS '统一身份属性';
COMMENT ON COLUMN core_auth_sso_provider.auto_create_user IS '是否自动创建用户';
COMMENT ON COLUMN core_auth_sso_provider.require_https IS '是否要求 HTTPS';
COMMENT ON COLUMN core_auth_sso_provider.create_time IS '创建时间';
COMMENT ON COLUMN core_auth_sso_provider.update_time IS '更新时间';
COMMENT ON TABLE core_auth_sso_identity_binding IS '单点登录身份绑定';
COMMENT ON COLUMN core_auth_sso_identity_binding.id IS '主键';
COMMENT ON COLUMN core_auth_sso_identity_binding.user_id IS '用户 ID';
COMMENT ON COLUMN core_auth_sso_identity_binding.provider_id IS '提供方 ID';
COMMENT ON COLUMN core_auth_sso_identity_binding.provider_type IS '提供方类型';
COMMENT ON COLUMN core_auth_sso_identity_binding.external_subject IS '外部主体 ID';
COMMENT ON COLUMN core_auth_sso_identity_binding.account IS '账号';
COMMENT ON COLUMN core_auth_sso_identity_binding.display_name IS '显示名称';
COMMENT ON COLUMN core_auth_sso_identity_binding.email IS '邮箱';
COMMENT ON COLUMN core_auth_sso_identity_binding.union_id IS '统一身份 ID';
COMMENT ON COLUMN core_auth_sso_identity_binding.last_login_time IS '最近登录时间';
COMMENT ON COLUMN core_auth_sso_identity_binding.create_time IS '创建时间';
COMMENT ON COLUMN core_auth_sso_identity_binding.update_time IS '更新时间';
COMMENT ON TABLE core_audit_log IS '审计日志表';
COMMENT ON COLUMN core_audit_log.id IS '主键';
COMMENT ON COLUMN core_audit_log.operation_type IS '操作类型';
COMMENT ON COLUMN core_audit_log.resource_type IS '资源类型';
COMMENT ON COLUMN core_audit_log.resource_id IS '资源ID';
COMMENT ON COLUMN core_audit_log.resource_name IS '资源名称';
COMMENT ON COLUMN core_audit_log.operation_desc IS '操作描述';
COMMENT ON COLUMN core_audit_log.request_method IS '请求方法';
COMMENT ON COLUMN core_audit_log.request_url IS '请求URL';
COMMENT ON COLUMN core_audit_log.request_params IS '请求参数';
COMMENT ON COLUMN core_audit_log.response_code IS '响应码';
COMMENT ON COLUMN core_audit_log.response_msg IS '响应消息';
COMMENT ON COLUMN core_audit_log.operator_id IS '操作人ID';
COMMENT ON COLUMN core_audit_log.operator_name IS '操作人姓名';
COMMENT ON COLUMN core_audit_log.operator_account IS '操作人账号';
COMMENT ON COLUMN core_audit_log.operator_ip IS '操作人IP';
COMMENT ON COLUMN core_audit_log.operation_time IS '操作时间';
COMMENT ON COLUMN core_audit_log.duration IS '执行时长(ms)';
COMMENT ON TABLE core_iam_org IS '组织表';
COMMENT ON COLUMN core_iam_org.id IS '组织ID';
COMMENT ON COLUMN core_iam_org.pid IS '父组织ID';
COMMENT ON COLUMN core_iam_org.name IS '组织名称';
COMMENT ON COLUMN core_iam_org.code IS '组织编码';
COMMENT ON COLUMN core_iam_org.path IS '组织路径';
COMMENT ON COLUMN core_iam_org."LEVEL" IS '层级';
COMMENT ON COLUMN core_iam_org.sort IS '排序';
COMMENT ON COLUMN core_iam_org.enable IS '是否启用';
COMMENT ON COLUMN core_iam_org.readonly IS '是否只读';
COMMENT ON COLUMN core_iam_org.create_time IS '创建时间';
COMMENT ON COLUMN core_iam_org.update_time IS '更新时间';
COMMENT ON TABLE core_iam_role IS '角色表';
COMMENT ON COLUMN core_iam_role.id IS '角色ID';
COMMENT ON COLUMN core_iam_role.oid IS '组织ID';
COMMENT ON COLUMN core_iam_role.name IS '角色名称';
COMMENT ON COLUMN core_iam_role.code IS '角色编码';
COMMENT ON COLUMN core_iam_role.description IS '描述';
COMMENT ON COLUMN core_iam_role.type_code IS '角色类型';
COMMENT ON COLUMN core_iam_role.readonly IS '是否只读';
COMMENT ON COLUMN core_iam_role.system_role IS '是否系统角色';
COMMENT ON COLUMN core_iam_role.org_admin IS '是否组织管理员';
COMMENT ON COLUMN core_iam_role.create_time IS '创建时间';
COMMENT ON COLUMN core_iam_role.update_time IS '更新时间';
COMMENT ON TABLE core_iam_user_org IS '用户组织关系表';
COMMENT ON COLUMN core_iam_user_org.id IS 'ID';
COMMENT ON COLUMN core_iam_user_org."UID" IS '用户ID';
COMMENT ON COLUMN core_iam_user_org.oid IS '组织ID';
COMMENT ON COLUMN core_iam_user_org.default_org IS '是否默认组织';
COMMENT ON COLUMN core_iam_user_org.create_time IS '创建时间';
COMMENT ON TABLE core_iam_user_role IS '用户角色关系表';
COMMENT ON COLUMN core_iam_user_role.id IS 'ID';
COMMENT ON COLUMN core_iam_user_role."UID" IS '用户ID';
COMMENT ON COLUMN core_iam_user_role.oid IS '组织ID';
COMMENT ON COLUMN core_iam_user_role.rid IS '角色ID';
COMMENT ON COLUMN core_iam_user_role.create_time IS '创建时间';
COMMENT ON TABLE core_iam_role_menu_permission IS '角色菜单权限表';
COMMENT ON COLUMN core_iam_role_menu_permission.id IS 'ID';
COMMENT ON COLUMN core_iam_role_menu_permission.rid IS '角色ID';
COMMENT ON COLUMN core_iam_role_menu_permission.menu_id IS '菜单ID';
COMMENT ON COLUMN core_iam_role_menu_permission.permission IS '权限动作';
COMMENT ON COLUMN core_iam_role_menu_permission.create_time IS '创建时间';
COMMENT ON TABLE core_iam_resource_index IS '统一资源索引表';
COMMENT ON COLUMN core_iam_resource_index.id IS 'ID';
COMMENT ON COLUMN core_iam_resource_index.resource_id IS '业务资源ID';
COMMENT ON COLUMN core_iam_resource_index.resource_type IS '资源类型';
COMMENT ON COLUMN core_iam_resource_index.oid IS '组织ID';
COMMENT ON COLUMN core_iam_resource_index.creator IS '创建人';
COMMENT ON COLUMN core_iam_resource_index.name IS '资源名称';
COMMENT ON COLUMN core_iam_resource_index.create_time IS '创建时间';
COMMENT ON COLUMN core_iam_resource_index.update_time IS '更新时间';
COMMENT ON TABLE core_iam_resource_permission IS '资源授权表';
COMMENT ON COLUMN core_iam_resource_permission.id IS 'ID';
COMMENT ON COLUMN core_iam_resource_permission.resource_type IS '资源类型';
COMMENT ON COLUMN core_iam_resource_permission.resource_id IS '业务资源ID';
COMMENT ON COLUMN core_iam_resource_permission.target_type IS '授权对象类型 user/role/org';
COMMENT ON COLUMN core_iam_resource_permission.target_id IS '授权对象ID';
COMMENT ON COLUMN core_iam_resource_permission.permission IS '权限动作';
COMMENT ON COLUMN core_iam_resource_permission.create_time IS '创建时间';
COMMENT ON TABLE core_scheduled_task_queue_state IS '报表和填报调度队列状态';
COMMENT ON COLUMN core_scheduled_task_queue_state.state_id IS '队列状态主键';
COMMENT ON COLUMN core_scheduled_task_queue_state.task_type IS '调度任务类型';
COMMENT ON COLUMN core_scheduled_task_queue_state.task_id IS '业务任务编号';
COMMENT ON COLUMN core_scheduled_task_queue_state.payload_hash IS '任务参数摘要';
COMMENT ON COLUMN core_scheduled_task_queue_state.status IS '队列执行状态';
COMMENT ON COLUMN core_scheduled_task_queue_state.worker_id IS '执行 Worker 编号';
COMMENT ON COLUMN core_scheduled_task_queue_state.enqueue_time IS '投递时间';
COMMENT ON COLUMN core_scheduled_task_queue_state.start_time IS '开始执行时间';
COMMENT ON COLUMN core_scheduled_task_queue_state.end_time IS '结束执行时间';
COMMENT ON COLUMN core_scheduled_task_queue_state.heartbeat_time IS 'Worker 心跳时间';
COMMENT ON COLUMN core_scheduled_task_queue_state.retry_count IS '执行重试次数';
COMMENT ON COLUMN core_scheduled_task_queue_state.lock_version IS '抢占版本号';
COMMENT ON COLUMN core_scheduled_task_queue_state.last_error IS '最近执行错误';
