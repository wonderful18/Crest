export const AUXILIARY_HEADER_FLAG = 'crestAuxiliaryHeader'

export const DEFAULT_AUXILIARY_HEADER = {
  enabled: false,
  rowHeight: 120,
  backgroundColor: '#FFFFFF',
  fontColor: '#1F2329',
  fontSize: 14,
  align: 'center',
  descriptions: []
}

const GENERATED_GROUP_PREFIX = '__crest_aux_header_group__'

function toText(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value)
}

function getColumnField(column) {
  if (typeof column === 'string') {
    return column
  }
  return column?.key ?? column?.field ?? ''
}

function getColumnTitle(column, groupNameMap, fieldNameMap) {
  if (typeof column === 'string') {
    return fieldNameMap[column] ?? column
  }
  const field = getColumnField(column)
  return column?.title ?? groupNameMap[field] ?? fieldNameMap[field] ?? field
}

function normalizeDescriptions(descriptions) {
  if (!Array.isArray(descriptions)) {
    return []
  }
  return descriptions
    .filter(item => item && item.field)
    .map(item => ({
      field: String(item.field),
      text: toText(item.text)
    }))
}

export function normalizeAuxiliaryHeader(auxiliaryHeader) {
  return {
    ...DEFAULT_AUXILIARY_HEADER,
    ...(auxiliaryHeader || {}),
    rowHeight: Number(auxiliaryHeader?.rowHeight) || DEFAULT_AUXILIARY_HEADER.rowHeight,
    fontSize: Number(auxiliaryHeader?.fontSize) || DEFAULT_AUXILIARY_HEADER.fontSize,
    descriptions: normalizeDescriptions(auxiliaryHeader?.descriptions)
  }
}

export function isAuxiliaryHeaderEnabled(auxiliaryHeader) {
  return normalizeAuxiliaryHeader(auxiliaryHeader).enabled === true
}

export function getAuxiliaryDescriptionMap(auxiliaryHeader) {
  return normalizeAuxiliaryHeader(auxiliaryHeader).descriptions.reduce((pre, cur) => {
    pre[cur.field] = cur.text
    return pre
  }, {})
}

export function normalizeAuxiliaryDescriptions(auxiliaryHeader, fields) {
  const descMap = getAuxiliaryDescriptionMap(auxiliaryHeader)
  return fields.map(field => ({
    field,
    text: descMap[field] ?? ''
  }))
}

export function hasAuxiliaryDescription(auxiliaryHeader, fields) {
  const descMap = getAuxiliaryDescriptionMap(auxiliaryHeader)
  return fields.some(field => toText(descMap[field]).trim().length > 0)
}

export function getLeafColumnFields(columns) {
  const result = []
  const walk = list => {
    ;(list || []).forEach(column => {
      if (typeof column !== 'string' && column?.children?.length) {
        walk(column.children)
        return
      }
      const field = getColumnField(column)
      if (field) {
        result.push(field)
      }
    })
  }
  walk(columns)
  return result
}

function getLeafColumnPaths(columns, fieldNameMap = {}) {
  const result = {}
  const walk = (list, path) => {
    ;(list || []).forEach(column => {
      const field = getColumnField(column)
      const title = getColumnTitle(column, {}, fieldNameMap)
      const nextPath = [...path, title]
      const children = typeof column === 'string' ? [] : column?.children || []
      if (children.length) {
        walk(children, nextPath)
        return
      }
      if (field) {
        result[field] = nextPath.length ? nextPath : [fieldNameMap[field] ?? field]
      }
    })
  }
  walk(columns, [])
  return result
}

function alignHeaderPath(path, depth) {
  if (!depth) {
    return []
  }
  if (!path.length) {
    return Array.from({ length: depth }, () => '')
  }
  if (path.length >= depth) {
    return path.slice(0, depth)
  }
  const [first] = path
  return [...Array.from({ length: depth - path.length }, () => first), ...path]
}

export function buildHeaderCopyRows(columns, fields, fieldNameMap = {}) {
  const leafPaths = getLeafColumnPaths(columns, fieldNameMap)
  const paths = (fields || []).map(field => {
    const normalizedField = toText(field)
    return leafPaths[normalizedField] || [fieldNameMap[normalizedField] ?? normalizedField]
  })
  const depth = Math.max(0, ...paths.map(path => path.length))
  if (!depth) {
    return []
  }
  const alignedPaths = paths.map(path => alignHeaderPath(path, depth))
  return Array.from({ length: depth }, (_, rowIndex) =>
    alignedPaths.map(path => path[rowIndex] ?? '')
  )
}

function createS2Column(field, title, children, extra = {}) {
  const column = {
    key: field,
    field,
    title: toText(title)
  }
  if (children?.length) {
    column.children = children
  }
  Object.assign(column, extra)
  return column
}

function createAuxiliaryLeaf(field, description) {
  return createS2Column(field, description, undefined, {
    [AUXILIARY_HEADER_FLAG]: true
  })
}

export function buildS2HeaderColumns(columns, meta = [], fieldNameMap = {}, auxiliaryHeader) {
  const groupNameMap = meta.reduce((pre, cur) => {
    if (cur?.field) {
      pre[cur.field] = cur.name
    }
    return pre
  }, {})
  const auxiliaryEnabled = isAuxiliaryHeaderEnabled(auxiliaryHeader)
  const descriptionMap = getAuxiliaryDescriptionMap(auxiliaryHeader)
  const sourceColumns = columns || []

  if (!auxiliaryEnabled && sourceColumns.every(column => typeof column === 'string')) {
    return [...sourceColumns]
  }

  const walk = (column, isRoot) => {
    const field = getColumnField(column)
    const title = getColumnTitle(column, groupNameMap, fieldNameMap)
    const children = typeof column === 'string' ? [] : column?.children || []

    if (!children.length) {
      if (!auxiliaryEnabled) {
        return createS2Column(field, title)
      }

      const leaf = createAuxiliaryLeaf(field, descriptionMap[field] ?? '')
      if (!isRoot) {
        return leaf
      }
      return createS2Column(`${GENERATED_GROUP_PREFIX}${field}`, title, [leaf], {
        generatedAuxiliaryGroup: true,
        sourceField: field
      })
    }

    return createS2Column(
      field,
      title,
      children.map(child => walk(child, false))
    )
  }

  return sourceColumns.map(column => walk(column, true))
}
