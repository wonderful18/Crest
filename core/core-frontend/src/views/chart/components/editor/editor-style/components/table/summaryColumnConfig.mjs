export const SUMMARY_COLUMN_VALIDATION = {
  FIELD_REQUIRED: 'field_required',
  FORMULA_REQUIRED: 'formula_required'
}

const isBlank = value => value === '' || value === null || value === undefined
const SUMMARY_COLUMN_NUMERIC_FIELD_TYPES = [2, 3, 4]

export const getSummaryColumnFieldKey = field => field?.engineFieldName || field?.field

export const buildSummaryColumnFieldOptions = (fields = [], { includeNonNumeric = false } = {}) => {
  const result = new Map()
  fields.forEach(field => {
    const key = getSummaryColumnFieldKey(field)
    if (!key || field?.id === '-1' || result.has(key)) {
      return
    }
    if (!includeNonNumeric && !SUMMARY_COLUMN_NUMERIC_FIELD_TYPES.includes(field?.fieldType)) {
      return
    }
    result.set(key, { ...field, id: field.id || key, engineFieldName: key })
  })
  return Array.from(result.values())
}

export const normalizeSummaryColumn = (
  column = {},
  { defaultName = '', idFactory = () => `summary_${Date.now()}` } = {}
) => {
  const originName = column.originName || ''
  return {
    id: column.id || idFactory(),
    name: column.name || defaultName,
    show: column.show !== false,
    summary: originName ? 'custom' : column.summary || 'sum',
    field: column.field,
    originName,
    position: column.position || 'append',
    format: {
      decimal: column.format?.decimal ?? 2,
      unit: column.format?.unit ?? '',
      percent: column.format?.percent ?? false,
      thousandSeparator: column.format?.thousandSeparator ?? true,
      emptyText: column.format?.emptyText ?? '--'
    }
  }
}

export const validateSummaryColumn = column => {
  if (column?.summary === 'custom') {
    return isBlank(column.originName)
      ? { valid: false, reason: SUMMARY_COLUMN_VALIDATION.FORMULA_REQUIRED }
      : { valid: true }
  }
  return isBlank(column?.field)
    ? { valid: false, reason: SUMMARY_COLUMN_VALIDATION.FIELD_REQUIRED }
    : { valid: true }
}
