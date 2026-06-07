const BLOCK_END_TAG_RE = /<\/(p|div|section|article|header|footer|li|tr|h[1-6])>/gi
const LINE_BREAK_TAG_RE = /<br\s*\/?>/gi
const ENCODED_LINE_BREAK_TAG_RE = /&lt;br\s*\/?&gt;/gi
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

  const text = normalizeActualNewlines(decodeEscapedWhitespace(stripDangerousHtml(String(value))))
    .replace(/&#10;|&#x0a;|&#13;|&#x0d;/gi, '\n')
    .replace(/&#9;|&#x09;/gi, '  ')
    .replace(ENCODED_LINE_BREAK_TAG_RE, '\n')
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

function decodeEscapedWhitespace(value) {
  let result = ''
  for (let index = 0; index < value.length; index += 1) {
    const current = value[index]
    const next = value[index + 1]
    if (current !== '\\' || next === undefined) {
      result += current
      continue
    }

    if (next === 'r') {
      result += '\n'
      index += value[index + 2] === '\\' && value[index + 3] === 'n' ? 3 : 1
      continue
    }
    if (next === 'n') {
      result += '\n'
      index += 1
      continue
    }
    if (next === 't') {
      result += '  '
      index += 1
      continue
    }

    result += current
  }
  return result
}

function normalizeActualNewlines(value) {
  let result = ''
  for (let index = 0; index < value.length; index += 1) {
    const current = value[index]
    if (current === '\r') {
      result += '\n'
      if (value[index + 1] === '\n') {
        index += 1
      }
      continue
    }
    result += current === '\t' ? '  ' : current
  }
  return result
}

function stripDangerousHtml(value) {
  let result = value
  for (const tagName of ['script', 'style', 'iframe']) {
    result = stripDangerousTag(result, tagName)
  }
  return result
}

function stripDangerousTag(value, tagName) {
  let result = ''
  let searchFrom = 0
  const lowerValue = value.toLowerCase()
  const openTag = `<${tagName}`
  const closeTagStart = `</${tagName}`
  while (searchFrom < value.length) {
    const openIndex = lowerValue.indexOf(openTag, searchFrom)
    if (openIndex < 0) {
      result += value.slice(searchFrom)
      break
    }
    if (!isTagBoundary(value[openIndex + openTag.length])) {
      result += value.slice(searchFrom, openIndex + 1)
      searchFrom = openIndex + 1
      continue
    }
    result += value.slice(searchFrom, openIndex)
    const closeIndex = lowerValue.indexOf(closeTagStart, openIndex + openTag.length)
    if (closeIndex < 0) {
      const tagEnd = value.indexOf('>', openIndex)
      searchFrom = tagEnd < 0 ? value.length : tagEnd + 1
    } else {
      const closeTagEnd = value.indexOf('>', closeIndex)
      searchFrom = closeTagEnd < 0 ? value.length : closeTagEnd + 1
    }
  }
  return result
}

function isTagBoundary(char) {
  return char === undefined || char === '>' || char === '/' || char === ' ' || char === '\n' || char === '\t'
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
