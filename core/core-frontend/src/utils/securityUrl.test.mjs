import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'securityUrl.ts')

function loadSecurityUrl() {
  const source = fs
    .readFileSync(sourcePath, 'utf8')
    .replace('import.meta.env.VITE_EMBEDDED_ORIGIN_LIST', "''")
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

describe('securityUrl', () => {
  it('keeps back navigation on the current origin', () => {
    const { safeSameOriginUrl } = loadSecurityUrl()

    assert.equal(
      safeSameOriginUrl('https://crest.example.com/#/screen/index?dvId=1', 'https://crest.example.com'),
      '/#/screen/index?dvId=1'
    )
    assert.equal(safeSameOriginUrl('https://evil.example.com/phish', 'https://crest.example.com'), '/')
    assert.equal(safeSameOriginUrl('javascript:alert(1)', 'https://crest.example.com'), '/')
  })

  it('sends embedded messages only to same origin or configured origins', () => {
    const { resolvePostMessageTargetOrigin } = loadSecurityUrl()

    assert.equal(
      resolvePostMessageTargetOrigin('https://crest.example.com/page', 'https://crest.example.com'),
      'https://crest.example.com'
    )
    assert.equal(
      resolvePostMessageTargetOrigin('https://evil.example.com/page', 'https://crest.example.com'),
      'https://crest.example.com'
    )
    assert.equal(
      resolvePostMessageTargetOrigin(
        'https://portal.example.com/report',
        'https://crest.example.com',
        'https://portal.example.com, https://ops.example.com'
      ),
      'https://portal.example.com'
    )
  })
})
