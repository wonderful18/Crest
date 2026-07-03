import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'canvasChartColorDefaults.ts')

const defaultColorCase = {
  basicStyle: {
    colors: ['#1E90FF', '#90EE90'],
    alpha: 100,
    gradient: false,
    tableBorderColor: '#E6E7E4',
    tableScrollBarColor: 'rgba(0, 0, 0, 0.15)'
  },
  misc: {
    flowMapConfig: {
      lineConfig: {
        mapLineAnimate: true,
        mapLineGradient: false,
        mapLineSourceColor: '#1E90FF',
        mapLineTargetColor: '#90EE90'
      }
    },
    nameFontColor: '#000000',
    valueFontColor: '#5470c6'
  },
  tableHeader: {
    tableHeaderBgColor: '#1E90FF',
    tableHeaderFontColor: '#000000'
  },
  tableCell: {
    tableItemBgColor: '#FFFFFF',
    tableFontColor: '#000000',
    tableItemSubBgColor: '#EEEEEE'
  },
  label: {
    color: '#000000',
    fontSize: 12
  },
  tooltip: {
    color: '#000000',
    fontSize: 12,
    backgroundColor: '#FFFFFF'
  }
}

function loadCanvasChartColorDefaults() {
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
    if (request === '@/views/chart/components/editor/util/chart') {
      return {
        DEFAULT_COLOR_CASE: defaultColorCase
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

describe('normalizeCanvasChartColor', () => {
  it('fills missing nested chart color defaults and preserves existing values', () => {
    const { normalizeCanvasChartColor } = loadCanvasChartColorDefaults()
    const legacyChartColor = {
      basicStyle: {
        alpha: 60,
        colors: ['#123456']
      },
      tableCell: {
        tableFontColor: '#222222'
      },
      misc: {
        flowMapConfig: {
          lineConfig: {
            mapLineAnimate: false
          }
        }
      }
    }

    const normalized = normalizeCanvasChartColor(legacyChartColor)

    assert.equal(normalized.basicStyle.alpha, 60)
    assert.deepEqual(normalized.basicStyle.colors, ['#123456'])
    assert.equal(normalized.basicStyle.gradient, false)
    assert.equal(normalized.tableHeader.tableHeaderBgColor, '#1E90FF')
    assert.equal(normalized.tableCell.tableFontColor, '#222222')
    assert.equal(normalized.tableCell.tableItemBgColor, '#FFFFFF')
    assert.equal(normalized.label.fontSize, 12)
    assert.equal(normalized.tooltip.backgroundColor, '#FFFFFF')
    assert.equal(normalized.misc.flowMapConfig.lineConfig.mapLineAnimate, false)
    assert.equal(normalized.misc.flowMapConfig.lineConfig.mapLineGradient, false)
    assert.notEqual(normalized.basicStyle, legacyChartColor.basicStyle)
  })

  it('uses defaults for missing or invalid required sections', () => {
    const { normalizeCanvasChartColor } = loadCanvasChartColorDefaults()

    const normalized = normalizeCanvasChartColor({
      basicStyle: null,
      tableHeader: undefined,
      tableCell: 'invalid'
    })

    assert.equal(normalized.basicStyle.gradient, false)
    assert.equal(normalized.tableHeader.tableHeaderFontColor, '#000000')
    assert.equal(normalized.tableCell.tableItemSubBgColor, '#EEEEEE')
    assert.equal(normalized.tooltip.color, '#000000')
  })
})
