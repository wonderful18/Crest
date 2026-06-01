const BLOCK_END_TAG_RE = /<\/(p|div|section|article|header|footer|li|tr|h[1-6])>/gi
const LINE_BREAK_TAG_RE = /<br\s*\/?>/gi
const ENCODED_LINE_BREAK_TAG_RE = /&lt;br\s*\/?&gt;/gi
const SCRIPT_STYLE_RE = /<\s*(script|style)[^>]*>[\s\S]*?<\s*\/\s*\1\s*>/gi
const PRESENTATION_TAG_RE =
  /<\/?(span|strong|b|em|i|u|p|div|section|article|header|footer|li|ul|ol|table|thead|tbody|tr|td|th|a|font|label|small|mark|code|pre)[^>]*>/gi

const entityMap = {
  '&nbsp;': ' ',
  '&#160;': ' ',
  '&ensp;': ' ',
  '&emsp;': '  ',
  '&thinsp;': ' ',
  '&amp;': '&',
  '&quot;': '"',
  '&#34;': '"',
  '&#39;': "'",
  '&apos;': "'"
}

export function normalizeTableCellText(value) {
  if (value === null || value === undefined) {
    return ''
  }

  const text = String(value)
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\n')
    .replace(/&#10;|&#x0a;|&#13;|&#x0d;/gi, '\n')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/&#9;|&#x09;/gi, '  ')
    .replace(/\\t/g, '  ')
    .replace(/\t/g, '  ')
    .replace(ENCODED_LINE_BREAK_TAG_RE, '\n')
    .replace(SCRIPT_STYLE_RE, '')
    .replace(LINE_BREAK_TAG_RE, '\n')
    .replace(BLOCK_END_TAG_RE, '\n')
    .replace(PRESENTATION_TAG_RE, '')
    .replace(/&nbsp;|&#160;|&ensp;|&emsp;|&thinsp;|&amp;|&quot;|&#34;|&#39;|&apos;/gi, match => {
      return entityMap[match.toLowerCase()] ?? match
    })

  return text
    .split('\n')
    .map(line => line.replace(/[ \f\v]+/g, ' ').trim())
    .join('\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

export function getTableCellCopyText(value, options = {}) {
  const text = normalizeTableCellText(value)
  if (options.preserveLineBreaks) {
    return text
  }
  return text.replace(/\n+/g, ' ').replace(/\t/g, '  ')
}

export function buildTableCopyText(rows, headerRows = []) {
  return [...headerRows, ...rows]
    .map(row => row.map(value => getTableCellCopyText(value)).join('\t'))
    .join('\n')
}

export function buildTableCopyHtml(rows, headerRows = []) {
  const header = headerRows
    .map(row => `<tr>${row.map(cell => `<th>${escapeHtml(getTableCellCopyText(cell))}</th>`).join('')}</tr>`)
    .join('')
  const body = rows
    .map(row => `<tr>${row.map(cell => `<td>${escapeHtml(getTableCellCopyText(cell))}</td>`).join('')}</tr>`)
    .join('')
  return `<meta charset="utf-8"><table><tbody>${header}${body}</tbody></table>`
}

export function createTableCopyTransformer(transformers = {}) {
  const normalizeMatrix = dataMatrix =>
    dataMatrix.map(row => row.map(value => getTableCellCopyText(value)))

  return {
    'text/plain': (dataMatrix, separator = '\t') => ({
      type: 'text/plain',
      content: normalizeMatrix(dataMatrix)
        .map(row => row.join(separator))
        .join('\n')
    }),
    'text/html': dataMatrix => {
      const htmlTransformer = transformers['text/html']
      const normalizedMatrix = normalizeMatrix(dataMatrix)
      if (htmlTransformer) {
        return htmlTransformer(normalizedMatrix)
      }
      return {
        type: 'text/html',
        content: buildTableCopyHtml(normalizedMatrix)
      }
    }
  }
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
