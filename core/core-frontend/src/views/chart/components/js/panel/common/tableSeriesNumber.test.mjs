import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  TABLE_SERIES_NUMBER_FIELD,
  configureTableSeriesNumber,
  getPageSeriesNumber,
  isTableSeriesNumberCell,
  spanSeriesNumberHeader
} from './tableSeriesNumber.mjs'

describe('table series number helpers', () => {
  it('uses S2 native series number config instead of treating the first data column as index', () => {
    const options = {}

    configureTableSeriesNumber(options, { showIndex: true, indexLabel: '序号' })

    assert.deepEqual(options.seriesNumber, {
      enable: true,
      text: '序号'
    })
    assert.equal(options.showSeriesNumber, true)
  })

  it('recognizes only the S2 series number field as the index cell', () => {
    assert.equal(isTableSeriesNumberCell({ valueField: TABLE_SERIES_NUMBER_FIELD }), true)
    assert.equal(isTableSeriesNumberCell({ valueField: 'launch_cycle', colIndex: 0 }), false)
  })

  it('keeps pagination offsets for custom rendered index values', () => {
    assert.equal(getPageSeriesNumber({ pageSize: 20, currentPage: 3 }, 4), 45)
  })

  it('spans the index header across auxiliary header rows', () => {
    const layoutResult = {
      colsHierarchy: { height: 152 },
      colNodes: [
        { field: TABLE_SERIES_NUMBER_FIELD, y: 0, height: 32 },
        { field: 'month', y: 0, height: 32 },
        { field: 'month', y: 32, height: 120 }
      ]
    }

    spanSeriesNumberHeader(layoutResult)

    assert.equal(layoutResult.colNodes[0].y, 0)
    assert.equal(layoutResult.colNodes[0].height, 152)
    assert.equal(layoutResult.colNodes[1].height, 32)
  })
})
