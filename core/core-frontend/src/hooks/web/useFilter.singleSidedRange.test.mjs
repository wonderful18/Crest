import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'useFilter.ts')

function loadUseFilter() {
  const source = fs.readFileSync(sourcePath, 'utf8')
  const output = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020
    }
  }).outputText

  const mod = new Module(sourcePath)
  mod.filename = sourcePath
  mod.paths = Module._nodeModulePaths(path.dirname(sourcePath))

  const originalLoad = Module._load
  Module._load = (request, parent, isMain) => {
    if (request === '@/store/modules/data-visualization/dvMain') {
      return {
        dvMainStoreWithOut: () => ({})
      }
    }
    if (request === 'pinia') {
      return {
        storeToRefs: () => ({
          componentData: { value: [] },
          canvasStyleData: { value: {} }
        })
      }
    }
    if (request === '@/custom-component/v-query/time-format') {
      return {
        getDynamicRange: () => [],
        getCustomTime: () => 0
      }
    }
    if (request === '@/custom-component/v-query/time-format-dayjs') {
      return {
        getCustomRange: () => []
      }
    }
    if (request === 'dayjs') {
      return {
        __esModule: true,
        default: originalLoad(request, parent, isMain)
      }
    }
    if (request === '@/utils/treeLevelSeparator') {
      return {
        decodeTreeLevelValue: value => String(value || '').split('-de-').join(',')
      }
    }
    return originalLoad(request, parent, isMain)
  }

  try {
    mod._compile(output, sourcePath)
    return mod.exports
  } finally {
    Module._load = originalLoad
  }
}

const { searchQuery } = loadUseFilter()

function buildRangeItem(selectValue) {
  return {
    id: 'filter-1',
    displayType: '7',
    timeType: 'fixed',
    timeGranularity: 'date',
    timeGranularityMultiple: 'daterange',
    selectValue,
    checkedFields: ['chart-1'],
    checkedFieldsMap: {
      'chart-1': 'start-field'
    },
    checkedFieldsMapArr: {
      'chart-1': ['start-field', 'end-field']
    },
    checkedFieldsMapEnd: {
      'chart-1': 'end-field'
    },
    parameters: [],
    parametersArr: {
      'chart-1': [{ id: 'start-field' }, { id: 'end-field' }]
    }
  }
}

function runRangeFilter(selectValue) {
  const filter = []
  searchQuery(
    [
      {
        id: 'query-1',
        propValue: [buildRangeItem(selectValue)]
      }
    ],
    filter,
    'chart-1',
    false
  )
  return filter
}

describe('useFilter single-sided time ranges', () => {
  it('maps start-only range filters to the start field with ge operator', () => {
    const filter = runRangeFilter(['2026-06-30', ''])

    assert.equal(filter.length, 1)
    assert.equal(filter[0].fieldId, 'start-field')
    assert.equal(filter[0].operator, 'ge')
    assert.deepEqual(filter[0].value, [+new Date('2026/06/30 00:00:00')])
  })

  it('maps end-only range filters to the end field with le operator', () => {
    const filter = runRangeFilter(['', '2026-06-30'])

    assert.equal(filter.length, 1)
    assert.equal(filter[0].fieldId, 'end-field')
    assert.equal(filter[0].operator, 'le')
    assert.deepEqual(filter[0].value, [+new Date('2026/06/30 23:59:59')])
  })

  it('keeps two-field range filters as paired between conditions when both sides exist', () => {
    const filter = runRangeFilter(['2026-06-01', '2026-06-30'])

    assert.equal(filter.length, 2)
    assert.equal(filter[0].fieldId, 'end-field')
    assert.equal(filter[0].arrayType, 'END')
    assert.equal(filter[0].operator, 'between')
    assert.equal(filter[1].fieldId, 'start-field')
    assert.equal(filter[1].operator, 'between')
    assert.deepEqual(filter[0].value, [
      +new Date('2026/06/30 23:59:59'),
      +new Date('2026/06/30 23:59:59')
    ])
    assert.deepEqual(filter[1].value, [
      +new Date('2026/06/01 00:00:00'),
      +new Date('2026/06/01 00:00:00')
    ])
  })
})
