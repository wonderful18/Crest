import test from 'node:test'
import assert from 'node:assert/strict'

import {
  appendSummaryColumnsToS2Config,
  buildSummaryRowWithColumns
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
