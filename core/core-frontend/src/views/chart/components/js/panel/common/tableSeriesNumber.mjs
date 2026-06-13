export const TABLE_SERIES_NUMBER_FIELD = '$$series_number$$'

export function configureTableSeriesNumber(options, tableHeader = {}) {
  const enable = Boolean(tableHeader.showIndex)
  options.showSeriesNumber = enable
  options.seriesNumber = {
    ...(options.seriesNumber || {}),
    enable,
    text: tableHeader.indexLabel || ''
  }
  return options
}

export function isTableSeriesNumberField(field) {
  return field === TABLE_SERIES_NUMBER_FIELD
}

export function isTableSeriesNumberCell(meta) {
  return isTableSeriesNumberField(meta?.valueField)
}

export function isTableSeriesNumberNode(node) {
  return isTableSeriesNumberField(node?.field)
}

export function getPageSeriesNumber(pageInfo = {}, rowIndex = 0) {
  const pageSize = Number(pageInfo.pageSize || 0)
  const currentPage = Math.max(Number(pageInfo.currentPage || 1), 1)
  const offset = pageSize ? pageSize * (currentPage - 1) : 0
  return offset + rowIndex + 1
}

export function spanSeriesNumberHeader(layoutResult) {
  const headerHeight = Number(layoutResult?.colsHierarchy?.height || 0)
  if (!headerHeight) {
    return false
  }
  let changed = false
  ;(layoutResult?.colNodes || []).forEach(node => {
    if (!isTableSeriesNumberNode(node)) {
      return
    }
    node.y = 0
    node.height = headerHeight
    changed = true
  })
  return changed
}
