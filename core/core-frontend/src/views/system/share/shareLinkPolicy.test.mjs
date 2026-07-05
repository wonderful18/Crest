import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'
import { describe, it } from 'node:test'

const currentDir = dirname(fileURLToPath(import.meta.url))
const systemShareSource = readFileSync(join(currentDir, 'index.vue'), 'utf8')
const shareRoot = join(currentDir, '../../share/share')
const shareHandlerSource = readFileSync(join(shareRoot, 'ShareHandler.vue'), 'utf8')
const shareVisualHeadSource = readFileSync(join(shareRoot, 'ShareVisualHead.vue'), 'utf8')

describe('system share link policy', () => {
  it('does not copy share passwords into public URLs', () => {
    assert.equal(systemShareSource.includes('current.pwd ? `${shareLink.value},${current.pwd}` : shareLink.value'), false)
    assert.equal(shareHandlerSource.includes('info += `,${state.detailInfo.pwd}`'), false)
    assert.equal(shareVisualHeadSource.includes('info += `,${state.detailInfo.pwd}`'), false)
    assert.match(systemShareSource, /toClipboard\(\s*shareLink\.value\s*\)/)
  })
})
