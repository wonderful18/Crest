import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'canvasComponentDefaults.ts')

const lightBackground = {
  backgroundColorSelect: true,
  backgroundColor: 'rgba(255,255,255,1)',
  backgroundImageEnable: false,
  innerPadding: { mode: 'uniform', top: 12 },
  borderRadius: { mode: 'uniform', topLeft: 0 }
}

const darkBackground = {
  backgroundColorSelect: true,
  backgroundColor: 'rgba(19,28,66,1)',
  backgroundImageEnable: false,
  innerPadding: { mode: 'uniform', top: 12 },
  borderRadius: { mode: 'uniform', topLeft: 0 }
}

function loadCanvasComponentDefaults() {
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
    if (request === '@/custom-component/component-list') {
      return {
        COMMON_COMPONENT_BACKGROUND_DARK: darkBackground,
        COMMON_COMPONENT_BACKGROUND_LIGHT: lightBackground
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

describe('normalizeCanvasComponentDefaults', () => {
  it('fills required component frame defaults for legacy imported components', () => {
    const { normalizeCanvasComponentDefaults } = loadCanvasComponentDefaults()
    const component = {
      component: 'UserView',
      style: { width: 600 },
      commonBackground: {
        backgroundImageEnable: true,
        outerImage: '/static-resource/bg.png',
        innerPadding: 8
      }
    }

    normalizeCanvasComponentDefaults(component, 'light')

    assert.equal(component.style.width, 600)
    assert.equal(component.commonBackground.backgroundColor, 'rgba(255,255,255,1)')
    assert.equal(component.commonBackground.backgroundImageEnable, true)
    assert.equal(component.commonBackground.outerImage, '/static-resource/bg.png')
    assert.equal(component.commonBackground.innerPadding, 8)
    assert.deepEqual(component.commonBackground.borderRadius, { mode: 'uniform', topLeft: 0 })
  })

  it('uses independent default objects when style and background are missing', () => {
    const { normalizeCanvasComponentDefaults } = loadCanvasComponentDefaults()
    const first = { component: 'Picture' }
    const second = { component: 'Picture', commonBackground: null }

    normalizeCanvasComponentDefaults(first, 'dark')
    normalizeCanvasComponentDefaults(second, 'dark')
    first.commonBackground.innerPadding.top = 99

    assert.deepEqual(first.style, {})
    assert.equal(first.commonBackground.backgroundColor, 'rgba(19,28,66,1)')
    assert.equal(second.commonBackground.innerPadding.top, 12)
  })

  it('fills required UserView fields for legacy imported charts', () => {
    const { normalizeCanvasComponentDefaults } = loadCanvasComponentDefaults()
    const component = {
      component: 'UserView',
      style: { width: 720 },
      propValue: null,
      matrixStyle: null
    }

    normalizeCanvasComponentDefaults(component, 'dark')

    assert.equal(component.style.width, 720)
    assert.equal(component.style.height, 300)
    assert.equal(component.style.adaptation, 'adaptation')
    assert.deepEqual(component.propValue, { textValue: '', urlList: [] })
    assert.deepEqual(component.matrixStyle, {})
  })
})
