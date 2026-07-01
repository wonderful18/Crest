import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  parseLoginRedirect,
  resolveDefaultLoginPath,
  shouldShowLocalLogin,
  shouldUseEmergencyLogin,
  stringifyLoginRedirect
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

  it('falls back to portal for non-local redirect targets', () => {
    for (const redirect of [
      'https://example.com',
      '//example.com/path',
      '%2F%2Fexample.com/path',
      'javascript:alert(1)',
      '/\\example.com'
    ]) {
      assert.deepEqual(parseLoginRedirect(redirect), {
        path: '/portal',
        query: {}
      })
    }
  })

  it('serializes only safe local redirect targets for SSO login', () => {
    assert.equal(stringifyLoginRedirect('/dashboard%3Fid%3D10'), '/dashboard?id=10')
    assert.equal(stringifyLoginRedirect('https://example.com'), '/portal')
  })
})
