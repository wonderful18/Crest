import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '../..')

const definitions = [
  {
    name: 'MySQL',
    output: 'v2.0.3_full_schema_mysql.sql',
    sources: [
      'core/core-backend/src/main/resources/db/migration/V1.5.0.1__initial_schema.sql',
      'core/core-backend/src/main/resources/db/migration/V1.5.8.1__data_asset_center.sql',
      'core/core-backend/src/main/resources/db/migration/V1.5.8.2__data_asset_profile_collation.sql',
      'core/core-backend/src/main/resources/db/migration/V2.0.0.1__export_task_queue.sql',
      'core/core-backend/src/main/resources/db/migration/V2.0.0.2__datasource_sync_task_queue.sql',
      'core/core-backend/src/main/resources/db/migration/V2.0.0.3__dataset_sync_task_queue.sql',
      'core/core-backend/src/main/resources/db/migration/V2.0.0.4__dataset_sync_task_queue_metadata.sql',
      'core/core-backend/src/main/resources/db/migration/V2.0.0.5__scheduled_task_queue_state.sql',
      'core/core-backend/src/main/resources/db/migration/V2.0.0.6__audit_log_filter_index.sql',
      'core/core-backend/src/main/resources/db/migration/V2.0.1.1__rename_metadata_tables.sql',
      'core/core-backend/src/main/resources/db/migration/V2.0.1.2__normalize_runtime_identifiers.sql',
      'core/core-backend/src/main/resources/db/migration/V2.0.2.1__normalize_scheduler_metadata_names.sql'
    ]
  },
  {
    name: 'OceanBase MySQL',
    output: 'v2.0.3_full_schema_ob_mysql.sql',
    sources: [
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V1.5.0.1__initial_schema.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V1.5.8.1__data_asset_center.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V1.5.8.2__data_asset_profile_collation.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.0.1__export_task_queue.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.0.2__datasource_sync_task_queue.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.0.3__dataset_sync_task_queue.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.0.4__dataset_sync_task_queue_metadata.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.0.5__scheduled_task_queue_state.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.0.6__audit_log_filter_index.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.1.1__rename_metadata_tables.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.1.2__normalize_runtime_identifiers.sql',
      'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.2.1__normalize_scheduler_metadata_names.sql'
    ]
  },
  {
    name: 'OceanBase Oracle',
    output: 'v2.0.3_full_schema_ob_oracle.sql',
    sources: [
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V1.5.0.1__initial_schema.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.1__export_task_queue.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.2__datasource_sync_task_queue.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.3__dataset_sync_task_queue.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.4__dataset_sync_task_queue_metadata.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.5__scheduled_task_queue_state.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.6__repair_seed_text_encoding.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.0.7__audit_log_filter_index.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.1.1__rename_metadata_tables.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.1.2__normalize_runtime_identifiers.sql',
      'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.2.1__normalize_scheduler_metadata_names.sql'
    ]
  }
]

const checkMode = process.argv.includes('--check')
const skippedSourceNames = new Set([
  'V2.0.0.6__repair_seed_text_encoding.sql',
  'V2.0.1.1__rename_metadata_tables.sql',
  'V2.0.1.2__normalize_runtime_identifiers.sql',
  'V2.0.2.1__normalize_scheduler_metadata_names.sql'
])

const read = relativePath => readFileSync(path.join(root, relativePath), 'utf8')

const parseTableRenamePairs = relativePath => {
  const source = read(relativePath)
  return [...source.matchAll(/crest_rename_table_if_needed\('([^']+)', '([^']+)'\)/g)].map(
    match => [match[1], match[2]]
  )
}

const parseTextReplacePairs = relativePath => {
  const source = read(relativePath)
  const pairs = [...source.matchAll(/crest_replace_text_if_needed\([^,]+,[^,]+,\s*'([^']*)',\s*'([^']*)'\)/g)].map(
    match => [match[1], match[2]]
  )
  return [...new Map(pairs.map(pair => [pair.join('\u0000'), pair])).values()]
}

const tableRenamePairs = [
  ...parseTableRenamePairs('core/core-backend/src/main/resources/db/migration/V2.0.1.1__rename_metadata_tables.sql'),
  ...parseTableRenamePairs(
    'core/core-backend/src/main/resources/db/migration/V2.0.2.1__normalize_scheduler_metadata_names.sql'
  )
]
const tableRenameMap = new Map(tableRenamePairs)

const resolveFinalTableName = tableName => {
  let current = tableName
  const visited = new Set()
  while (tableRenameMap.has(current) && !visited.has(current)) {
    visited.add(current)
    current = tableRenameMap.get(current)
  }
  return current
}

const tableNameReplacements = [...new Set(tableRenamePairs.map(([oldName]) => oldName))]
  .map(oldName => [oldName, resolveFinalTableName(oldName)])
  .filter(([oldName, newName]) => oldName !== newName)
  .flatMap(([oldName, newName]) => [
    [oldName, newName],
    [oldName.toUpperCase(), newName]
  ])

const runtimeIdentifierReplacements = parseTextReplacePairs(
  'core/core-backend/src/main/resources/db/migration/V2.0.1.2__normalize_runtime_identifiers.sql'
)

const schedulerMetadataReplacements = [
  ['QRTZ_BLOB_TRIGGERS_ibfk_1', 'fk_csch_blob_trigger'],
  ['QRTZ_CRON_TRIGGERS_ibfk_1', 'fk_csch_cron_trigger'],
  ['QRTZ_SIMPLE_TRIGGERS_ibfk_1', 'fk_csch_simple_trigger'],
  ['QRTZ_SIMPROP_TRIGGERS_ibfk_1', 'fk_csch_simprop_trigger'],
  ['QRTZ_TRIGGERS_ibfk_1', 'fk_csch_trigger_job'],
  ['`qrtz_instance` varchar(1024) DEFAULT NULL COMMENT \'Quartz 实例 ID\'', '`scheduler_fire_instance_id` varchar(1024) DEFAULT NULL COMMENT \'调度触发实例编号\''],
  ['`qrtz_instance` longtext COMMENT \'状态\'', '`scheduler_fire_instance_id` longtext COMMENT \'调度触发实例编号\''],
  ['qrtz_instance', 'scheduler_fire_instance_id'],
  ['`scheduler_fire_instance_id` varchar(1024) DEFAULT NULL COMMENT \'Quartz 实例 ID\'', '`scheduler_fire_instance_id` varchar(1024) DEFAULT NULL COMMENT \'调度触发实例编号\''],
  ['`scheduler_fire_instance_id` longtext COMMENT \'状态\'', '`scheduler_fire_instance_id` longtext COMMENT \'调度触发实例编号\''],
  ['CREATE INDEX `SCHED_NAME` ON `core_schedule_triggers`', 'CREATE INDEX `idx_core_schedule_triggers_job` ON `core_schedule_triggers`'],
  ['CREATE INDEX SCHED_NAME ON core_schedule_triggers', 'CREATE INDEX idx_core_schedule_triggers_job ON core_schedule_triggers']
]

const commentTextReplacements = [
  ["COMMENT 'api'", "COMMENT '接口'"],
  ["COMMENT 'table-row'", "COMMENT '表格行扩展配置'"],
  ["COMMENT 'user or api'", "COMMENT '消息来源类型'"],
  ["COMMENT 'mysql oracle ...'", "COMMENT '数据库引擎类型'"],
  ["COMMENT 'create sql'", "COMMENT '建表 SQL'"],
  ["COMMENT 'free or license'", "COMMENT '令牌类型'"],
  ["COMMENT 'Create timestamp'", "COMMENT '创建时间'"],
  ["COMMENT 'Update timestamp'", "COMMENT '更新时间'"],
  ["COMMENT 'uuid'", "COMMENT 'UUID'"],
  ["COMMENT 'ticket'", "COMMENT '分享票据'"],
  ["COMMENT 'Web hooks'", "COMMENT 'Webhook 接收人'"],
  ["COMMENT 'url'", "COMMENT '地址'"],
  ["COMMENT 'content_type'", "COMMENT '内容类型'"],
  ["COMMENT 'link jump ID'", "COMMENT '跳转配置ID'"],
  ["COMMENT 'SM2 private key'", "COMMENT 'SM2 私钥'"],
  ["COMMENT 'SM2 public key'", "COMMENT 'SM2 公钥'"],
  ["COMMENT 'SM2 key creation time'", "COMMENT 'SM2 密钥生成时间'"]
]

const tableCommentOverrides = new Map([
  ['core_query_assistant_config', '查询助手配置'],
  ['core_query_assistant_message', '查询助手问答记录'],
  ['core_query_assistant_token', '查询助手令牌记录'],
  ['core_chart_view', '图表组件'],
  ['core_auth_token', '认证令牌信息'],
  ['core_template', '模板'],
  ['core_template_category', '模板分类'],
  ['core_template_category_relation', '模板分类关系'],
  ['core_webhook_config', 'Webhook 配置'],
  ['core_chart_view_snapshot', '图表快照'],
  ['core_visualization_snapshot', '可视化资源快照'],
  ['core_visualization_jump_snapshot', '跳转记录快照'],
  ['core_visualization_jump_action_snapshot', '跳转配置快照'],
  ['core_visualization_jump_target_snapshot', '跳转目标配置快照'],
  ['core_visualization_linkage_snapshot', '联动记录快照'],
  ['core_visualization_linkage_field_snapshot', '联动字段快照'],
  ['core_visualization_parameter_snapshot', '外部参数关联快照'],
  ['core_visualization_parameter_item_snapshot', '外部参数配置快照'],
  ['core_visualization_parameter_target_snapshot', '外部参数目标字段快照'],
  ['core_auth_sso_provider', '单点登录提供方配置'],
  ['core_auth_sso_identity_binding', '单点登录身份绑定']
])

const quartzColumnComments = {
  sched_name: '调度器名称',
  trigger_name: '触发器名称',
  trigger_group: '触发器分组',
  blob_data: '触发器二进制数据',
  calendar_name: '日历名称',
  calendar: '日历数据',
  cron_expression: 'Cron 表达式',
  time_zone_id: '时区 ID',
  entry_id: '触发记录 ID',
  instance_name: '调度实例名称',
  fired_time: '触发时间',
  sched_time: '计划触发时间',
  priority: '优先级',
  state: '状态',
  job_name: '作业名称',
  job_group: '作业分组',
  is_nonconcurrent: '是否禁止并发',
  requests_recovery: '是否请求恢复',
  description: '描述',
  job_class_name: '作业类名',
  is_durable: '是否持久化',
  is_update_data: '是否更新作业数据',
  job_data: '作业数据',
  lock_name: '锁名称',
  last_checkin_time: '最近心跳时间',
  checkin_interval: '心跳间隔',
  repeat_count: '重复次数',
  repeat_interval: '重复间隔',
  times_triggered: '已触发次数',
  str_prop_1: '字符串属性 1',
  str_prop_2: '字符串属性 2',
  str_prop_3: '字符串属性 3',
  int_prop_1: '整数属性 1',
  int_prop_2: '整数属性 2',
  long_prop_1: '长整数属性 1',
  long_prop_2: '长整数属性 2',
  dec_prop_1: '小数属性 1',
  dec_prop_2: '小数属性 2',
  bool_prop_1: '布尔属性 1',
  bool_prop_2: '布尔属性 2',
  next_fire_time: '下次触发时间',
  prev_fire_time: '上次触发时间',
  trigger_state: '触发器状态',
  trigger_type: '触发器类型',
  start_time: '开始时间',
  end_time: '结束时间',
  misfire_instr: '错过触发处理策略'
}

const columnCommentOverrides = new Map([
  ...Object.entries(quartzColumnComments).flatMap(([column, comment]) =>
    [
      'core_schedule_blob_triggers',
      'core_schedule_calendars',
      'core_schedule_cron_triggers',
      'core_schedule_fired_triggers',
      'core_schedule_job_details',
      'core_schedule_locks',
      'core_schedule_paused_trigger_groups',
      'core_schedule_scheduler_state',
      'core_schedule_simple_triggers',
      'core_schedule_simprop_triggers',
      'core_schedule_triggers'
    ].map(table => [`${table}.${column}`, comment])
  ),
  ['core_export_download_task.id', '下载任务 ID'],
  ['core_export_download_task.create_time', '创建时间'],
  ['core_export_download_task.valid_time', '有效截止时间'],
  ['core_crypto_key.sm2_private_key', 'SM2 私钥'],
  ['core_crypto_key.sm2_public_key', 'SM2 公钥'],
  ['core_crypto_key.sm2_create_time', 'SM2 密钥生成时间'],
  ['core_iam_user.auth_type', '认证类型'],
  ['core_iam_user.external_id', '外部身份 ID'],
  ['core_iam_user.last_login_time', '最近登录时间'],
  ['core_auth_sso_provider.id', '主键'],
  ['core_auth_sso_provider.provider_key', '提供方标识'],
  ['core_auth_sso_provider.provider_type', '提供方类型'],
  ['core_auth_sso_provider.name', '提供方名称'],
  ['core_auth_sso_provider.enabled', '是否启用'],
  ['core_auth_sso_provider.client_id', '客户端 ID'],
  ['core_auth_sso_provider.client_secret', '客户端密钥'],
  ['core_auth_sso_provider.authorization_endpoint', '授权端点'],
  ['core_auth_sso_provider.token_endpoint', '令牌端点'],
  ['core_auth_sso_provider.user_info_endpoint', '用户信息端点'],
  ['core_auth_sso_provider.issuer', '签发方'],
  ['core_auth_sso_provider.scope', '授权范围'],
  ['core_auth_sso_provider.redirect_uri', '回调地址'],
  ['core_auth_sso_provider.user_id_attribute', '用户 ID 属性'],
  ['core_auth_sso_provider.account_attribute', '账号属性'],
  ['core_auth_sso_provider.name_attribute', '姓名属性'],
  ['core_auth_sso_provider.email_attribute', '邮箱属性'],
  ['core_auth_sso_provider.union_id_attribute', '统一身份属性'],
  ['core_auth_sso_provider.auto_create_user', '是否自动创建用户'],
  ['core_auth_sso_provider.require_https', '是否要求 HTTPS'],
  ['core_auth_sso_provider.create_time', '创建时间'],
  ['core_auth_sso_provider.update_time', '更新时间'],
  ['core_auth_sso_identity_binding.id', '主键'],
  ['core_auth_sso_identity_binding.user_id', '用户 ID'],
  ['core_auth_sso_identity_binding.provider_id', '提供方 ID'],
  ['core_auth_sso_identity_binding.provider_type', '提供方类型'],
  ['core_auth_sso_identity_binding.external_subject', '外部主体 ID'],
  ['core_auth_sso_identity_binding.account', '账号'],
  ['core_auth_sso_identity_binding.display_name', '显示名称'],
  ['core_auth_sso_identity_binding.email', '邮箱'],
  ['core_auth_sso_identity_binding.union_id', '统一身份 ID'],
  ['core_auth_sso_identity_binding.last_login_time', '最近登录时间'],
  ['core_auth_sso_identity_binding.create_time', '创建时间'],
  ['core_auth_sso_identity_binding.update_time', '更新时间'],
  ['core_chart_view.scene_id', '所属可视化资源 ID'],
  ['core_chart_view.x_axis', '横轴字段配置'],
  ['core_chart_view.y_axis', '纵轴字段配置'],
  ['core_chart_view.flow_map_start_name', '流向地图起点字段'],
  ['core_chart_view.flow_map_end_name', '流向地图终点字段'],
  ['core_chart_view.ext_color', '颜色维度字段'],
  ['core_query_assistant_config.copilot_url', '查询助手服务地址'],
  ['core_query_assistant_message.copilot_sql', '查询助手返回 SQL'],
  ['core_query_assistant_message.api_msg', '接口返回信息'],
  ['core_query_assistant_message.chart_ok', '图表生成状态'],
  ['core_query_assistant_message.chart', '图表配置'],
  ['core_query_assistant_message.msg_status', '消息处理状态'],
  ['core_query_assistant_message.err_msg', '错误信息'],
  ['core_query_assistant_token.token', '令牌值'],
  ['core_dataset.mode', '连接模式：0 直连，1 同步'],
  ['core_asset_profile.asset_type', '资产类型'],
  ['core_dataset_table.type', '数据表类型'],
  ['core_auth_token.token', '认证令牌'],
  ['core_share_ticket.uuid', '分享 UUID'],
  ['core_chart_view_snapshot.scene_id', '所属可视化资源 ID'],
  ['core_chart_view_snapshot.x_axis', '横轴字段配置'],
  ['core_chart_view_snapshot.y_axis', '纵轴字段配置'],
  ['core_chart_view_snapshot.flow_map_start_name', '流向地图起点字段'],
  ['core_chart_view_snapshot.flow_map_end_name', '流向地图终点字段'],
  ['core_chart_view_snapshot.ext_color', '颜色维度字段'],
  ['core_visualization_background.url', '图片地址'],
  ['core_visualization_background_image.url', '图片地址'],
  ['core_audit_log.id', '主键']
])

const orderedTableNameReplacements = tableNameReplacements.sort((left, right) => right[0].length - left[0].length)

const orderedTextReplacements = [...schedulerMetadataReplacements, ...commentTextReplacements, ...runtimeIdentifierReplacements].sort(
  (left, right) => right[0].length - left[0].length
)

const escapeRegExp = value => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const replaceSqlIdentifier = (source, oldName, newName) =>
  source.replace(new RegExp(`(?<![A-Za-z0-9_])${escapeRegExp(oldName)}(?![A-Za-z0-9_])`, 'g'), newName)

const applyFinalNaming = source => {
  const withTableNames = orderedTableNameReplacements.reduce(
    (content, [oldValue, newValue]) => replaceSqlIdentifier(content, oldValue, newValue),
    source
  )
  return orderedTextReplacements.reduce(
    (content, [oldValue, newValue]) => content.split(oldValue).join(newValue),
    withTableNames
  )
}

const unescapeMysqlComment = value => value.replace(/\\'/g, "'")

const escapeOracleComment = value => value.replace(/'/g, "''")

const normalizeIdentifier = value => value.replace(/^`|`$/g, '').replace(/^"|"$/g, '').toLowerCase()

const splitSqlStatements = source => source.split(/;\s*(?:\r?\n|$)/)

const upsertTableComments = (comments, tableName) => {
  const key = normalizeIdentifier(tableName)
  if (!comments.has(key)) {
    comments.set(key, { table: '', columns: new Map() })
  }
  return comments.get(key)
}

const extractMysqlComments = source => {
  const comments = new Map()

  for (const statement of splitSqlStatements(source)) {
    const match = statement.match(/CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?([A-Za-z0-9_]+)`?/i)
    const alterMatch = statement.match(/ALTER TABLE\s+`?([A-Za-z0-9_]+)`?/i)

    if (!match && !alterMatch) {
      continue
    }

    if (alterMatch) {
      const tableComments = upsertTableComments(comments, alterMatch[1])
      const columnCommentPattern = /(?:ADD|MODIFY)\s+COLUMN\s+`?([A-Za-z0-9_]+)`?[\s\S]*?\bCOMMENT\s+'((?:\\'|[^'])*)'/gi
      for (const columnComment of statement.matchAll(columnCommentPattern)) {
        tableComments.columns.set(
          normalizeIdentifier(columnComment[1]),
          unescapeMysqlComment(columnComment[2])
        )
      }
      continue
    }

    const tableName = match[1]
    const bodyStart = statement.indexOf('(', match.index)
    const bodyEnd = statement.lastIndexOf(')')
    if (bodyStart < 0 || bodyEnd <= bodyStart) {
      continue
    }

    const body = statement.slice(bodyStart + 1, bodyEnd)
    const tableOptions = statement.slice(bodyEnd + 1)
    const tableComments = upsertTableComments(comments, tableName)
    const tableComment = tableOptions.match(/\bCOMMENT\s*=\s*'((?:\\'|[^'])*)'/i)

    if (tableComment) {
      tableComments.table = unescapeMysqlComment(tableComment[1])
    }

    body.split('\n').forEach(line => {
      const columnComment = line.match(/^\s*`?([A-Za-z0-9_]+)`?\s+[\s\S]*?\bCOMMENT\s+'((?:\\'|[^'])*)'/i)
      if (columnComment) {
        tableComments.columns.set(normalizeIdentifier(columnComment[1]), unescapeMysqlComment(columnComment[2]))
      }
    })
  }

  return comments
}

const extractOracleTables = source => {
  const tables = []

  for (const statement of splitSqlStatements(source)) {
    const match = statement.match(/CREATE TABLE\s+(?:IF NOT EXISTS\s+)?("?[A-Za-z0-9_]+"?)/i)
    if (!match) {
      continue
    }

    const tableIdentifier = match[1]
    const bodyStart = statement.indexOf('(', match.index)
    const bodyEnd = statement.lastIndexOf(')')
    if (bodyStart < 0 || bodyEnd <= bodyStart) {
      continue
    }

    const body = statement.slice(bodyStart + 1, bodyEnd)
    const columns = new Map()

    body.split('\n').forEach(line => {
      const column = line.match(/^\s*("?[A-Za-z0-9_]+"?)\s+/)
      if (!column) {
        return
      }

      const name = normalizeIdentifier(column[1])
      if (['constraint', 'primary', 'unique', 'index', 'foreign', 'key'].includes(name)) {
        return
      }

      columns.set(name, column[1])
    })

    tables.push({
      name: normalizeIdentifier(tableIdentifier),
      identifier: tableIdentifier,
      columns
    })
  }

  return tables
}

const buildOracleComments = (oracleSource, mysqlCommentSource) => {
  const mysqlComments = extractMysqlComments(mysqlCommentSource)
  const statements = []

  for (const table of extractOracleTables(oracleSource)) {
    const comments = mysqlComments.get(table.name)
    if (!comments) {
      continue
    }

    const tableComment = tableCommentOverrides.get(table.name) || comments.table
    if (tableComment) {
      statements.push(`COMMENT ON TABLE ${table.identifier} IS '${escapeOracleComment(tableComment)}';`)
    }

    for (const [columnName, columnIdentifier] of table.columns) {
      const comment = columnCommentOverrides.get(`${table.name}.${columnName}`) || comments.columns.get(columnName)
      if (comment) {
        statements.push(
          `COMMENT ON COLUMN ${table.identifier}.${columnIdentifier} IS '${escapeOracleComment(comment)}';`
        )
      }
    }
  }

  return statements.length > 0
    ? ['', '-- 表和字段备注', ...statements, ''].join('\n')
    : ''
}

const separator = source =>
  [
    '',
    '-- ----------------------------------------------------------------------',
    `-- Source: ${source}`,
    '-- ----------------------------------------------------------------------',
    ''
  ].join('\n')

const buildContent = definition => {
  const header = [
    `-- Crest v2.0.2 ${definition.name} 空库一次性初始化 SQL。`,
    '-- 仅适用于新建 Crest 系统库；已有系统库请执行对应增量升级 SQL。',
    '-- 本文件直接使用 v2.0.2 最终命名，不包含升级过程中的重命名 SQL。',
    '-- 本文件由 installer/upgrade-sql/generate-full-schema.mjs 生成。',
    ''
  ].join('\n')

  const body = definition.sources
    .filter(source => !skippedSourceNames.has(path.basename(source)))
    .map(source => {
      const sourcePath = path.join(root, source)
      return `${separator(source)}${applyFinalNaming(readFileSync(sourcePath, 'utf8')).trim()}\n`
    })
    .join('\n')
  const oracleComments =
    definition.name === 'OceanBase Oracle'
      ? buildOracleComments(body, buildContent(definitions.find(item => item.name === 'OceanBase MySQL')))
      : ''

  return `${header}${body}${oracleComments}`
}

for (const definition of definitions) {
  const outputPath = path.join(__dirname, definition.output)
  const expected = buildContent(definition)

  if (checkMode) {
    if (!existsSync(outputPath) || readFileSync(outputPath, 'utf8') !== expected) {
      console.error(`${definition.output} is not up to date`)
      process.exitCode = 1
    }
    continue
  }

  writeFileSync(outputPath, expected, 'utf8')
  console.log(`generated ${definition.output}`)
}
