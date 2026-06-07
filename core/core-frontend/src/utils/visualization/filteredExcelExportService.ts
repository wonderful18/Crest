import { h } from 'vue'
import { cloneDeep } from 'lodash-es'
import { ElButton, ElMessage } from 'element-plus-secondary'
import { RefreshLeft } from '@element-plus/icons-vue'
import { getData } from '@/api/chart'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { useEmitt } from '@/hooks/web/useEmitt'
import { exportExcelDownload } from '@/views/chart/components/js/util'
import { exportPivotExcel } from '@/views/chart/components/js/panel/common/common_table'
import {
  EXCEL_EXPORT_CONTENT,
  EXCEL_EXPORT_SCOPE,
  buildExportChartExtRequest,
  defaultExportButtonConfig,
  defaultViewExcelExportConfig,
  explainExportValidation,
  validateExportTarget
} from './filteredExcelExport.mjs'

type ExportScope = 'currentFiltered' | 'all'
type ExportContent = 'view' | 'dataset' | 'formatted'

interface FilteredExcelExportOptions {
  targetViewId: string | number
  scope?: ExportScope
  content?: ExportContent
  dvName?: string
  onQueued?: () => void
  validateQueries?: boolean
}

const normalizeId = (id: string | number) => (id === null || id === undefined ? '' : String(id))

const exportCenterOpen = () => {
  useEmitt().emitter.emit('data-export-center', { activeName: 'ALL' })
}

export const showExportQueuedMessage = (openExportCenter = exportCenterOpen) => {
  ElMessage({
    message: h('p', null, [
      '后台导出中,可前往',
      h(
        ElButton,
        {
          text: true,
          size: 'small',
          class: 'btn-text',
          onClick: openExportCenter
        },
        '数据导出中心'
      ),
      '查看进度，进行下载'
    ]),
    iconClass: 'el-icon-loading',
    icon: h(RefreshLeft) as any,
    showClose: true,
    customClass: 'crest-message-loading crest-message-export'
  } as any)
}

export const ensureViewExcelExportConfig = element => {
  element.excelExport = defaultViewExcelExportConfig(element.excelExport)
  return element.excelExport
}

export const ensureExportButtonConfig = element => {
  element.exportButton = defaultExportButtonConfig(element.exportButton)
  return element.exportButton
}

const resolveChartData = data => {
  if (!data) {
    return null
  }
  return data.data || data
}

const fetchChartData = async (targetViewId: string, viewInfo, chartExtRequest) => {
  const dvMainStore = dvMainStoreWithOut()
  const response = await getData({
    ...cloneDeep(viewInfo),
    chartExtRequest
  })
  if ((response as any)?.code && (response as any).code !== 0) {
    throw new Error((response as any).msg || '获取图表数据失败')
  }
  dvMainStore.setViewDataDetails(targetViewId, response)
  return resolveChartData((response as any)?.data)
}

export const exportFilteredExcel = async ({
  targetViewId,
  scope = EXCEL_EXPORT_SCOPE.CURRENT_FILTERED,
  content = EXCEL_EXPORT_CONTENT.VIEW,
  dvName,
  onQueued,
  validateQueries = true
}: FilteredExcelExportOptions) => {
  const targetId = normalizeId(targetViewId)
  const dvMainStore = dvMainStoreWithOut()
  const validation = validateExportTarget({
    targetViewId: targetId,
    components: dvMainStore.componentData,
    canvasViewInfo: dvMainStore.canvasViewInfo,
    scope,
    content,
    validateQueries
  })

  if (!validation.valid) {
    ElMessage.error(explainExportValidation(validation))
    return false
  }

  const sourceViewInfo = cloneDeep(dvMainStore.getViewDetails(targetId) || validation.viewInfo)
  const lastRequest = dvMainStore.getLastViewRequestInfo(targetId) || sourceViewInfo.chartExtRequest
  const chartExtRequest = buildExportChartExtRequest({
    lastRequest,
    viewInfo: sourceViewInfo,
    scope
  })

  if (content === EXCEL_EXPORT_CONTENT.FORMATTED) {
    const instance = dvMainStore.getViewInstanceInfo(targetId)
    if (!instance) {
      ElMessage.error('当前表格尚未渲染完成，请稍后再导出')
      return false
    }
    await exportPivotExcel(instance, sourceViewInfo)
    return true
  }

  let viewDataInfo = resolveChartData(dvMainStore.getViewDataDetails(targetId))
  if (!viewDataInfo) {
    try {
      viewDataInfo = await fetchChartData(targetId, sourceViewInfo, chartExtRequest)
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '获取图表数据失败')
      return false
    }
  }

  if (!viewDataInfo) {
    ElMessage.error('当前表格暂无可导出的数据')
    return false
  }

  const chart = {
    ...sourceViewInfo,
    chartExtRequest,
    data: viewDataInfo,
    downloadType: content,
    busiFlag: dvMainStore.dvInfo?.type
  }

  await exportExcelDownload(
    chart,
    dvName || dvMainStore.dvInfo?.name || sourceViewInfo.title,
    result => {
      if (result === 'error') {
        return
      }
      if (onQueued) {
        onQueued()
      } else {
        showExportQueuedMessage()
      }
    }
  )
  return true
}
