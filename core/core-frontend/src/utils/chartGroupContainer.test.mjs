import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'chartGroupContainer.ts')

function loadChartGroupContainer() {
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

describe('chartGroupContainer', () => {
  it('fills chart group defaults without overwriting existing background choices', () => {
    const { normalizeChartGroupContainer } = loadChartGroupContainer()
    const group = {
      component: 'Group',
      chartGroupContainer: {
        layoutMode: 'free'
      },
      commonBackground: {
        backgroundColorSelect: true,
        backgroundColor: 'rgba(1,2,3,1)'
      },
      propValue: []
    }

    normalizeChartGroupContainer(group)

    assert.equal(group.chartGroupContainer.clipOverflow, true)
    assert.equal(group.chartGroupContainer.seamless, false)
    assert.equal(group.chartGroupContainer.layoutMode, 'keep')
    assert.equal(group.commonBackground.backgroundColor, 'rgba(1,2,3,1)')
    assert.deepEqual(group.commonBackground.innerPadding, {
      mode: 'uniform',
      top: 0,
      right: 0,
      bottom: 0,
      left: 0
    })
  })

  it('applies seamless layout only to the selected group and its children', () => {
    const { applyChartGroupSeamlessLayout } = loadChartGroupContainer()
    const group = {
      component: 'Group',
      style: { borderActive: true, borderWidth: 2 },
      commonBackground: {
        backgroundColorSelect: true,
        backgroundColor: 'rgba(19,28,66,1)',
        backgroundImageEnable: false,
        innerPadding: { mode: 'uniform', top: 8 }
      },
      propValue: [
        {
          component: 'UserView',
          style: { borderActive: true, borderWidth: 1, borderRadius: 6 },
          commonBackground: {
            backgroundColorSelect: true,
            backgroundColor: 'rgba(255,255,255,1)',
            backgroundImageEnable: true,
            outerImage: 'static-resource/chart-bg.png',
            innerPadding: { mode: 'uniform', top: 12 },
            borderRadius: { mode: 'uniform', topLeft: 4 }
          }
        },
        {
          component: 'Picture',
          style: { borderActive: true, borderWidth: 1 },
          commonBackground: {
            backgroundColorSelect: true,
            backgroundImageEnable: true
          }
        }
      ]
    }
    const unrelated = {
      component: 'UserView',
      style: { borderActive: true },
      commonBackground: { backgroundColorSelect: true }
    }

    applyChartGroupSeamlessLayout(group)

    assert.equal(group.chartGroupContainer.seamless, true)
    assert.equal(group.style.borderActive, true)
    assert.equal(group.commonBackground.backgroundColorSelect, true)
    assert.equal(group.commonBackground.backgroundColor, 'rgba(19,28,66,1)')
    assert.deepEqual(group.commonBackground.innerPadding, {
      mode: 'uniform',
      top: 0,
      right: 0,
      bottom: 0,
      left: 0
    })
    assert.equal(group.propValue[0].style.borderActive, false)
    assert.equal(group.propValue[0].style.borderWidth, 0)
    assert.equal(group.propValue[0].style.borderRadius, 0)
    assert.equal(group.propValue[0].commonBackground.backgroundColorSelect, false)
    assert.equal(group.propValue[0].commonBackground.backgroundImageEnable, false)
    assert.deepEqual(group.propValue[0].commonBackground.borderRadius, {
      mode: 'uniform',
      topLeft: 0,
      topRight: 0,
      bottomRight: 0,
      bottomLeft: 0
    })
    assert.deepEqual(unrelated.commonBackground, { backgroundColorSelect: true })
  })

  it('removes direct child gaps along the dominant layout axis', () => {
    const { applyChartGroupSeamlessLayout } = loadChartGroupContainer()
    const group = {
      component: 'Group',
      style: { width: 500, height: 120 },
      commonBackground: {},
      propValue: [
        {
          component: 'UserView',
          style: { left: 0, top: 0, width: 200, height: 120 },
          groupStyle: {}
        },
        {
          component: 'UserView',
          style: { left: 220, top: 0, width: 280, height: 120 },
          groupStyle: {}
        }
      ]
    }

    applyChartGroupSeamlessLayout(group)

    assert.equal(group.propValue[1].style.left, 200)
    assert.deepEqual(group.propValue[1].groupStyle, {
      left: 0.4,
      top: 0,
      width: 0.56,
      height: 1
    })
  })

  it('keeps grid layouts in place when applying seamless styling', () => {
    const { applyChartGroupSeamlessLayout } = loadChartGroupContainer()
    const group = {
      component: 'Group',
      style: { width: 420, height: 240 },
      commonBackground: {},
      propValue: [
        { component: 'UserView', style: { left: 0, top: 0, width: 200, height: 100 } },
        { component: 'UserView', style: { left: 220, top: 0, width: 200, height: 100 } },
        { component: 'UserView', style: { left: 0, top: 120, width: 200, height: 100 } },
        { component: 'UserView', style: { left: 220, top: 120, width: 200, height: 100 } }
      ]
    }

    applyChartGroupSeamlessLayout(group)

    assert.deepEqual(
      group.propValue.map(component => [component.style.left, component.style.top]),
      [
        [0, 0],
        [220, 0],
        [0, 120],
        [220, 120]
      ]
    )
  })

  it('does not compact child positions when a direct child has incomplete geometry', () => {
    const { applyChartGroupSeamlessLayout } = loadChartGroupContainer()
    const group = {
      component: 'Group',
      style: { width: 500, height: 120 },
      commonBackground: {},
      propValue: [
        { component: 'UserView', style: { left: 0, top: 0, width: 200, height: 120 } },
        { component: 'UserView', style: { left: 220, top: 0, width: 280, height: 120 } },
        { component: 'UserView', style: { left: 500, top: 0 } }
      ]
    }

    applyChartGroupSeamlessLayout(group)

    assert.equal(group.propValue[1].style.left, 220)
  })

  it('finds the parent chart group for a selected child component', () => {
    const { findChartGroupParent } = loadChartGroupContainer()
    const child = { id: 'chart-1', component: 'UserView' }
    const group = {
      id: 'group-1',
      component: 'Group',
      name: '组合',
      propValue: [child]
    }

    const result = findChartGroupParent([group], 'chart-1')

    assert.equal(result.group, group)
    assert.equal(result.component, child)
    assert.equal(result.groupIndex, 0)
    assert.equal(result.componentIndex, 0)
  })

  it('calculates a stable group boundary for one or more components', () => {
    const { calculateChartGroupBounds } = loadChartGroupContainer()

    assert.deepEqual(
      calculateChartGroupBounds([{ style: { left: 20, top: 30, width: 100, height: 80 } }]),
      { left: 20, top: 30, width: 100, height: 80 }
    )
    assert.deepEqual(
      calculateChartGroupBounds([
        { style: { left: 20, top: 30, width: 100, height: 80 } },
        { style: { left: 150, top: 50, width: 60, height: 70 } }
      ]),
      { left: 20, top: 30, width: 190, height: 90 }
    )
  })

  it('applies explicit horizontal and vertical layout modes with configured gaps', () => {
    const { applyChartGroupLayout } = loadChartGroupContainer()
    const group = {
      component: 'Group',
      style: { width: 400, height: 200 },
      commonBackground: {},
      propValue: [
        { id: 'b', style: { left: 140, top: 20, width: 80, height: 60 }, groupStyle: {} },
        { id: 'a', style: { left: 10, top: 20, width: 100, height: 60 }, groupStyle: {} }
      ]
    }

    assert.equal(applyChartGroupLayout(group, 'horizontal', { horizontalGap: 12 }), true)
    assert.deepEqual(
      group.propValue.map(component => [component.id, component.style.left]),
      [
        ['b', 112],
        ['a', 0]
      ]
    )

    group.propValue[0].style.top = 80
    group.propValue[1].style.top = 0
    assert.equal(applyChartGroupLayout(group, 'vertical', { verticalGap: 8 }), true)
    assert.deepEqual(
      group.propValue.map(component => [component.id, component.style.top]),
      [
        ['b', 68],
        ['a', 0]
      ]
    )
  })

  it('applies grid layout only when rows and columns can be identified', () => {
    const { applyChartGroupLayout } = loadChartGroupContainer()
    const group = {
      component: 'Group',
      style: { width: 420, height: 240 },
      commonBackground: {},
      propValue: [
        { id: 'a', style: { left: 0, top: 0, width: 100, height: 50 }, groupStyle: {} },
        { id: 'b', style: { left: 130, top: 0, width: 100, height: 50 }, groupStyle: {} },
        { id: 'c', style: { left: 0, top: 90, width: 100, height: 50 }, groupStyle: {} },
        { id: 'd', style: { left: 130, top: 90, width: 100, height: 50 }, groupStyle: {} }
      ]
    }

    assert.equal(applyChartGroupLayout(group, 'grid', { horizontalGap: 10, verticalGap: 12 }), true)
    assert.deepEqual(
      group.propValue.map(component => [component.id, component.style.left, component.style.top]),
      [
        ['a', 0, 0],
        ['b', 110, 0],
        ['c', 0, 62],
        ['d', 110, 62]
      ]
    )

    group.propValue.pop()
    assert.equal(applyChartGroupLayout(group, 'grid', { horizontalGap: 10, verticalGap: 12 }), false)
    assert.equal(group.chartGroupContainer.layoutMode, 'grid')
  })

  it('does not persist unsupported layout modes when layout application fails', () => {
    const { applyChartGroupLayout } = loadChartGroupContainer()
    const group = {
      component: 'Group',
      chartGroupContainer: {
        layoutMode: 'horizontal',
        horizontalGap: 4,
        verticalGap: 0
      },
      style: { width: 220, height: 60 },
      commonBackground: {},
      propValue: [
        { id: 'a', style: { left: 0, top: 0, width: 100, height: 50 }, groupStyle: {} },
        { id: 'b', style: { left: 120, top: 0, width: 100, height: 50 }, groupStyle: {} }
      ]
    }

    assert.equal(applyChartGroupLayout(group, 'grid', { horizontalGap: 10, verticalGap: 12 }), false)
    assert.deepEqual(group.chartGroupContainer, {
      clipOverflow: true,
      seamless: false,
      layoutMode: 'horizontal',
      horizontalGap: 4,
      verticalGap: 0
    })
  })
})
