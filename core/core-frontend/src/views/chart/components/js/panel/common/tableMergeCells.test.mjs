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
        { rowIndex: 0, colIndex: 2, showText: true },
        { rowIndex: 1, colIndex: 2, showText: false }
      ],
      [
        { rowIndex: 3, colIndex: 2, showText: true },
        { rowIndex: 4, colIndex: 2, showText: false }
      ]
    ])
  })

  it('keeps custom field merges inside preceding dimension context', () => {
    const cellsInfo = buildMergeCellsInfo({
      fields,
      meta,
      data: [
        { region: '华东', city: '上海', sales: 10 },
        { region: '华东', city: '上海', sales: 20 },
        { region: '华南', city: '广州', sales: 30 },
        { region: '华南', city: '广州', sales: 40 }
      ],
      showIndex: true,
      mergeFields: ['city']
    })

    assert.deepEqual(simplify(cellsInfo), [
      [
        { rowIndex: 0, colIndex: 2, showText: true },
        { rowIndex: 1, colIndex: 2, showText: false }
      ],
      [
        { rowIndex: 2, colIndex: 2, showText: true },
        { rowIndex: 3, colIndex: 2, showText: false }
      ]
    ])
  })

  it('merges a custom field across preceding context when its value is identical', () => {
    const cellsInfo = buildMergeCellsInfo({
      fields,
      meta,
      data: [
        { region: '华东', city: '上海', sales: 10 },
        { region: '华东', city: '上海', sales: 20 },
        { region: '华南', city: '上海', sales: 30 },
        { region: '华南', city: '上海', sales: 40 }
      ],
      showIndex: true,
      mergeFields: ['city']
    })

    assert.deepEqual(simplify(cellsInfo), [
      [
        { rowIndex: 0, colIndex: 2, showText: false },
        { rowIndex: 1, colIndex: 2, showText: true },
        { rowIndex: 2, colIndex: 2, showText: false },
        { rowIndex: 3, colIndex: 2, showText: false }
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

  it('merges the last column when it is identical but a preceding column is not', () => {
    const manyFields = Array.from({ length: 5 }, (_, i) => ({
      engineFieldName: `d${i}`,
      groupType: 'd'
    }))
    const manyMeta = Array.from({ length: 5 }, (_, i) => ({ field: `d${i}` }))
    const manyData = [
      { d0: 's', d1: 's', d2: 's', d3: 'P', d4: 's' },
      { d0: 's', d1: 's', d2: 's', d3: 'Q', d4: 's' },
      { d0: 's', d1: 's', d2: 's', d3: 'P', d4: 's' },
      { d0: 's', d1: 's', d2: 's', d3: 'Q', d4: 's' }
    ]

    const cellsInfo = buildMergeCellsInfo({
      fields: manyFields,
      meta: manyMeta,
      data: manyData,
      showIndex: false,
      mergeFields: ['d0', 'd1', 'd2', 'd3', 'd4']
    })

    const lastCol = cellsInfo.filter(cells => cells[0].colIndex === 4)
    assert.deepEqual(simplify(lastCol), [
      [
        { rowIndex: 0, colIndex: 4, showText: false },
        { rowIndex: 1, colIndex: 4, showText: true },
        { rowIndex: 2, colIndex: 4, showText: false },
        { rowIndex: 3, colIndex: 4, showText: false }
      ]
    ])
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
