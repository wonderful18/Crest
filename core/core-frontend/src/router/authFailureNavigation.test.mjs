import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { isLoginPath, resolveAuthFailureNavigation } from './authFailureNavigation.mjs'

describe('auth failure navigation', () => {
  it('allows the normal login route to render after cached auth fails', () => {
    assert.equal(isLoginPath('/login'), true)
    assert.deepEqual(
      resolveAuthFailureNavigation({
        path: '/login',
        fullPath: '/login'
      }),
      {
        type: 'allow'
      }
    )
  })

  it('redirects protected routes back to the normal login entry', () => {
    assert.deepEqual(
      resolveAuthFailureNavigation({
        path: '/workbranch/index',
        fullPath: '/workbranch/index?tab=dataset'
      }),
      {
        type: 'redirect',
        location: {
          path: '/login',
          query: {
            redirect: '/workbranch/index?tab=dataset'
          }
        }
      }
    )
  })
})
