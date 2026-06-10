import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { describe, it } from 'node:test'

const source = readFileSync(new URL('./ApiHttpRequestForm.vue', import.meta.url), 'utf8')

describe('API datasource header suggestions', () => {
  it('offers the standard Authorization header', () => {
    assert.match(source, /\{\s*value:\s*'Authorization'\s*\}/)
  })
})
