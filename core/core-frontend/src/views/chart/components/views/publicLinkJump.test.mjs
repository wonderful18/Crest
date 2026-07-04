import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { buildPublicLinkTargetUrl, isPublicLinkHash } from './publicLinkJump.mjs'

describe('public link jump url', () => {
  it('detects public link routes', () => {
    assert.equal(isPublicLinkHash('#/link/shareDemo01'), true)
    assert.equal(isPublicLinkHash('#/preview?dvId=1'), false)
  })

  it('keeps share entry and opens target visualization from the same public link', () => {
    const url = buildPublicLinkTargetUrl({
      hash: '#/link/shareDemo01?ticket=abc',
      targetDvId: '9000000001',
      targetDvType: 'dataV',
      jumpInfoParam: '&jumpInfoParam=xxx'
    })

    assert.equal(
      url,
      '#/link/shareDemo01?ticket=abc&targetDvId=9000000001&targetDvType=dataV&fromLink=true&ignoreParams=true&jumpInfoParam=xxx'
    )
  })

  it('keeps attached outer params when present', () => {
    const url = buildPublicLinkTargetUrl({
      hash: '#/link/shareDemo01',
      targetDvId: '9000000002',
      targetDvType: 'dashboard',
      attachParamsInfo: '&attachParams=yyy',
      jumpInfoParam: '&jumpInfoParam=xxx'
    })

    assert.equal(
      url,
      '#/link/shareDemo01?targetDvId=9000000002&targetDvType=dashboard&fromLink=true&attachParams=yyy&jumpInfoParam=xxx'
    )
  })

  it('opens target public link directly when the target visualization has one', () => {
    const url = buildPublicLinkTargetUrl({
      hash: '#/link/shareDemo01?ticket=source-ticket',
      publicJumpId: 'targetShare02',
      targetDvId: '9000000002',
      targetDvType: 'dataV'
    })

    assert.equal(
      url,
      '#/link/targetShare02?targetDvId=9000000002&targetDvType=dataV&fromLink=true&ignoreParams=true'
    )
  })
})
