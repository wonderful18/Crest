import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { buildMergeCellsInfo, getMergeableFieldOptions } from './tableMergeCells.mjs'

const fields = [
  { engineFieldName: 'region', groupType: 'd' },
  { engineFieldName: 'city', groupType: 'd' },
  { engineFieldName: 'sales', groupType: 'q' }
]

const meta = [{ field: 'region' }, { field: 'city' }, { field: 'sales' }]

const data = [
  { region: '华东', city: '上海', sales: 10 },
  { region: '华东', city: '上海', sales: 20 },
  { region: '华东', city: '杭州', sales: 30 },
  { region: '华南', city: '广州', sales: 40 },
  { region: '华南', city: '广州', sales: 50 }
]

const simplify = cellsInfo =>
  cellsInfo.map(cells =>
    cells.map(cell => ({
      rowIndex: cell.rowIndex,
      colIndex: cell.colIndex,
      showText: cell.showText
    }))
  )

describe('buildMergeCellsInfo', () => {
  it('keeps old configs merging every dimension before the first quota', () => {
    const cellsInfo = buildMergeCellsInfo({ fields, meta, data, showIndex: false })

    assert.deepEqual(simplify(cellsInfo), [
      [
        { rowIndex: 0, colIndex: 0, showText: false },
        { rowIndex: 1, colIndex: 0, showText: true },
        { rowIndex: 2, colIndex: 0, showText: false }
      ],
      [
        { rowIndex: 3, colIndex: 0, showText: true },
        { rowIndex: 4, colIndex: 0, showText: false }
      ],
      [
        { rowIndex: 0, colIndex: 1, showText: true },
        { rowIndex: 1, colIndex: 1, showText: false }
      ],
      [
        { rowIndex: 3, colIndex: 1, showText: true },
        { rowIndex: 4, colIndex: 1, showText: false }
      ]
    ])
  })

  it('merges only selected fields while preserving their table column index', () => {
    const cellsInfo = buildMergeCellsInfo({
      fields,
      meta,
      data,
      showIndex: true,
      mergeFields: ['city']
    })

    assert.deepEqual(simplify(cellsInfo), [
      [
        { rowIndex: 0, colIndex: 0, showText: true },
        { rowIndex: 1, colIndex: 0, showText: false }
      ],
      [
        { rowIndex: 3, colIndex: 0, showText: true },
        { rowIndex: 4, colIndex: 0, showText: false }
      ],
      [
        { rowIndex: 0, colIndex: 2, showText: true },
        { rowIndex: 1, colIndex: 2, showText: false }
      ],
      [
        { rowIndex: 3, colIndex: 2, showText: true },
        { rowIndex: 4, colIndex: 2, showText: false }
      ]
    ])
  })

  it('does not merge anything when the custom field list is empty', () => {
    const cellsInfo = buildMergeCellsInfo({
      fields,
      meta,
      data,
      showIndex: false,
      mergeFields: []
    })

    assert.deepEqual(cellsInfo, [])
  })

  it('offers only dimensions before the first quota as configurable merge fields', () => {
    assert.deepEqual(
      getMergeableFieldOptions([
        { engineFieldName: 'region', chartShowName: '区域', groupType: 'd' },
        { engineFieldName: 'city', name: '城市', groupType: 'd' },
        { engineFieldName: 'sales', name: '销售额', groupType: 'q' },
        { engineFieldName: 'owner', name: '负责人', groupType: 'd' }
      ]),
      [
        { id: 'region', label: '区域' },
        { id: 'city', label: '城市' }
      ]
    )
  })
})
