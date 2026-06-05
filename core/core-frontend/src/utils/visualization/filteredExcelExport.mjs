export const EXCEL_EXPORT_SCOPE = {
  CURRENT_FILTERED: 'currentFiltered',
  ALL: 'all'
}

export const EXCEL_EXPORT_CONTENT = {
  VIEW: 'view',
  DATASET: 'dataset',
  FORMATTED: 'formatted'
}

export const EXCEL_EXPORT_TABLE_TYPES = ['table-info', 'table-normal', 'table-pivot']

const DEFAULT_BUTTON_TEXT = '导出Excel'

export function deepClone(value) {
  if (value === null || value === undefined) {
    return value
  }
  return JSON.parse(JSON.stringify(value))
}

export function normalizeViewId(id) {
  return id === null || id === undefined ? '' : String(id)
}

export function isExportableTableView(viewInfo) {
  return EXCEL_EXPORT_TABLE_TYPES.includes(viewInfo?.type)
}

export function isPivotTableView(viewInfo) {
  return viewInfo?.type === 'table-pivot'
}

export function defaultViewExcelExportConfig(config = {}) {
  return {
    enabled: config.enabled !== false,
    scope: config.scope || EXCEL_EXPORT_SCOPE.CURRENT_FILTERED,
    content: config.content || EXCEL_EXPORT_CONTENT.VIEW
  }
}

export function defaultExportButtonConfig(config = {}) {
  return {
    targetViewId: normalizeViewId(config.targetViewId),
    scope: config.scope || EXCEL_EXPORT_SCOPE.CURRENT_FILTERED,
    content: config.content || EXCEL_EXPORT_CONTENT.VIEW,
    text: config.text || DEFAULT_BUTTON_TEXT
  }
}

export function flattenCanvasComponents(components = []) {
  const result = []
  const visit = component => {
    if (!component) {
      return
    }
    result.push(component)
    if (Array.isArray(component.propValue)) {
      if (component.component === 'Group' || component.innerType === 'Group') {
        component.propValue.forEach(visit)
        return
      }
      if (component.component === 'Tabs' || component.innerType === 'Tabs') {
        component.propValue.forEach(tab => {
          ;(tab?.componentData || []).forEach(visit)
        })
      }
    }
  }
  components.forEach(visit)
  return result
}

export function collectExportableTableViews(components = [], canvasViewInfo = {}) {
  return flattenCanvasComponents(components)
    .filter(component => component?.component === 'UserView')
    .map(component => {
      const id = normalizeViewId(component.id)
      const viewInfo = canvasViewInfo[id] || canvasViewInfo[component.id]
      return {
        id,
        component,
        viewInfo,
        label: viewInfo?.title || component?.name || component?.label || id
      }
    })
    .filter(item => item.id && isExportableTableView(item.viewInfo))
}

export function getQueryTargetIds(queryItem = {}) {
  const checkedFields = queryItem.checkedFields || []
  const checkedFieldsMap = queryItem.checkedFieldsMap || {}
  let fieldMaps = Object.entries(checkedFieldsMap).filter(([viewId]) =>
    checkedFields.includes(viewId)
  )

  if (queryItem.displayType === '9') {
    const treeFieldList = queryItem.treeFieldList || []
    const treeCheckedList = queryItem.treeCheckedList?.length
      ? queryItem.treeCheckedList.filter((_, index) => index < treeFieldList.length)
      : treeFieldList.map(() => ({
          checkedFields: [...checkedFields],
          checkedFieldsMap: deepClone(checkedFieldsMap)
        }))

    fieldMaps = treeCheckedList
      .map(item =>
        Object.entries(item.checkedFieldsMap || {}).filter(([viewId]) =>
          (item.checkedFields || []).includes(viewId)
        )
      )
      .flat()
  }

  return [...new Set(fieldMaps.filter(([, fieldId]) => !!fieldId).map(([viewId]) => viewId))]
}

export function queryItemAffectsView(queryItem, targetViewId) {
  const targetId = normalizeViewId(targetViewId)
  return getQueryTargetIds(queryItem).some(viewId => normalizeViewId(viewId) === targetId)
}

function valuePresent(value) {
  if (Array.isArray(value)) {
    return value.length > 0
  }
  return value !== null && value !== undefined && value !== ''
}

function pairValueCount(start, end) {
  return [start, end].filter(value => value !== null && value !== undefined && value !== '').length
}

export function validateQueryItemValue(queryItem = {}) {
  const displayType = String(queryItem.displayType ?? '')

  if (displayType === '22') {
    const start = queryItem.numValueStart
    const end = queryItem.numValueEnd
    const count = pairValueCount(start, end)
    if (queryItem.required && count < 2) {
      return { valid: false, reason: 'required' }
    }
    if (count === 1) {
      return { valid: false, reason: 'numberRangeIncomplete' }
    }
    if (!isNaN(start) && !isNaN(end) && count === 2 && Number(end) < Number(start)) {
      return { valid: false, reason: 'numberRangeInvalid' }
    }
    return { valid: true }
  }

  if (!queryItem.required) {
    return { valid: true }
  }

  if (displayType === '8') {
    const conditionType = queryItem.conditionType ?? 0
    const first = queryItem.conditionValueF
    const second = queryItem.conditionValueS
    if (conditionType === 0) {
      return valuePresent(first) ? { valid: true } : { valid: false, reason: 'required' }
    }
    return pairValueCount(first, second) === 2
      ? { valid: true }
      : { valid: false, reason: 'required' }
  }

  return valuePresent(queryItem.selectValue) ? { valid: true } : { valid: false, reason: 'required' }
}

export function validateQueryComponentsForExport(components = [], targetViewId) {
  const queryComponents = flattenCanvasComponents(components).filter(
    component => component?.component === 'VQuery' || component?.innerType === 'VQuery'
  )

  for (const component of queryComponents) {
    for (const queryItem of component.propValue || []) {
      if (!queryItemAffectsView(queryItem, targetViewId)) {
        continue
      }
      const check = validateQueryItemValue(queryItem)
      if (!check.valid) {
        return {
          valid: false,
          reason: check.reason,
          queryName: queryItem.name,
          componentId: component.id
        }
      }
    }
  }

  return { valid: true }
}

export function validateExportTarget({
  targetViewId,
  components = [],
  canvasViewInfo = {},
  scope = EXCEL_EXPORT_SCOPE.CURRENT_FILTERED,
  content = EXCEL_EXPORT_CONTENT.VIEW,
  validateQueries = true
}) {
  const targetId = normalizeViewId(targetViewId)
  if (!targetId) {
    return { valid: false, reason: 'targetRequired' }
  }

  const viewInfo = canvasViewInfo[targetId] || canvasViewInfo[targetViewId]
  if (!viewInfo) {
    return { valid: false, reason: 'targetNotFound' }
  }

  if (!isExportableTableView(viewInfo)) {
    return { valid: false, reason: 'unsupportedViewType', viewInfo }
  }

  if (content === EXCEL_EXPORT_CONTENT.FORMATTED && !isPivotTableView(viewInfo)) {
    return { valid: false, reason: 'formattedOnlyPivot', viewInfo }
  }

  if (scope === EXCEL_EXPORT_SCOPE.ALL) {
    return { valid: true, viewInfo }
  }

  if (validateQueries) {
    const queryCheck = validateQueryComponentsForExport(components, targetId)
    if (!queryCheck.valid) {
      return { ...queryCheck, viewInfo }
    }
  }

  return { valid: true, viewInfo }
}

export function buildDefaultChartExtRequest(viewInfo = {}, lastRequest = {}) {
  return {
    user: lastRequest.user,
    filter: [],
    linkageFilters: [],
    outerParamsFilters: [],
    webParamsFilters: [],
    drill: [],
    resultCount: viewInfo.resultCount || lastRequest.resultCount || 1000,
    resultMode: viewInfo.resultMode || lastRequest.resultMode || 'all'
  }
}

export function buildExportChartExtRequest({
  lastRequest,
  viewInfo,
  scope = EXCEL_EXPORT_SCOPE.CURRENT_FILTERED
}) {
  if (scope === EXCEL_EXPORT_SCOPE.ALL) {
    return buildDefaultChartExtRequest(viewInfo, lastRequest || {})
  }

  const request = lastRequest ? deepClone(lastRequest) : buildDefaultChartExtRequest(viewInfo)
  request.filter = Array.isArray(request.filter) ? request.filter : []
  request.linkageFilters = Array.isArray(request.linkageFilters) ? request.linkageFilters : []
  request.outerParamsFilters = Array.isArray(request.outerParamsFilters)
    ? request.outerParamsFilters
    : []
  request.webParamsFilters = Array.isArray(request.webParamsFilters) ? request.webParamsFilters : []
  request.drill = Array.isArray(request.drill) ? request.drill : []
  request.resultCount = request.resultCount || viewInfo?.resultCount || 1000
  request.resultMode = request.resultMode || viewInfo?.resultMode || 'all'
  return request
}

export function explainExportValidation(validation) {
  const queryName = validation?.queryName ? `【${validation.queryName}】` : ''
  switch (validation?.reason) {
    case 'targetRequired':
      return '请选择目标表格'
    case 'targetNotFound':
      return '目标表格不存在或已被删除'
    case 'unsupportedViewType':
      return '当前仅支持表格组件导出'
    case 'formattedOnlyPivot':
      return '带格式导出仅支持透视表'
    case 'required':
      return `${queryName}请先完成必填筛选项`
    case 'numberRangeIncomplete':
      return `${queryName}请填写完整的数值范围`
    case 'numberRangeInvalid':
      return `${queryName}结束值不能小于开始值`
    default:
      return '当前配置无法导出'
  }
}
