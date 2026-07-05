import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import Convert from './convert.js'

describe('datasource JSON schema converter', () => {
  it('converts normal nested JSON values', () => {
    const schema = new Convert().format({
      user: { name: 'alice' },
      items: [{ count: 2 }]
    })

    assert.equal(schema.type, 'object')
    assert.equal(schema.properties.user.type, 'object')
    assert.equal(schema.properties.user.properties.name.type, 'string')
    assert.equal(schema.properties.items.type, 'array')
  })

  it('does not recurse forever on cyclic objects', () => {
    const value = { name: 'root' }
    value.self = value

    const schema = new Convert().format(value)

    assert.equal(schema.properties.self.type, 'object')
    assert.equal(schema.properties.self.properties, undefined)
  })

  it('rejects excessively deep JSON structures', () => {
    const value = {}
    let cursor = value
    for (let i = 0; i < 85; i++) {
      cursor.child = {}
      cursor = cursor.child
    }

    assert.throws(() => new Convert().format(value), /depth exceeds limit/)
  })

  it('rejects excessively large JSON structures', () => {
    const value = {}
    for (let i = 0; i < 5001; i++) {
      value[`field${i}`] = i
    }

    assert.throws(() => new Convert().format(value), /node count exceeds limit/)
  })
})
