import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { describe, it } from 'node:test'

const source = readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const apiSource = readFileSync(new URL('../../../../api/datasource.ts', import.meta.url), 'utf8')

describe('datasource snowflake id handling', () => {
  it('keeps datasource detail ids as strings when calling backend detail APIs', () => {
    assert.equal(/validateById\(Number\(/.test(source), false)
    assert.equal(/getById\(Number\(/.test(source), false)
  })

  it('allows datasource detail API helpers to receive string ids', () => {
    assert.match(apiSource, /validateById\s*=\s*\(id:\s*number\s*\|\s*string\)/)
    assert.match(apiSource, /getById\s*=\s*\(id:\s*number\s*\|\s*string\)/)
  })
})
