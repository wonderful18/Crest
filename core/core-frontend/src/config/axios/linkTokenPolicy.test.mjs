import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { shouldAttachLinkToken } from './linkTokenPolicy.mjs'

describe('link token request policy', () => {
  it('does not attach share link tokens to authentication requests', () => {
    assert.equal(shouldAttachLinkToken('/login/refresh'), false)
    assert.equal(shouldAttachLinkToken('/login/local-login'), false)
    assert.equal(shouldAttachLinkToken('public-key'), false)
  })

  it('keeps share link tokens on normal data requests', () => {
    assert.equal(shouldAttachLinkToken('/chart-data/data'), true)
    assert.equal(shouldAttachLinkToken('/dataset-data/field-tree'), true)
  })

  it('only attaches share link tokens while the browser is on a share link route', () => {
    assert.equal(shouldAttachLinkToken('/chart-data/data', '#/link/rxFpxYLa'), true)
    assert.equal(shouldAttachLinkToken('/menu/list', '#/data/dataset'), false)
  })
})
