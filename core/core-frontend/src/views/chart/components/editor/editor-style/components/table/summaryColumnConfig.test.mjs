import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildSummaryColumnFieldOptions,
  normalizeSummaryColumn,
  validateSummaryColumn,
  SUMMARY_COLUMN_VALIDATION
} from './summaryColumnConfig.mjs'

test('normalizes summary column defaults', () => {
  const column = normalizeSummaryColumn(
    {
      id: 'summary_amount',
      name: '',
      format: {
        decimal: 1
      }
    },
    {
      defaultName: '汇总指标列',
      idFactory: () => 'generated'
    }
  )

  assert.equal(column.id, 'summary_amount')
  assert.equal(column.name, '汇总指标列')
  assert.equal(column.show, true)
  assert.equal(column.summary, 'sum')
  assert.deepEqual(column.format, {
    decimal: 1,
    unit: '',
    percent: false,
    thousandSeparator: true,
    emptyText: '--'
  })
})

test('non-custom summary column requires a metric field', () => {
  const result = validateSummaryColumn({
    summary: 'sum',
    field: '',
    originName: ''
  })

  assert.equal(result.valid, false)
  assert.equal(result.reason, SUMMARY_COLUMN_VALIDATION.FIELD_REQUIRED)
})

test('normalizes legacy formula summary column as custom', () => {
  const column = normalizeSummaryColumn({
    summary: 'sum',
    field: 'duration',
    originName: 'SUM([duration]) / COUNT([duration])'
  })

  assert.equal(column.summary, 'custom')
})

test('custom summary column requires a formula expression', () => {
  const result = validateSummaryColumn({
    summary: 'custom',
    field: 'duration',
    originName: ''
  })

  assert.equal(result.valid, false)
  assert.equal(result.reason, SUMMARY_COLUMN_VALIDATION.FORMULA_REQUIRED)
})

test('custom summary column accepts a formula expression', () => {
  const result = validateSummaryColumn({
    summary: 'custom',
    field: 'duration',
    originName: '[duration] / [count]'
  })

  assert.equal(result.valid, true)
})

test('non-count summary columns only expose numeric fields', () => {
  const fields = [
    { engineFieldName: 'f_3', name: '项目名称', fieldType: 0 },
    { engineFieldName: 'f_9', name: '投入工时', fieldType: 2 }
  ]

  const options = buildSummaryColumnFieldOptions(fields)

  assert.deepEqual(
    options.map(field => field.engineFieldName),
    ['f_9']
  )
})

test('count summary columns can expose dimension fields for legacy selections', () => {
  const fields = [
    { engineFieldName: 'f_3', name: '项目名称', fieldType: 0 },
    { engineFieldName: 'f_9', name: '投入工时', fieldType: 2 }
  ]

  const options = buildSummaryColumnFieldOptions(fields, { includeNonNumeric: true })

  assert.deepEqual(
    options.map(field => `${field.engineFieldName}:${field.name}`),
    ['f_3:项目名称', 'f_9:投入工时']
  )
})
