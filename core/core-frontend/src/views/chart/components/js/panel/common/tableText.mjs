const BLOCK_TAGS = new Set([
  'p',
  'div',
  'section',
  'article',
  'header',
  'footer',
  'li',
  'tr',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6'
])

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

  const text = decodeKnownEntities(
    extractHtmlText(normalizeActualNewlines(decodeEscapedWhitespace(String(value))))
  )

  return text
    .split('\n')
    .map(line => line.replace(/[ \f\v]+/g, ' ').trim())
    .join('\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function extractHtmlText(value) {
  if (typeof DOMParser === 'undefined') {
    return stripHtmlFallback(value)
  }
  const document = new DOMParser().parseFromString(value, 'text/html')
  return collectText(document.body)
}

function collectText(node) {
  let result = ''
  Array.from(node.childNodes).forEach(child => {
    if (child.nodeType === 3) {
      result += child.textContent ?? ''
      return
    }
    if (child.nodeType !== 1) {
      return
    }
    const tagName = child.tagName.toLowerCase()
    if (tagName === 'br') {
      result += '\n'
    }
    result += collectText(child)
    if (BLOCK_TAGS.has(tagName)) {
      result += '\n'
    }
  })
  return result
}

function stripHtmlFallback(value) {
  let result = ''
  let insideTag = false
  for (let index = 0; index < value.length; index += 1) {
    const current = value[index]
    if (current === '<') {
      insideTag = true
      continue
    }
    if (current === '>') {
      insideTag = false
      continue
    }
    if (!insideTag) {
      result += current
    }
  }
  return result
}

function decodeKnownEntities(value) {
  return Object.entries(entityMap).reduce((result, [entity, replacement]) => {
    return replaceIgnoreCase(result, entity, replacement)
  }, value)
}

function replaceIgnoreCase(value, search, replacement) {
  let result = ''
  let cursor = 0
  const lowerValue = value.toLowerCase()
  const lowerSearch = search.toLowerCase()
  let index = lowerValue.indexOf(lowerSearch, cursor)
  while (index !== -1) {
    result += value.slice(cursor, index) + replacement
    cursor = index + search.length
    index = lowerValue.indexOf(lowerSearch, cursor)
  }
  return result + value.slice(cursor)
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
