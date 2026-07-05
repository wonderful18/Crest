import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'staticResource.ts')

function loadStaticResourceApi() {
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
    if (request === '@/config/axios') {
      return { post: () => Promise.resolve() }
    }
    if (request === '@/views/visualized/data/dataset/form/util.js') {
      return { guid: () => 'test-resource' }
    }
    if (request === 'element-plus-secondary') {
      return { ElMessage: { error: () => {} } }
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

describe('staticResource upload validation', () => {
  it('allows only image formats supported by the backend static-resource service', () => {
    const { isSupportedStaticImage } = loadStaticResourceApi()

    assert.equal(isSupportedStaticImage({ name: 'background.JPG', type: 'image/jpeg' }), true)
    assert.equal(isSupportedStaticImage({ name: 'icon.svg', type: 'image/svg+xml' }), true)
    assert.equal(isSupportedStaticImage({ name: 'legacy.png', type: '' }), true)
    assert.equal(isSupportedStaticImage({ name: 'modern.webp', type: 'image/webp' }), false)
    assert.equal(isSupportedStaticImage({ name: 'photo.heic', type: 'image/heic' }), false)
    assert.equal(isSupportedStaticImage({ name: 'no-extension', type: 'image/png' }), false)
  })
})
