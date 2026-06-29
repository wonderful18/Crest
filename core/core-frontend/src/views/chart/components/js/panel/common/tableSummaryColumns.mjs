export const getVisibleSummaryColumns = summaryColumns => {
  return Array.isArray(summaryColumns) ? summaryColumns.filter(column => column?.show !== false) : []
}

export const getSummaryColumnEmptyText = column => {
  return column?.format?.emptyText ?? '--'
}

const toNumber = value => {
  if (value === '' || value === null || value === undefined) {
    return null
  }
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : null
}

const addThousands = value => {
  const [integer, decimal] = `${value}`.split('.')
  const formatted = integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return decimal === undefined ? formatted : `${formatted}.${decimal}`
}

export const formatSummaryColumnValue = (value, column) => {
  const numberValue = toNumber(value)
  if (numberValue === null) {
    return value ?? getSummaryColumnEmptyText(column)
  }
  const format = column?.format || {}
  if (
    format.decimal === undefined &&
    !format.percent &&
    !format.thousandSeparator &&
    !format.unit
  ) {
    return value
  }
  const decimal = Number.isInteger(format.decimal) ? format.decimal : undefined
  const displayValue = format.percent ? numberValue * 100 : numberValue
  let result = decimal === undefined ? `${displayValue}` : displayValue.toFixed(decimal)
  if (format.thousandSeparator) {
    result = addThousands(result)
  }
  if (format.percent) {
    return `${result}%`
  }
  return `${result}${format.unit || ''}`
}

export const appendSummaryColumnsToS2Config = (dataConfig, summaryColumns = []) => {
  const visibleColumns = getVisibleSummaryColumns(summaryColumns)
  if (!visibleColumns.length) {
    return dataConfig
  }
  const columns = dataConfig.fields?.columns || []
  const meta = dataConfig.meta || []
  visibleColumns.forEach(column => {
    if (!columns.includes(column.id)) {
      columns.push(column.id)
    }
    if (!meta.some(item => item.field === column.id)) {
      meta.push({
        field: column.id,
        name: column.name,
        id: column.id
      })
    }
  })
  ;(dataConfig.data || []).forEach(row => {
    visibleColumns.forEach(column => {
      if (row[column.id] === undefined) {
        row[column.id] = getSummaryColumnEmptyText(column)
      }
    })
  })
  return dataConfig
}

export const buildSummaryRowWithColumns = (
  summaryRow,
  summaryColumns = [],
  summaryColumnResult = {}
) => {
  const result = { ...summaryRow }
  getVisibleSummaryColumns(summaryColumns).forEach(column => {
    const value = summaryColumnResult?.[column.id]
    result[column.id] = value === undefined || value === null
      ? getSummaryColumnEmptyText(column)
      : formatSummaryColumnValue(value, column)
  })
  return result
}
