import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'importTemplateFlow.ts')

function loadImportTemplateFlow() {
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

  const originalLoad = Module._load
  Module._load = (request, parent, isMain) => {
    if (request === '@/utils/secureRandom') {
      return {
        secureRandomInt: () => 35
      }
    }
    return originalLoad(request, parent, isMain)
  }

  try {
    mod._compile(output, sourcePath)
    return mod.exports
  } finally {
    Module._load = originalLoad
  }
}

const {
  buildImportedTemplateCacheKey,
  buildImportedTemplateEditorUrl,
  isSupportedVisualizationTemplateFile,
  navigateImportedTemplateEditor,
  resetNativeFileInput
} = loadImportTemplateFlow()

describe('importTemplateFlow', () => {
  it('accepts only DET2 and DET2APP template files case-insensitively', () => {
    assert.equal(isSupportedVisualizationTemplateFile('risk-screen.DET2'), true)
    assert.equal(isSupportedVisualizationTemplateFile('risk-screen.det2app'), true)
    assert.equal(isSupportedVisualizationTemplateFile('risk-screen.json'), false)
    assert.equal(isSupportedVisualizationTemplateFile(''), false)
  })

  it('builds deterministic imported template editor routes', () => {
    const cacheKey = 'CREST-VISUALIZATION-IMPORT-1-ab c'

    assert.equal(
      buildImportedTemplateEditorUrl('dataV', cacheKey),
      '#/dvCanvas?opt=create&createType=uploadedTemplate&templatePayloadKey=CREST-VISUALIZATION-IMPORT-1-ab%20c'
    )
    assert.equal(
      buildImportedTemplateEditorUrl('dashboard', cacheKey),
      '#/dashboard?opt=create&createType=uploadedTemplate&templatePayloadKey=CREST-VISUALIZATION-IMPORT-1-ab%20c'
    )
  })

  it('navigates imported templates in the current page to avoid async popup blockers', () => {
    const calls = []
    const url = navigateImportedTemplateEditor('dataV', 'CACHE KEY', (targetUrl, target) => {
      calls.push([targetUrl, target])
    })

    assert.equal(url, '#/dvCanvas?opt=create&createType=uploadedTemplate&templatePayloadKey=CACHE%20KEY')
    assert.deepEqual(calls, [[url, '_self']])
  })

  it('resets native file input defensively', () => {
    const input = { value: '/tmp/template.det2' }

    resetNativeFileInput(input)
    resetNativeFileInput(null)

    assert.equal(input.value, '')
  })

  it('creates stable cache keys with seven random base36 characters', () => {
    assert.equal(
      buildImportedTemplateCacheKey(() => 1234),
      'CREST-VISUALIZATION-IMPORT-1234-000000z'
    )
  })
})
