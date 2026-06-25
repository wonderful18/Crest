import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '../../..')

const read = relativePath => readFileSync(path.join(root, relativePath), 'utf8')

const walk = directory => {
  const files = []
  for (const entry of readdirSync(directory)) {
    const fullPath = path.join(directory, entry)
    const stat = statSync(fullPath)
    if (stat.isDirectory()) {
      files.push(...walk(fullPath))
    } else {
      files.push(fullPath)
    }
  }
  return files
}

const runtimeScanRoots = [
  'core/core-backend/src/main/java',
  'core/core-backend/src/main/resources/mybatis',
  'core/core-frontend/src',
  'sdk/api',
  'sdk/common',
  'sdk/extensions',
  'installer',
  'deploy'
]

const runtimeScanFiles = ['core/core-frontend/index.html']

const runtimeScanExcludes = [
  '/node_modules/',
  '/target/',
  '/upgrade-sql/',
  '/init-sql/',
  '/docs/release/'
]

const forbiddenRuntimePatterns = [
  /DataEase/,
  /dataease/,
  /DET2APP/,
  /DET2/,
  /\.DET2/i,
  /\|DE\|/,
  /\bDE\b/,
  /\bDE_(CAST_DATE_FORMAT|UNIX_TIMESTAMP|STR_TO_DATE|FROM_UNIXTIME|DATE_FORMAT)\b/,
  /-de-/,
  /\b[A-Za-z0-9_]*_de(?:\b|_[A-Za-z0-9_]+)/,
  /_de_/,
  /(^|[^A-Za-z0-9_])de-[A-Za-z0-9_-]+/,
  /de-preview-content/,
  /de-canvas-canvas-main/,
  /de-chart-active/,
  /de-map-rendering/,
  /de-map-rendered/,
  /de-dialog-form/,
  /QRTZ_/,
  /Qrtz/,
  /qrtz_/,
  /core_template_category_rel\b/,
  /core_export_download\b/,
  /dataset_table_join(?!_relation)/
]

const versionPomFiles = [
  'pom.xml',
  'core/pom.xml',
  'core/core-backend/pom.xml',
  'core/core-frontend/pom.xml',
  'sdk/pom.xml',
  'sdk/api/pom.xml',
  'sdk/api/api-base/pom.xml',
  'sdk/api/api-permissions/pom.xml',
  'sdk/api/api-sync/pom.xml',
  'sdk/common/pom.xml',
  'sdk/extensions/pom.xml',
  'sdk/extensions/extensions-datafilling/pom.xml',
  'sdk/extensions/extensions-datasource/pom.xml',
  'sdk/extensions/extensions-view/pom.xml'
]

const dbTypes = [
  {
    name: 'mysql',
    migrationDir: 'migration',
    suffix: 'mysql',
    fullSchema: 'v2.0.2_full_schema_mysql.sql'
  },
  {
    name: 'ob-mysql',
    migrationDir: 'migration-ob-mysql',
    suffix: 'ob_mysql',
    fullSchema: 'v2.0.2_full_schema_ob_mysql.sql'
  },
  {
    name: 'ob-oracle',
    migrationDir: 'migration-ob-oracle',
    suffix: 'ob_oracle',
    fullSchema: 'v2.0.2_full_schema_ob_oracle.sql'
  }
]

test('runtime source paths no longer contain legacy external names or markers', () => {
  const failures = []

  const scanFile = file => {
    const relativePath = path.relative(root, file)
    if (!/\.(java|xml|ts|tsx|js|jsx|vue|less|css|scss|html|yaml|yml|sql|md)$/.test(file)) {
      return
    }

    const source = readFileSync(file, 'utf8')
    forbiddenRuntimePatterns.forEach(pattern => {
      if (pattern.test(source)) {
        failures.push(`${relativePath}: ${pattern}`)
      }
    })
  }

  for (const relativeRoot of runtimeScanRoots) {
    const absoluteRoot = path.join(root, relativeRoot)
    if (!existsSync(absoluteRoot)) {
      continue
    }

    for (const file of walk(absoluteRoot)) {
      const relativePath = path.relative(root, file)
      if (runtimeScanExcludes.some(exclude => file.includes(exclude))) {
        continue
      }
      scanFile(file)
    }
  }

  for (const relativeFile of runtimeScanFiles) {
    const absoluteFile = path.join(root, relativeFile)
    if (existsSync(absoluteFile)) {
      scanFile(absoluteFile)
    }
  }

  assert.deepEqual(failures, [])
})

test('project version is 2.0.2 across release metadata', () => {
  assert.equal(read('VERSION').trim(), '2.0.2')
  assert.match(read('core/core-frontend/package.json'), /"version": "2\.0\.2"/)

  for (const file of versionPomFiles) {
    const source = read(file)
    assert.doesNotMatch(source, /<version>2\.0\.0<\/version>/, file)
  }

  assert.match(read('pom.xml'), /<version>2\.0\.2<\/version>/)
  assert.match(read('pom.xml'), /<crest\.version>2\.0\.2<\/crest\.version>/)
})

test('v2.0.1 and v2.0.2 migration scripts are present for every supported system database', () => {
  for (const dbType of dbTypes) {
    const migrationBase = `core/core-backend/src/main/resources/db/${dbType.migrationDir}`
    assert.ok(existsSync(path.join(root, migrationBase, 'V2.0.1.1__rename_metadata_tables.sql')))
    assert.ok(
      existsSync(path.join(root, migrationBase, 'V2.0.1.2__normalize_runtime_identifiers.sql'))
    )

    assert.ok(
      existsSync(
        path.join(
          root,
          `installer/upgrade-sql/v2.0.0_to_v2.0.1_metadata_tables_${dbType.suffix}.sql`
        )
      )
    )
    assert.ok(
      existsSync(
        path.join(
          root,
          `installer/upgrade-sql/v2.0.0_to_v2.0.1_runtime_identifiers_${dbType.suffix}.sql`
        )
      )
    )
    assert.ok(
      existsSync(
        path.join(root, migrationBase, 'V2.0.2.1__normalize_scheduler_metadata_names.sql')
      )
    )
    assert.ok(
      existsSync(path.join(root, migrationBase, 'V2.0.2.2__rotate_default_crypto_key.sql'))
    )
    assert.ok(
      existsSync(
        path.join(
          root,
          `installer/upgrade-sql/v2.0.1_to_v2.0.2_metadata_names_${dbType.suffix}.sql`
        )
      )
    )
    assert.ok(
      existsSync(
        path.join(
          root,
          `installer/upgrade-sql/v2.0.1_to_v2.0.2_rotate_default_crypto_key_${dbType.suffix}.sql`
        )
      )
    )
  }
})

test('scheduler metadata migrations cover the remaining mixed table names', () => {
  const mysqlMigration = read(
    'core/core-backend/src/main/resources/db/migration/V2.0.2.1__normalize_scheduler_metadata_names.sql'
  )
  const obMysqlMigration = read(
    'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.2.1__normalize_scheduler_metadata_names.sql'
  )
  const obOracleMigration = read(
    'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.2.1__normalize_scheduler_metadata_names.sql'
  )

  assert.equal(mysqlMigration, obMysqlMigration)
  assert.equal(
    mysqlMigration,
    read('installer/upgrade-sql/v2.0.1_to_v2.0.2_metadata_names_mysql.sql')
  )
  assert.equal(
    obMysqlMigration,
    read('installer/upgrade-sql/v2.0.1_to_v2.0.2_metadata_names_ob_mysql.sql')
  )
  assert.equal(
    obOracleMigration,
    read('installer/upgrade-sql/v2.0.1_to_v2.0.2_metadata_names_ob_oracle.sql')
  )

  for (const source of [mysqlMigration, obOracleMigration]) {
    for (const tableName of [
      'core_schedule_job_details',
      'core_schedule_triggers',
      'core_schedule_cron_triggers',
      'core_schedule_simple_triggers',
      'core_schedule_blob_triggers',
      'core_schedule_simprop_triggers',
      'core_schedule_fired_triggers',
      'core_schedule_calendars',
      'core_schedule_paused_trigger_groups',
      'core_schedule_locks',
      'core_schedule_scheduler_state',
      'core_template_category_relation',
      'core_export_download_task'
    ]) {
      assert.match(source, new RegExp(tableName))
    }
    assert.match(source, /scheduler_fire_instance_id/)
  }

  assert.match(read('core/core-backend/src/main/resources/application.yml'), /tablePrefix: core_schedule_/)
  assert.match(read('core/core-backend/src/main/resources/application.yml'), /job-store-type: jdbc/)
  assert.match(
    read('core/core-backend/src/main/java/io/crest/config/QuartzScheduleTableNameDataSource.java'),
    /core_schedule_JOB_DETAILS/
  )
})

test('default crypto key seed is not shipped in public initialization SQL', () => {
  const initializationSqlFiles = [
    'core/core-backend/src/main/resources/db/migration/V1.5.0.1__initial_schema.sql',
    'core/core-backend/src/main/resources/db/migration-ob-mysql/V1.5.0.1__initial_schema.sql',
    'core/core-backend/src/main/resources/db/migration-ob-oracle/V1.5.0.1__initial_schema.sql',
    ...dbTypes.map(dbType => `installer/upgrade-sql/${dbType.fullSchema}`)
  ]

  for (const file of initializationSqlFiles) {
    const source = read(file)
    assert.doesNotMatch(source, /INSERT INTO `?(?:core_)?crypto_key`?/i, file)
    assert.doesNotMatch(source, /1779664249927/, file)
  }

  for (const dbType of dbTypes) {
    const migration = read(
      `core/core-backend/src/main/resources/db/${dbType.migrationDir}/V2.0.2.2__rotate_default_crypto_key.sql`
    )
    const installerSql = read(
      `installer/upgrade-sql/v2.0.1_to_v2.0.2_rotate_default_crypto_key_${dbType.suffix}.sql`
    )

    assert.equal(migration, installerSql)
    assert.match(migration, /DELETE FROM core_crypto_key/)
    assert.match(migration, /create_time = 1779664249927/)
    assert.doesNotMatch(migration, /private_key|public_key|aes_key/i)
  }
})

test('about system build information is exposed and injected by package pipeline', () => {
  const accountOperator = read('core/core-frontend/src/layout/components/AccountOperator.vue')
  const frontendConfig = read('core/core-frontend/config/common.ts')
  const backendConfig = read('core/core-backend/src/main/resources/application.yml')
  const dockerWorkflow = read('.github/workflows/docker-publish.yml')

  assert.match(accountOperator, /system_about/)
  assert.match(accountOperator, /querySystemAbout/)
  assert.match(accountOperator, /__CREST_FRONTEND_VERSION__/)
  assert.match(accountOperator, /__CREST_FRONTEND_COMMIT_ID__/)
  assert.match(frontendConfig, /CREST_FRONTEND_COMMIT_ID/)
  assert.match(frontendConfig, /CREST_FRONTEND_VERSION/)
  assert.match(read('core/core-frontend/src/vite-env.d.ts'), /__CREST_FRONTEND_COMMIT_ID__/)
  assert.match(
    read('core/core-backend/src/main/java/io/crest/system/server/SystemAboutServer.java'),
    /\/system\/about/
  )
  assert.match(backendConfig, /commit-id: \$\{CREST_BACKEND_COMMIT_ID:unknown\}/)
  assert.match(read('Dockerfile.backend'), /ARG CREST_BACKEND_COMMIT_ID=unknown/)
  assert.match(read('Dockerfile.backend'), /ENV CREST_BACKEND_COMMIT_ID=/)
  assert.match(read('Dockerfile.frontend'), /ARG CREST_FRONTEND_COMMIT_ID=unknown/)
  assert.match(dockerWorkflow, /CREST_FRONTEND_COMMIT_ID: \$\{\{ github\.sha \}\}/)
  assert.match(dockerWorkflow, /CREST_BACKEND_COMMIT_ID=\$\{\{ github\.sha \}\}/)
  assert.match(dockerWorkflow, /org\.opencontainers\.image\.revision=\$\{\{ github\.sha \}\}/)
  assert.match(read('docs/release/v2.0.2.md'), /关于系统/)
  assert.match(read('installer/README.md'), /前端 Commit、后端版本和后端 Commit/)
  assert.match(read('installer/make-app-image-package.sh'), /source_commit=\$\{SOURCE_COMMIT\}/)
  assert.match(read('installer/make-offline-package.sh'), /package-manifest\.txt/)
  assert.match(read('installer/make-offline-package.sh'), /source_commit=\$\{SOURCE_COMMIT\}/)
})

test('full schema SQL is generated, complete and documented', () => {
  const checkResult = spawnSync(
    process.execPath,
    ['installer/upgrade-sql/generate-full-schema.mjs', '--check'],
    {
      cwd: root,
      encoding: 'utf8'
    }
  )
  assert.equal(checkResult.status, 0, checkResult.stdout + checkResult.stderr)

  const releaseNotes202 = read('docs/release/v2.0.2.md')
  const upgradeReadme = read('installer/upgrade-sql/README.md')
  const installerReadme = read('installer/README.md')
  const offlinePackage = read('installer/make-offline-package.sh')
  const appImagePackage = read('installer/make-app-image-package.sh')

  for (const dbType of dbTypes) {
    const fullSchemaPath = `installer/upgrade-sql/${dbType.fullSchema}`
    assert.ok(existsSync(path.join(root, fullSchemaPath)), fullSchemaPath)

    const fullSchema = read(fullSchemaPath)
    assert.match(fullSchema, /空库一次性初始化 SQL/)
    assert.match(fullSchema, /仅适用于新建 Crest 系统库/)
    assert.match(fullSchema, /直接使用 v2\.0\.2 最终命名/)
    assert.match(fullSchema, /Source: /)
    assert.match(fullSchema, /core_schedule_triggers/)
    assert.match(fullSchema, /core_template_category_relation/)
    assert.match(fullSchema, /core_export_download_task/)
    assert.match(fullSchema, /scheduler_fire_instance_id/)
    assert.doesNotMatch(fullSchema, /core_core_/)
    assert.doesNotMatch(fullSchema, /core_core_core_/)
    assert.doesNotMatch(fullSchema, /QRTZ_|qrtz_/)
    assert.doesNotMatch(fullSchema, /core_template_category_rel\b/)
    assert.doesNotMatch(fullSchema, /core_export_download\b/)
    assert.doesNotMatch(fullSchema, /dataset_table_join\b/)
    assert.doesNotMatch(fullSchema, /scheduler_instance_id\b/)
    assert.doesNotMatch(fullSchema, /INSERT INTO `?core_crypto_key`?/i)
    assert.doesNotMatch(fullSchema, /\|DE\||-de-/)
    assert.doesNotMatch(
      fullSchema,
      /\bDE_(CAST_DATE_FORMAT|UNIX_TIMESTAMP|STR_TO_DATE|FROM_UNIXTIME|DATE_FORMAT)\b/
    )
    assert.doesNotMatch(
      fullSchema,
      /V2\.0\.(1\.1__rename_metadata_tables|1\.2__normalize_runtime_identifiers|2\.1__normalize_scheduler_metadata_names)/
    )

    for (const source of [
      releaseNotes202,
      upgradeReadme,
      installerReadme,
      offlinePackage,
      appImagePackage
    ]) {
      assert.ok(source.includes(dbType.fullSchema), dbType.fullSchema)
    }
  }
})

test('runtime identifier migrations cover saved parameter and tree configuration', () => {
  const mysqlRuntime = read(
    'core/core-backend/src/main/resources/db/migration/V2.0.1.2__normalize_runtime_identifiers.sql'
  )
  const obMysqlRuntime = read(
    'core/core-backend/src/main/resources/db/migration-ob-mysql/V2.0.1.2__normalize_runtime_identifiers.sql'
  )
  const obOracleRuntime = read(
    'core/core-backend/src/main/resources/db/migration-ob-oracle/V2.0.1.2__normalize_runtime_identifiers.sql'
  )

  assert.equal(mysqlRuntime, obMysqlRuntime)
  assert.equal(
    mysqlRuntime,
    read('installer/upgrade-sql/v2.0.0_to_v2.0.1_runtime_identifiers_mysql.sql')
  )
  assert.equal(
    obMysqlRuntime,
    read('installer/upgrade-sql/v2.0.0_to_v2.0.1_runtime_identifiers_ob_mysql.sql')
  )
  assert.equal(
    obOracleRuntime,
    read('installer/upgrade-sql/v2.0.0_to_v2.0.1_runtime_identifiers_ob_oracle.sql')
  )

  for (const source of [mysqlRuntime, obOracleRuntime]) {
    assert.match(source, /\|DE\|/)
    assert.match(source, /\|DATASET_PARAM\|/)
    assert.match(source, /-de-/)
    assert.match(source, / > /)
    assert.match(source, /DE_DATE_FORMAT/)
    assert.match(source, /CREST_DATE_FORMAT/)
    assert.match(source, /de-rich-text/)
    assert.match(source, /rich-text/)
    assert.match(source, /de-reset-button/)
    assert.match(source, /crest-reset-button/)
    assert.match(source, /de-button/)
    assert.match(source, /crest-button/)
    assert.match(source, /de-template-data/)
    assert.match(source, /crest-template-data/)
    assert.match(source, /de-report-filter-/)
    assert.match(source, /crest-report-filter-/)
    assert.match(source, /de-canvas-/)
    assert.match(source, /crest-canvas-/)
    assert.match(source, /core_dataset_field/)
    assert.match(source, /origin_name/)
    assert.match(source, /params/)
    assert.match(source, /core_visualization_parameter_target/)
    assert.match(source, /core_visualization_parameter_item/)
    assert.match(source, /core_chart_view/)
    assert.match(source, /core_visualization/)
    assert.match(source, /core_template/)
    assert.match(source, /core_template_view_data/)
  }

  assert.match(obOracleRuntime, /DBMS_LOB\.INSTR/)
})

test('installer and release notes list release scripts in execution order', () => {
  const releaseNotes201 = read('docs/release/v2.0.1.md')
  const releaseNotes202 = read('docs/release/v2.0.2.md')
  const upgradeReadme = read('installer/upgrade-sql/README.md')
  const installerReadme = read('installer/README.md')
  const offlinePackage = read('installer/make-offline-package.sh')
  const appImagePackage = read('installer/make-app-image-package.sh')

  for (const dbType of dbTypes) {
    const metadataScript = `v2.0.0_to_v2.0.1_metadata_tables_${dbType.suffix}.sql`
    const runtimeScript = `v2.0.0_to_v2.0.1_runtime_identifiers_${dbType.suffix}.sql`

    for (const source of [
      releaseNotes201,
      upgradeReadme,
      installerReadme,
      offlinePackage,
      appImagePackage
    ]) {
      const metadataIndex = source.indexOf(metadataScript)
      const runtimeIndex = source.indexOf(runtimeScript)
      assert.ok(metadataIndex >= 0, metadataScript)
      assert.ok(runtimeIndex > metadataIndex, runtimeScript)
    }

    const schedulerScript = `v2.0.1_to_v2.0.2_metadata_names_${dbType.suffix}.sql`
    const keyRotationScript = `v2.0.1_to_v2.0.2_rotate_default_crypto_key_${dbType.suffix}.sql`
    for (const source of [releaseNotes202, upgradeReadme, installerReadme, offlinePackage, appImagePackage]) {
      const runtimeIndex = source.indexOf(runtimeScript)
      const schedulerIndex = source.indexOf(schedulerScript)
      const keyRotationIndex = source.indexOf(keyRotationScript)
      assert.ok(schedulerIndex >= 0, schedulerScript)
      assert.ok(keyRotationIndex > schedulerIndex, keyRotationScript)
      if (runtimeIndex >= 0) {
        assert.ok(schedulerIndex > runtimeIndex, schedulerScript)
      }
    }
  }
})

test('frontend helpers use Crest runtime identifiers only', () => {
  assert.match(read('core/core-frontend/src/utils/datasetParameterFieldId.ts'), /\|DATASET_PARAM\|/)
  assert.doesNotMatch(read('core/core-frontend/src/utils/datasetParameterFieldId.ts'), /\|DE\|/)
  assert.match(
    read('core/core-frontend/src/utils/treeLevelSeparator.ts'),
    /TREE_LEVEL_SEPARATOR = ' > '/
  )
  assert.doesNotMatch(read('core/core-frontend/src/utils/treeLevelSeparator.ts'), /-de-/)
})
