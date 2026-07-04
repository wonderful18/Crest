export function isSortTooltipRequest(showOptions = {}) {
  if (showOptions.sortMenu === true) {
    return true
  }
  return isCustomSortIconName(showOptions.iconName) || isCustomSortIconName(showOptions.name)
}

export function getTooltipPointerPosition(event = {}) {
  const { clientX, clientY } = event
  if (typeof clientX !== 'number' || typeof clientY !== 'number') {
    return null
  }
  return { x: clientX, y: clientY }
}

export function hasTooltipContent(showOptions = {}) {
  return showOptions.content !== undefined && showOptions.content !== null && showOptions.content !== ''
}

export function getHeaderSortStateId(meta = {}) {
  return meta.id || meta.field
}

export function getHeaderSortField(meta = {}) {
  return meta.field || meta.id
}

export function sortTableRowsForHeader(sortParams = {}) {
  const { data, sortFieldId, sortMethod } = sortParams
  if (!sortMethod || !sortFieldId || !Array.isArray(data)) {
    return data
  }
  const direction = String(sortMethod).toUpperCase() === 'ASC' ? 1 : -1
  return [...data].sort((a, b) => {
    if (isSummaryRow(a) && isSummaryRow(b)) {
      return 0
    }
    if (isSummaryRow(a)) {
      return 1
    }
    if (isSummaryRow(b)) {
      return -1
    }
    return compareSortValues(a?.[sortFieldId], b?.[sortFieldId], direction)
  })
}

function isCustomSortIconName(iconName) {
  return typeof iconName === 'string' && iconName.startsWith('customSort')
}

function isSummaryRow(row) {
  return Boolean(row?.SUMMARY)
}

function compareSortValues(valueA, valueB, direction) {
  if (isNil(valueA) && isNil(valueB)) {
    return 0
  }
  if (isNil(valueA)) {
    return 1
  }
  if (isNil(valueB)) {
    return -1
  }
  if (canConvertToNumber(valueA) && canConvertToNumber(valueB)) {
    return (Number(valueA) - Number(valueB)) * direction
  }
  return valueA.toString().localeCompare(valueB.toString(), 'zh') * direction
}

function isNil(value) {
  return value === null || value === undefined
}

function canConvertToNumber(value) {
  if (typeof value === 'number') {
    return !Number.isNaN(value)
  }
  if (!value) {
    return false
  }
  if (typeof value === 'string') {
    return !Number.isNaN(Number(value))
  }
  return false
}
