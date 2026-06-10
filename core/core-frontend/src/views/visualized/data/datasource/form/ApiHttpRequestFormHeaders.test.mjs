import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { describe, it } from 'node:test'

const source = readFileSync(new URL('./ApiHttpRequestForm.vue', import.meta.url), 'utf8')
const drawerSource = readFileSync(new URL('./ApiHttpRequestDraw.vue', import.meta.url), 'utf8')

describe('API datasource header suggestions', () => {
  it('offers the standard Authorization header', () => {
    assert.match(source, /\{\s*value:\s*'Authorization'\s*\}/)
  })
})

describe('API datasource JsonPath field selection', () => {
  it('keeps field checkboxes editable after JsonPath detects row fields', () => {
    assert.doesNotMatch(drawerSource, /:disabled="apiItem\.useJsonPath"/)
  })
})
