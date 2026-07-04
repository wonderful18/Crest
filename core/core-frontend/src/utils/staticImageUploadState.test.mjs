import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { describe, it } from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const srcRoot = path.resolve(__dirname, '..')

const readSource = relativePath => fs.readFileSync(path.join(srcRoot, relativePath), 'utf8')

describe('static image upload state', () => {
  it('replaces uploaded background file lists instead of appending stale images', () => {
    const sources = [
      'components/visualization/component-background/CanvasBackground.vue',
      'components/visualization/component-background/BackgroundOverallCommon.vue',
      'components/visualization/common/CrestUpload.vue'
    ]

    sources.forEach(sourcePath => {
      const source = readSource(sourcePath)

      assert.doesNotMatch(source, /fileList\.push\(/, sourcePath)
      assert.match(source, /syncFileList/, sourcePath)
      assert.match(source, /if \(!fileUrl\)/, sourcePath)
    })
  })

  it('does not keep the obsolete query-component hidden image upload entry', () => {
    const source = readSource('views/chart/components/editor/editor-style/VQueryChartStyle.vue')

    assert.doesNotMatch(source, /@change="reUpload"/)
    assert.doesNotMatch(source, /uploadFileResult/)
  })
})
