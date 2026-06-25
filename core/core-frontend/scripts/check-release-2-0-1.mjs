import assert from 'node:assert/strict'
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
  /de-dialog-form/
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
  { name: 'mysql', migrationDir: 'migration', suffix: 'mysql' },
  { name: 'ob-mysql', migrationDir: 'migration-ob-mysql', suffix: 'ob_mysql' },
  { name: 'ob-oracle', migrationDir: 'migration-ob-oracle', suffix: 'ob_oracle' }
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

test('project version is 2.0.1 across release metadata', () => {
  assert.equal(read('VERSION').trim(), '2.0.1')
  assert.match(read('core/core-frontend/package.json'), /"version": "2\.0\.1"/)

  for (const file of versionPomFiles) {
    const source = read(file)
    assert.doesNotMatch(source, /<version>2\.0\.0<\/version>/, file)
  }

  assert.match(read('pom.xml'), /<version>2\.0\.1<\/version>/)
  assert.match(read('pom.xml'), /<crest\.version>2\.0\.1<\/crest\.version>/)
})

test('v2.0.1 migration scripts are present for every supported system database', () => {
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

test('installer and release notes list v2.0.1 scripts in execution order', () => {
  const releaseNotes = read('docs/release/v2.0.1.md')
  const upgradeReadme = read('installer/upgrade-sql/README.md')
  const installerReadme = read('installer/README.md')
  const offlinePackage = read('installer/make-offline-package.sh')
  const appImagePackage = read('installer/make-app-image-package.sh')

  for (const dbType of dbTypes) {
    const metadataScript = `v2.0.0_to_v2.0.1_metadata_tables_${dbType.suffix}.sql`
    const runtimeScript = `v2.0.0_to_v2.0.1_runtime_identifiers_${dbType.suffix}.sql`

    for (const source of [
      releaseNotes,
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
