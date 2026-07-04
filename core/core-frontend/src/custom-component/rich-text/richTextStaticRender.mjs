import xss from 'xss'

const { FilterXSS, getDefaultWhiteList } = xss

const EDITABLE_POSITIONS = new Set(['canvas', 'canvasDataV', 'edit'])
const COMMON_RICH_TEXT_ATTRS = ['class', 'style', 'title']

const richTextWhiteList = Object.fromEntries(
  Object.entries(getDefaultWhiteList()).map(([tag, attrs]) => [
    tag,
    Array.from(new Set([...attrs, ...COMMON_RICH_TEXT_ATTRS]))
  ])
)

richTextWhiteList.a = Array.from(new Set([...(richTextWhiteList.a || []), 'rel']))
richTextWhiteList.source = ['src', 'type']

const richTextFilter = new FilterXSS({
  whiteList: richTextWhiteList,
  stripIgnoreTag: true,
  stripIgnoreTagBody: ['script', 'style', 'iframe', 'object', 'embed']
})

export function isStaticRichTextRender({ showPosition, disabled, canEdit }) {
  return !canEdit && (disabled || !EDITABLE_POSITIONS.has(showPosition))
}

export function sanitizeRichTextHtml(value) {
  return richTextFilter.process(value || '')
}
