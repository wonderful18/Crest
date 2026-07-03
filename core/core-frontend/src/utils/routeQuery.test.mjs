import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'routeQuery.ts')

function loadRouteQuery() {
  const source = fs.readFileSync(sourcePath, 'utf8')
  const output = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020
    }
  }).outputText

  const mod = new Module(sourcePath)
  mod.filename = sourcePath
  mod.paths = Module._nodeModulePaths(path.dirname(sourcePath))
  mod._compile(output, sourcePath)
  return mod.exports
}

describe('routeQuery', () => {
  it('falls back to hash query values when router query is empty', () => {
    const { resolveRouteQueryValue } = loadRouteQuery()

    assert.equal(
      resolveRouteQueryValue({}, '#/dvCanvas?dvId=980001&opt=copy&pid=42', 'dvId'),
      '980001'
    )
    assert.equal(
      resolveRouteQueryValue({}, '#/dvCanvas?dvId=980001&opt=copy&pid=42', 'opt'),
      'copy'
    )
    assert.equal(
      resolveRouteQueryValue({}, '#/dvCanvas?dvId=980001&opt=copy&pid=42', 'pid'),
      '42'
    )
  })

  it('prefers router query values and supports array values', () => {
    const { resolveRouteQueryValue } = loadRouteQuery()

    assert.equal(resolveRouteQueryValue({ dvId: ['980002'] }, '#/dvCanvas?dvId=980001', 'dvId'), '980002')
    assert.equal(resolveRouteQueryValue({ opt: 'create' }, '#/dvCanvas?opt=copy', 'opt'), 'create')
  })
})
