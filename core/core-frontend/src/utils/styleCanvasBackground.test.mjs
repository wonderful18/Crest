import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'style.ts')

const storeState = {
  dvInfo: {
    type: 'dataV'
  },
  mobileInPc: false
}

function loadStyleUtils() {
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
    if (request === '@/utils/translate') {
      return {
        sin: () => 0,
        cos: () => 1,
        toPercent: value => `${value * 100}%`
      }
    }
    if (request === '@/utils/imgUtils') {
      return {
        imgUrlTrans: url => `resolved:${url}`
      }
    }
    if (request === '@/views/chart/components/js/util') {
      return {
        hexColorToRGBA: color => color
      }
    }
    if (request === '@/store/modules/data-visualization/dvMain') {
      return {
        dvMainStoreWithOut: () => storeState
      }
    }
    if (request === '@/utils/canvasUtils') {
      return {
        isMainCanvas: canvasId => canvasId === 'canvas-main',
        isTabCanvas: () => false
      }
    }
    if (request === '@/utils/backgroundStyleUtils') {
      return {
        setBackgroundImageStyle: (style, url) => {
          style.backgroundImage = `url("resolved:${url}")`
          style.backgroundRepeat = 'no-repeat'
          style.backgroundPosition = 'center'
          style.backgroundSize = '100% 100%'
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

const { getCanvasStyle } = loadStyleUtils()

describe('getCanvasStyle background images', () => {
  it('maps large-screen uploaded background images to independent CSS properties', () => {
    storeState.dvInfo.type = 'dataV'
    storeState.mobileInPc = false

    const style = getCanvasStyle({
      backgroundColorSelect: true,
      backgroundColor: 'rgba(3,8,20,1)',
      backgroundImageEnable: true,
      background: 'static-resource/canvas bg.png',
      fontSize: 14,
      color: '#fff',
      fontFamily: 'PingFang SC'
    })

    assert.equal(style.backgroundColor, 'rgba(3,8,20,1)')
    assert.equal(style.backgroundImage, 'url("resolved:static-resource/canvas bg.png")')
    assert.equal(style.backgroundRepeat, 'no-repeat')
    assert.equal(style.backgroundPosition, 'center')
    assert.equal(style.backgroundSize, '100% 100%')
    assert.equal(style.background, undefined)
  })
})
