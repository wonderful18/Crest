const fieldName = item => item?.field ?? item?.engineFieldName

const isQuotaField = (field, fieldsMap) => fieldsMap[field]?.groupType === 'q'

const createFieldMap = fields =>
  (fields || []).reduce((pre, cur) => {
    pre[cur.engineFieldName] = cur
    return pre
  }, {})

const buildCellsForRange = ({ data, field, colIndex, start, end }) => {
  const mergeCells = []
  const nextRanges = []
  let lastVal = data[start]?.[field]
  let lastIndex = start

  for (let index = start; index <= end; index++) {
    const curVal = data[index]?.[field]
    if (curVal !== lastVal || index === end) {
      const curRange = index - lastIndex
      if (curRange > 1 || (index === end && curRange === 1 && lastVal === curVal)) {
        const tmpMergeCells = []
        let textIndex = curRange % 2 === 1 ? (curRange - 1) / 2 : curRange / 2 - 1
        if (index === end && lastVal === curVal) {
          textIndex = curRange % 2 === 1 ? (curRange - 1) / 2 : curRange / 2
        }
        for (let j = 0; j < curRange; j++) {
          tmpMergeCells.push({
            colIndex,
            rowIndex: lastIndex + j,
            showText: j === textIndex
          })
        }
        if (index === end && lastVal === curVal) {
          tmpMergeCells.push({
            colIndex,
            rowIndex: index,
            showText: false
          })
        }
        mergeCells.push(tmpMergeCells)
        nextRanges.push([lastIndex, index === end && lastVal === curVal ? index : index - 1])
      }
      lastVal = curVal
      lastIndex = index
    }
  }

  return { mergeCells, nextRanges }
}

export const getMergeableFieldOptions = axis => {
  const options = []
  for (const item of axis || []) {
    if (item?.groupType === 'q') {
      break
    }
    options.push({
      id: item.engineFieldName,
      label: item.chartShowName ?? item.name
    })
  }
  return options
}

export const buildMergeCellsInfo = ({ fields, meta, data, showIndex = false, mergeFields }) => {
  const tableData = data || []
  if (!tableData.length) {
    return []
  }

  const fieldsMap = createFieldMap(fields)
  const quotaIndex = (meta || []).findIndex(item => isQuotaField(fieldName(item), fieldsMap))
  if (quotaIndex === 0) {
    return []
  }

  const mergeableColumns = (meta || [])
    .map((item, index) => ({
      field: fieldName(item),
      colIndex: showIndex ? index + 1 : index
    }))
    .filter((_, index) => index < quotaIndex || quotaIndex === -1)

  const selectedFields = Array.isArray(mergeFields) ? new Set(mergeFields) : null
  const columnsToMerge = selectedFields
    ? mergeableColumns.filter(item => selectedFields.has(item.field))
    : mergeableColumns

  if (!columnsToMerge.length) {
    return []
  }

  let ranges = [[0, tableData.length - 1]]
  const mergedCellsInfo = []

  columnsToMerge.forEach(column => {
    const nextRanges = []
    ranges.forEach(([start, end]) => {
      const result = buildCellsForRange({
        data: tableData,
        field: column.field,
        colIndex: column.colIndex,
        start,
        end
      })
      mergedCellsInfo.push(...result.mergeCells)
      nextRanges.push(...result.nextRanges)
    })
    ranges = nextRanges
  })

  if (showIndex && mergedCellsInfo.length) {
    const firstMergedColIndex = mergedCellsInfo[0][0].colIndex
    const indexMergedCells = mergedCellsInfo.filter(
      cells => cells[0].colIndex === firstMergedColIndex
    )
    indexMergedCells.reverse().forEach(cells => {
      const tmpCells = cells.map(cell => ({
        ...cell,
        colIndex: 0
      }))
      mergedCellsInfo.unshift(tmpCells)
    })
  }

  return mergedCellsInfo
}
