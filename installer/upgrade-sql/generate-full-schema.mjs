import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '../..')

const definitions = [
  {
    name: 'MySQL',
    output: 'v2.0.2_full_schema_mysql.sql',
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
    output: 'v2.0.2_full_schema_ob_mysql.sql',
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
    output: 'v2.0.2_full_schema_ob_oracle.sql',
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
  ['qrtz_instance', 'scheduler_fire_instance_id'],
  ['`scheduler_fire_instance_id` varchar(1024) DEFAULT NULL COMMENT \'Quartz 实例 ID\'', '`scheduler_fire_instance_id` varchar(1024) DEFAULT NULL COMMENT \'调度触发实例编号\''],
  ['`scheduler_fire_instance_id` longtext COMMENT \'状态\'', '`scheduler_fire_instance_id` longtext COMMENT \'调度触发实例编号\''],
  ['CREATE INDEX `SCHED_NAME` ON `core_schedule_triggers`', 'CREATE INDEX `idx_core_schedule_triggers_job` ON `core_schedule_triggers`'],
  ['CREATE INDEX SCHED_NAME ON core_schedule_triggers', 'CREATE INDEX idx_core_schedule_triggers_job ON core_schedule_triggers']
]

const orderedTableNameReplacements = tableNameReplacements.sort((left, right) => right[0].length - left[0].length)

const orderedTextReplacements = [...schedulerMetadataReplacements, ...runtimeIdentifierReplacements].sort(
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
      ? [
          '',
          "COMMENT ON COLUMN core_dataset.scheduler_fire_instance_id IS '调度触发实例编号';",
          "COMMENT ON COLUMN core_datasource.scheduler_fire_instance_id IS '调度触发实例编号';",
          ''
        ].join('\n')
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
