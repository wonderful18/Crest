import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { describe, it } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const srcRoot = path.resolve(__dirname, '..')

const readSource = relativePath => fs.readFileSync(path.join(srcRoot, relativePath), 'utf8')

describe('canvas material catalog', () => {
  it('exposes a background card material without changing the rectangle material', () => {
    const componentListSource = readSource('custom-component/component-list.ts')
    const componentConfigSource = readSource('custom-component/common/ComponentConfig.ts')
    const componentsSource = readSource('utils/components.ts')

    assert.match(componentListSource, /component:\s*'BackgroundCard'/)
    assert.match(componentListSource, /name:\s*t\('visualization\.background_card'\)/)
    assert.match(componentListSource, /component:\s*'RectShape'/)
    assert.match(componentConfigSource, /value:\s*'BackgroundCard'/)
    assert.match(componentConfigSource, /value:\s*'RectShape'/)
    assert.match(componentsSource, /BackgroundCard:\s*RectShape/)
    assert.match(componentsSource, /BackgroundCardAttr:\s*RectShapeAttr/)
  })

  it('does not expose raw internal group area labels', () => {
    const componentListSource = readSource('custom-component/component-list.ts')

    assert.doesNotMatch(componentListSource, /name:\s*'group_area'/)
    assert.doesNotMatch(componentListSource, /label:\s*'group_area'/)
    assert.match(componentListSource, /t\('visualization\.group_area'\)/)
  })

  it('uses the rectangle class for the rectangle shape component', () => {
    const rectShapeSource = readSource('custom-component/rect-shape/Component.vue')

    assert.match(rectShapeSource, /class="rect-shape"/)
    assert.doesNotMatch(rectShapeSource, /class="circle-shape"/)
  })

  it('keeps material icon components out of reactive state', () => {
    const commonGroupSource = readSource('custom-component/component-group/CommonGroup.vue')

    assert.match(commonGroupSource, /const groupList = CANVAS_MATERIAL/)
    assert.doesNotMatch(commonGroupSource, /groupList:\s*CANVAS_MATERIAL/)
  })

  it('falls back when S2 auto-wrap table cell metadata is incomplete', () => {
    const tableSource = readSource('views/chart/components/js/panel/common/common_table.ts')

    assert.match(
      tableSource,
      /this\.meta\?\.autoWrap && this\.meta\?\.width && this\.meta\?\.height/
    )
    assert.match(tableSource, /this\.meta\?\.autoWrap && this\.meta\?\.width/)
    assert.match(tableSource, /if \(!cell\?\.meta \|\| !cell\?\.spreadsheet\)/)
    assert.match(tableSource, /const cellMeta = cell\.meta/)
    assert.match(
      tableSource,
      /cellMeta\.height \?\? cell\.getBBoxByType\?\.\(\)\?\.height \?\? textHeight/
    )
    assert.match(
      tableSource,
      /const containerHeight = newChart\.container\?\.cfg\?\.height \?\? newChart\.options\?\.height/
    )
    assert.match(tableSource, /getEmptyPlaceholder\(cell, placeholder\)/)
  })

  it('registers emitter cleanup only when a component instance is active', () => {
    const emitterSource = readSource('hooks/web/useEmitt.ts')

    assert.match(emitterSource, /getCurrentInstance\(\)/)
    assert.match(emitterSource, /if \(getCurrentInstance\(\)\) \{/)
    assert.match(emitterSource, /onBeforeUnmount\(\(\) => \{/)
  })
})
