import test from 'node:test'
import assert from 'node:assert/strict'

import {
  appendSummaryColumnsToS2Config,
  buildSummaryRowWithColumns,
  evalSummaryColumnFormula
} from './tableSummaryColumns.mjs'

test('appendSummaryColumnsToS2Config appends visible virtual columns and empty detail values', () => {
  const dataConfig = {
    fields: { columns: ['project', 'duration'] },
    meta: [{ field: 'project', name: '项目' }],
    data: [{ project: 'A', duration: 4 }]
  }
  const summaryColumns = [
    {
      id: 'summary_avg_duration',
      name: '平均处理时长',
      show: true,
      format: { emptyText: '--' }
    },
    {
      id: 'summary_hidden',
      name: '隐藏汇总',
      show: false
    }
  ]

  appendSummaryColumnsToS2Config(dataConfig, summaryColumns)

  assert.deepEqual(dataConfig.fields.columns, ['project', 'duration', 'summary_avg_duration'])
  assert.deepEqual(dataConfig.meta.at(-1), {
    field: 'summary_avg_duration',
    name: '平均处理时长',
    id: 'summary_avg_duration'
  })
  assert.equal(dataConfig.data[0].summary_avg_duration, '--')
  assert.equal(dataConfig.data[0].summary_hidden, undefined)
})

test('buildSummaryRowWithColumns adds virtual summary results without changing existing column summaries', () => {
  const summaryRow = { SUMMARY: true, duration: 12 }
  const summaryColumns = [
    {
      id: 'summary_avg_duration',
      name: '平均处理时长',
      show: true,
      format: { emptyText: '--' }
    },
    {
      id: 'summary_no_result',
      name: '无结果列',
      show: true,
      format: { emptyText: '--' }
    }
  ]

  const result = buildSummaryRowWithColumns(summaryRow, summaryColumns, {
    summary_avg_duration: '6.50'
  })

  assert.deepEqual(result, {
    SUMMARY: true,
    duration: 12,
    summary_avg_duration: '6.50',
    summary_no_result: '--'
  })
})

test('buildSummaryRowWithColumns formats virtual summary values', () => {
  const result = buildSummaryRowWithColumns(
    { SUMMARY: true },
    [
      {
        id: 'summary_rate',
        name: '完成率',
        show: true,
        format: {
          decimal: 2,
          percent: true,
          thousandSeparator: true,
          unit: ''
        }
      },
      {
        id: 'summary_amount',
        name: '销售额',
        show: true,
        format: {
          decimal: 1,
          percent: false,
          thousandSeparator: true,
          unit: '元'
        }
      }
    ],
    {
      summary_rate: '0.12345',
      summary_amount: '12345.67'
    }
  )

  assert.equal(result.summary_rate, '12.35%')
  assert.equal(result.summary_amount, '12,345.7元')
})

test('evalSummaryColumnFormula resolves [fieldId] refs to row values and evaluates arithmetic', () => {
  const row = { f_dev: 3, f_test: 5, f_accept: 2 }
  const idToEngineField = { id_dev: 'f_dev', id_test: 'f_test', id_accept: 'f_accept' }
  assert.equal(
    evalSummaryColumnFormula('[id_dev]+[id_test]+[id_accept]', row, idToEngineField),
    10
  )
  assert.equal(
    evalSummaryColumnFormula('[id_dev]*[id_test]-[id_accept]', row, idToEngineField),
    13
  )
})

test('evalSummaryColumnFormula strips leading = (Excel-style formula from editor)', () => {
  const row = { f_dev: 3, f_test: 5 }
  const idToEngineField = { id_dev: 'f_dev', id_test: 'f_test' }
  assert.equal(
    evalSummaryColumnFormula('=[id_dev]+[id_test]', row, idToEngineField),
    8
  )
})

test('evalSummaryColumnFormula treats empty/missing field values as 0', () => {
  const row = { f_dev: 3, f_test: null, f_accept: undefined }
  const idToEngineField = { id_dev: 'f_dev', id_test: 'f_test', id_accept: 'f_accept' }
  assert.equal(
    evalSummaryColumnFormula('[id_dev]+[id_test]+[id_accept]', row, idToEngineField),
    3
  )
})

test('evalSummaryColumnFormula returns null for non-arithmetic (SQL function) formulas', () => {
  const row = { f_dev: 3 }
  const idToEngineField = { id_dev: 'f_dev' }
  assert.equal(
    evalSummaryColumnFormula('SUM([id_dev])', row, idToEngineField),
    null
  )
  assert.equal(evalSummaryColumnFormula('', row, idToEngineField), null)
  assert.equal(evalSummaryColumnFormula('1+2', row, idToEngineField), null)
})

test('appendSummaryColumnsToS2Config fills custom formula column per row', () => {
  const dataConfig = {
    fields: { columns: ['project', 'f_dev', 'f_test', 'f_accept'] },
    meta: [{ field: 'project', name: '项目' }],
    data: [
      { project: 'A', f_dev: 3, f_test: 5, f_accept: 2 },
      { project: 'B', f_dev: 1, f_test: 4, f_accept: 0 }
    ]
  }
  const summaryColumns = [
    {
      id: 'summary_full_cycle',
      name: '交付全周期',
      show: true,
      summary: 'custom',
      originName: '[id_dev]+[id_test]+[id_accept]',
      format: { emptyText: '--' }
    }
  ]
  const idToEngineField = { id_dev: 'f_dev', id_test: 'f_test', id_accept: 'f_accept' }

  appendSummaryColumnsToS2Config(dataConfig, summaryColumns, idToEngineField)

  assert.equal(dataConfig.data[0].summary_full_cycle, 10)
  assert.equal(dataConfig.data[1].summary_full_cycle, 5)
})

test('appendSummaryColumnsToS2Config keeps non-custom columns as empty text', () => {
  const dataConfig = {
    fields: { columns: ['project'] },
    meta: [{ field: 'project', name: '项目' }],
    data: [{ project: 'A' }]
  }
  const summaryColumns = [
    {
      id: 'summary_avg_duration',
      name: '平均处理时长',
      show: true,
      summary: 'sum',
      format: { emptyText: '--' }
    }
  ]
  appendSummaryColumnsToS2Config(dataConfig, summaryColumns, {})
  assert.equal(dataConfig.data[0].summary_avg_duration, '--')
})
