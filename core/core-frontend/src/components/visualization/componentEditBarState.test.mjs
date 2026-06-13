import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { canShowDownloadAction } from './componentEditBarState.mjs'

describe('component edit bar download visibility', () => {
  it('hides download for template data', () => {
    assert.equal(canShowDownloadAction('template'), false)
  })

  it('keeps download available for exportable popup views', () => {
    assert.equal(canShowDownloadAction('dataset'), true)
  })
})
