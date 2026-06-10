import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  parseLoginRedirect,
  resolveDefaultLoginPath,
  shouldShowLocalLogin,
  shouldUseEmergencyLogin
} from './loginEntryPolicy.mjs'

describe('login entry policy', () => {
  it('keeps local account login visible on the normal login entry', () => {
    assert.equal(
      shouldShowLocalLogin(
        {
          enabled: true,
          allowLocalLogin: false
        },
        '/login'
      ),
      true
    )
  })

  it('allows admin emergency login through the normal login entry', () => {
    assert.equal(shouldUseEmergencyLogin('/login'), true)
  })

  it('sends backend users to the workbench when there is no explicit redirect', () => {
    assert.equal(resolveDefaultLoginPath(true), '/workbranch/index')
    assert.equal(resolveDefaultLoginPath(false), '/portal')
  })

  it('parses decoded redirect query strings safely', () => {
    assert.deepEqual(parseLoginRedirect('/dashboard%3Fid%3D10'), {
      path: '/dashboard',
      query: {
        id: '10'
      }
    })
  })
})
