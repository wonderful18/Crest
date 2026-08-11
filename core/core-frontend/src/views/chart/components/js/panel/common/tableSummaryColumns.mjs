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

// 仅允许算术运算符、数字、括号与空白，避免 SQL 函数等非法字符被求值
const ARITHMETIC_EXPR = /^[\d+\-*/().\s]+$/

// 解析字段引用占位 [fieldId]，返回所有引用到的 id
const FIELD_REF_REGEX = /\[([^\]]+)\]/g

/**
 * 从图表的行/列/指标轴构建 字段 id -> engineFieldName 映射，
 * 供自定义汇总列公式逐行求值时把 [fieldId] 占位解析为该行字段值。
 * @param {ChartObj} chart
 * @returns {Record<string, string>}
 */
export const buildIdToEngineFieldMap = chart => {
  const fields = [
    ...(chart?.xAxis || []),
    ...(chart?.xAxisExt || []),
    ...(chart?.yAxis || [])
  ]
  const map = {}
  fields.forEach(f => {
    if (f?.id != null && f?.engineFieldName) {
      map[f.id] = f.engineFieldName
    }
  })
  return map
}

/**
 * 前端逐行求值自定义汇总列公式。
 * originName 形如 `[fieldId1]+[fieldId2]`，把 [fieldId] 替换为该行对应字段值后求值。
 * 仅支持算术运算 (+ - * / 与括号、数字)，含 SQL 函数等非算术字符返回 null（由调用方回退空值占位）。
 * 字段值为空/非数字按 0 处理，与 safeDecimalSum 口径一致。
 * @param {string} originName 公式
 * @param {Record<string, any>} row 单行数据（键为 engineFieldName）
 * @param {Record<string, string>} idToEngineField 字段 id -> engineFieldName 映射
 * @returns {number|null}
 */
export const evalSummaryColumnFormula = (originName, row, idToEngineField = {}) => {
  if (!originName || typeof originName !== 'string') {
    return null
  }
  // 公式编辑器存公式常带前导 '='（类 Excel 写法，如 =[fieldId1]+[fieldId2]），求值前去掉
  let expr = originName.trim()
  if (expr.startsWith('=')) {
    expr = expr.slice(1)
  }
  let matched = false
  expr = expr.replace(FIELD_REF_REGEX, (_, id) => {
    matched = true
    const engineField = idToEngineField[id]
    const raw = engineField ? row[engineField] : undefined
    const num = Number(raw)
    return Number.isFinite(num) && raw !== '' && raw !== null && raw !== undefined
      ? `(${num})`
      : '(0)'
  })
  // 没有任何字段引用，或替换后含非法字符（如 SQL 函数名），不前端求值
  if (!matched || !ARITHMETIC_EXPR.test(expr)) {
    return null
  }
  try {
    // 白名单校验后用 Function 求值；expr 仅含数字与算术运算符
    // eslint-disable-next-line no-new-func
    const result = Function(`"use strict"; return (${expr});`)()
    return Number.isFinite(result) ? result : null
  } catch (_e) {
    return null
  }
}

export const appendSummaryColumnsToS2Config = (dataConfig, summaryColumns = [], idToEngineField = {}) => {
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
        // 自定义公式列：在前端按公式逐行求值，数据行直接显示每行算出的值
        if (column.summary === 'custom' && column.originName) {
          const value = evalSummaryColumnFormula(column.originName, row, idToEngineField)
          row[column.id] = value === null || value === undefined
            ? getSummaryColumnEmptyText(column)
            : value
        } else {
          row[column.id] = getSummaryColumnEmptyText(column)
        }
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
