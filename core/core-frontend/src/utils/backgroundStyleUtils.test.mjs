import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'backgroundStyleUtils.ts')

function loadBackgroundStyleUtils() {
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
    if (request === '@/utils/imgUtils') {
      return {
        imgUrlTrans: url => `resolved:${url}`
      }
    }
    if (request === '@/Types') {
      return {
        ShorthandMode: {
          Uniform: 'uniform',
          Axis: 'axis',
          PerEdge: 'per_edge'
        }
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

const { getBlurBgStyle, getComponentBackgroundStyle } = loadBackgroundStyleUtils()

describe('backgroundStyleUtils', () => {
  it('maps uploaded component background images to independent CSS background properties', () => {
    const style = getComponentBackgroundStyle(
      {
        backgroundColorSelect: true,
        backgroundColor: 'rgba(1,2,3,.4)',
        backgroundImageEnable: true,
        backgroundType: 'outerImage',
        outerImage: 'static-resource/bg image.png',
        innerPadding: {
          mode: 'per_edge',
          top: 4,
          right: 6,
          bottom: 8,
          left: 10
        },
        borderRadius: {
          mode: 'per_edge',
          topLeft: 1,
          topRight: 2,
          bottomRight: 3,
          bottomLeft: 4
        }
      },
      { scale: 2 }
    )

    assert.equal(style.padding, '8px 12px 16px 20px')
    assert.equal(style.borderRadius, '2px 4px 6px 8px')
    assert.equal(style.backgroundColor, 'rgba(1,2,3,.4)')
    assert.equal(style.backgroundImage, 'url("resolved:static-resource/bg image.png")')
    assert.equal(style.backgroundRepeat, 'no-repeat')
    assert.equal(style.backgroundPosition, 'center')
    assert.equal(style.backgroundSize, '100% 100%')
    assert.equal(style.background, undefined)
  })

  it('uses the uploaded image on the blur layer without background shorthand', () => {
    const style = getBlurBgStyle(
      {
        backdropFilterEnable: true,
        backdropFilter: 6,
        backgroundImageEnable: true,
        backgroundType: 'outerImage',
        outerImage: 'static-resource/blur.png',
        borderRadius: {
          mode: 'uniform',
          topLeft: 5
        }
      },
      2
    )

    assert.equal(style.backgroundImage, 'url("resolved:static-resource/blur.png")')
    assert.equal(style.backgroundRepeat, 'no-repeat')
    assert.equal(style.backgroundPosition, 'center')
    assert.equal(style.backgroundSize, '100% 100%')
    assert.equal(style.background, undefined)
    assert.equal(style.filter, 'blur(6px)')
    assert.equal(style.borderRadius, '10px')
  })
})
