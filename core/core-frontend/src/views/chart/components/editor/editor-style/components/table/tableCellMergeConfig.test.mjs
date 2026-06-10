import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { createTableCellChangePayload } from './tableCellMergeConfig.mjs'

describe('createTableCellChangePayload', () => {
  it('returns a cloned payload so local select edits cannot mutate parent chart config', () => {
    const form = {
      mergeCells: true,
      mergeFields: ['region', 'city'],
      alignConfig: [{ id: 'region', align: 'left' }]
    }

    const payload = createTableCellChangePayload(form, 'mergeFields', [])

    assert.notEqual(payload, form)
    assert.notEqual(payload.mergeFields, form.mergeFields)
    assert.notEqual(payload.alignConfig, form.alignConfig)

    form.mergeFields.pop()
    form.alignConfig[0].align = 'right'

    assert.deepEqual(payload.mergeFields, ['region', 'city'])
    assert.deepEqual(payload.alignConfig, [{ id: 'region', align: 'left' }])
  })

  it('restores all configurable fields when merge is enabled from an empty selection', () => {
    const payload = createTableCellChangePayload(
      {
        mergeCells: true,
        mergeFields: []
      },
      'mergeCells',
      [{ id: 'region' }, { id: 'city' }]
    )

    assert.deepEqual(payload, {
      mergeCells: true,
      mergeFields: ['region', 'city']
    })
  })

  it('turns merge off when the selected merge field list is cleared', () => {
    const payload = createTableCellChangePayload(
      {
        mergeCells: true,
        mergeFields: []
      },
      'mergeFields',
      [{ id: 'region' }]
    )

    assert.deepEqual(payload, {
      mergeCells: false,
      mergeFields: []
    })
  })
})
