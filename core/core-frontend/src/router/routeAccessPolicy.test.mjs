import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { shouldBypassAuthenticatedMenuGuard } from './routeAccessPolicy.mjs'

describe('route access policy', () => {
  it('allows public share links before authenticated backend menu validation', () => {
    assert.equal(
      shouldBypassAuthenticatedMenuGuard({
        path: '/link/fEm8qSuD',
        name: 'link',
        backendAccess: true
      }),
      true
    )
  })

  it('keeps backend workspace routes in authenticated backend menu validation', () => {
    assert.equal(
      shouldBypassAuthenticatedMenuGuard({
        path: '/workbranch/index',
        name: 'workbranch',
        backendAccess: true
      }),
      false
    )
  })
})
