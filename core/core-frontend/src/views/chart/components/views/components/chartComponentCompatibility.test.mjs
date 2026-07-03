import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'chartComponentCompatibility.ts')

function loadChartComponentCompatibility() {
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

describe('chart component compatibility', () => {
  it('treats missing imported drill fields as not configured', () => {
    const { hasConfiguredDrillFields } = loadChartComponentCompatibility()

    assert.equal(hasConfiguredDrillFields(null), false)
    assert.equal(hasConfiguredDrillFields({}), false)
    assert.equal(hasConfiguredDrillFields({ drillFields: null }), false)
    assert.equal(hasConfiguredDrillFields({ drillFields: [] }), false)
  })

  it('detects configured drill fields', () => {
    const { hasConfiguredDrillFields } = loadChartComponentCompatibility()

    assert.equal(hasConfiguredDrillFields({ drillFields: [{ id: 'field-1' }] }), true)
  })
})
