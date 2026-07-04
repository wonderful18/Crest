import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { isStaticRichTextRender, sanitizeRichTextHtml } from './richTextStaticRender.mjs'

describe('rich text static rendering', () => {
  it('renders saved rich text directly on preview and share pages', () => {
    assert.equal(
      isStaticRichTextRender({
        showPosition: 'preview',
        disabled: true,
        canEdit: false
      }),
      true
    )
  })

  it('keeps the editor active while editing on the canvas', () => {
    assert.equal(
      isStaticRichTextRender({
        showPosition: 'canvasDataV',
        disabled: false,
        canEdit: true
      }),
      false
    )
  })

  it('keeps rich text formatting while removing executable content', () => {
    const html = sanitizeRichTextHtml(
      '<p style="color:red" onclick="alert(1)">Crest</p><table><tr><td>77</td></tr></table><img src="javascript:alert(1)" onerror="alert(1)"><script>alert(1)</script>'
    )

    assert.match(html, /style="color:red;?"/)
    assert.match(html, /<table>/)
    assert.match(html, /<td>77<\/td>/)
    assert.doesNotMatch(html, /onclick/)
    assert.doesNotMatch(html, /onerror/)
    assert.doesNotMatch(html, /javascript:/)
    assert.doesNotMatch(html, /script/)
  })
})
