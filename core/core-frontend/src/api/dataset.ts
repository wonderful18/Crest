import request from '@/config/axios'
import {
  originNameHandle,
  originNameHandleBack,
  originNameHandleBackWithArr
} from '@/utils/CalculateFields'
import { type Field } from '@/api/chart'
import { cloneDeep } from 'lodash-es'
import type { BusiTreeRequest } from '@/models/tree/TreeNode'
import { nameTrim } from '@/utils/utils'
export interface DatasetOrFolder {
  name: string
  action?: string
  isCross?: boolean
  id?: number | string
  pid?: number | string
  nodeType: 'folder' | 'dataset'
  mode?: number
  union?: Array<{}>
  allFields?: Array<{}>
}

export interface EnumValue {
  queryId: string
  displayId?: string
  sortId?: string
  sort?: string
  resultMode?: number
  searchText: string
  filter?: Array<{}>
  visualizationId?: string | number
}

interface Fields {
  fields: Array<{}>
  data: Array<{}>
}
export interface ParamsDetail {
  datasetGroupId: string
  type: Array<string | number>
  variableName: string
  params?: Field[]
  [key: string]: any
}

export interface DatasetDetail {
  id: string
  name: string
  componentId: string
  fields: {
    dimensionList: Array<Field>
    quotaList: Array<Field>
    parameterList?: Array<Field>
  }
  activelist?: string
  hasParameter?: boolean
  checkList: string[]
  list: Array<Field>
  checkedFields?: Field[]
  checkedFieldsMap?: Record<string, Field>
  [key: string]: any
}

export interface FieldData {
  allFields: Array<{}>
  data: Fields
  total?: number
}

export interface Dataset {
  id: string
  pid: string
  name: string
  isCross?: boolean
  mode?: number
  union?: Array<{}>
  allFields?: Array<{}>
}

export interface Table {
  datasourceId: string
  name: string
  tableName: string
  type: string
  unableCheck?: boolean
}

export interface DatasetSyncTask {
  id?: string | number
  datasetGroupId?: string | number
  datasetName?: string
  name?: string
  updateType?: 'all_scope' | 'add_scope'
  incrementalFieldId?: string | number | null
  incrementalLastValue?: string
  startTime?: number
  syncRate?: 'RIGHTNOW' | 'SIMPLE_CRON' | 'CRON' | 'MANUAL'
  cron?: string
  simpleCronValue?: number
  simpleCronType?: 'minute' | 'hour' | 'day'
  endTime?: number
  lastExecTime?: number
  heartbeatTime?: number
  lastExecStatus?: string
  taskStatus?: string
  cacheReady?: number
  schemaHash?: string
  fullSyncIntervalHours?: number
  lastFullSyncTime?: number
  verifyEnabled?: number
  lastVerifyTime?: number
  lastVerifyStatus?: string
  lastVerifyMessage?: string
  lastSourceRowCount?: number
  lastCacheRowCount?: number
  cacheExpireHours?: number
  taskTimeoutMinutes?: number
  consecutiveFailures?: number
  failureWarnThreshold?: number
  cacheExpired?: boolean
  failureWarned?: boolean
  durationMillis?: number
  failureReason?: string
  lastLogStartTime?: number
  lastLogEndTime?: number
  lastLogStatus?: string
  lastLogInfo?: string
  lastTriggerType?: string
}

export interface DatasetSyncLog {
  id?: string | number
  datasetGroupId?: string | number
  taskId?: string | number
  updateType?: string
  tableName?: string
  startTime?: number
  endTime?: number
  taskStatus?: string
  rowCount?: number
  info?: string
  createTime?: number
  triggerType?: string
}

export interface DatasetSyncTaskRequest {
  keyword?: string
  taskStatus?: string
  syncRate?: string
  updateType?: string
}

export interface DatasetSyncTaskPage {
  page: number
  pageSize: number
  total: number
  records: DatasetSyncTask[]
}
// 获取权限路
// edit
export const saveDatasetTree = async (data: DatasetOrFolder): Promise<IResponse> => {
  nameTrim(data)
  const copyData = cloneDeep(data)
  originNameHandle(copyData.allFields)
  return request.post({ url: '/dataset-tree/record', data: copyData }).then(res => {
    if (res?.data?.allFields?.length) {
      originNameHandleBack(res?.data?.allFields)
    }
    return res?.data
  })
}

// create
export const createDatasetTree = async (data: DatasetOrFolder): Promise<IResponse> => {
  nameTrim(data)
  const copyData = cloneDeep(data)
  originNameHandle(copyData.allFields)
  return request.post({ url: '/dataset-tree', data: copyData }).then(res => {
    if (res?.data?.allFields?.length) {
      originNameHandleBack(res?.data?.allFields)
    }
    return res?.data
  })
}

// rename
export const renameDatasetTree = async (data: DatasetOrFolder): Promise<IResponse> => {
  nameTrim(data)
  return request.post({ url: '/dataset-tree/rename', data }).then(res => {
    return res?.data
  })
}

export const enumValueObj = async (data: EnumValue): Promise<Record<string, string>[]> => {
  return request.post({ url: '/dataset-data/enum-values/object', data }).then(res => {
    return res?.data
  })
}

export const enumValueDs = async (data: any): Promise<Record<string, string>[]> => {
  return request.post({ url: '/dataset-data/enum-values/dataset', data }).then(res => {
    return res?.data
  })
}

export const moveDatasetTree = async (data: DatasetOrFolder): Promise<IResponse> => {
  return request.post({ url: '/dataset-tree/move', data }).then(res => {
    return res?.data
  })
}

export const datasetTree = async (data: BusiTreeRequest): Promise<IResponse> => {
  data.busiFlag = 'dataset'
  return request.post({ url: '/dataset-tree/tree', data }).then(res => {
    return res?.data
  })
}

export const barInfoApi = async (id): Promise<IResponse> => {
  return request.get({ url: `/dataset-tree/bar-info/${id}`, data: {} }).then(res => {
    return res?.data
  })
}

export const delDatasetTree = async (id): Promise<IResponse> => {
  return request.delete({ url: `/dataset-tree/${id}`, data: {} }).then(res => {
    return res?.data
  })
}

// 导出数据集文件。
export const exportDatasetData = (data = {}) => {
  return request.post({
    url: '/dataset-tree/export-dataset',
    method: 'post',
    data: data,
    loading: true,
    responseType: 'blob'
  })
}

export const exportLimit = async (): Promise<boolean> => {
  return request.post({ url: `/export-center/export-limit`, data: {} }).then(res => {
    return res?.data
  })
}

export const perDelete = async (id): Promise<boolean> => {
  return request.post({ url: `/dataset-tree/deletion-impact/${id}`, data: {} }).then(res => {
    return res?.data
  })
}

export const datasourceList = async (weight?: number): Promise<IResponse> => {
  const data = { busiFlag: 'datasource' }
  if (weight) {
    data['weight'] = weight
  }
  return request.post({ url: '/datasource/tree', data }).then(res => {
    return res?.data
  })
}

export const tables = async (data): Promise<Table[]> => {
  return request.post({ url: `/datasource/tables`, data }).then(res => {
    return res?.data
  })
}

export const getTableField = async (data): Promise<IResponse> => {
  return request.post({ url: '/dataset-data/table-fields', data }).then(res => {
    return res?.data
  })
}

export const getPreviewData = async (data): Promise<IResponse> => {
  const copyData = cloneDeep(data)
  originNameHandle(copyData.allFields)
  return request.post({ url: '/dataset-data/preview-data', data: copyData }).then(res => {
    if (res?.data?.allFields?.length) {
      originNameHandleBack(res?.data?.allFields)
    }

    if (res?.data?.data?.fields?.length) {
      originNameHandleBack(res?.data?.data?.fields)
    }
    return res?.data
  })
}

export const datasetPreview = async (id): Promise<FieldData> => {
  return request.post({ url: `/dataset-tree/detail/${id}`, data: {} }).then(res => {
    return res?.data
  })
}

export const datasetTotal = async (id): Promise<FieldData> => {
  return request.post({ url: `/dataset-data/dataset-total`, data: { id: id } }).then(res => {
    return res?.data
  })
}

export const datasetDetails = async (id): Promise<Dataset> => {
  return request.post({ url: `/dataset-tree/details/${id}`, data: {} }).then(res => {
    if (res?.data?.allFields?.length) {
      originNameHandleBack(res?.data?.allFields)
    }
    return res?.data
  })
}

export const tableUpdate = async (data): Promise<IResponse> => {
  return request.put({ url: '/dataset/table', data }).then(res => {
    return res?.data
  })
}

export const getPreviewSql = async (data): Promise<IResponse> => {
  return request.post({ url: '/dataset-data/preview-sql', data }).then(res => {
    return res?.data
  })
}

export const getDsDetails = async (data): Promise<DatasetDetail[]> => {
  return request.post({ url: '/dataset-tree/dataset-details', data }).then(res => {
    return res?.data
  })
}
export const getDsDetailsWithPerm = async (data): Promise<DatasetDetail[]> => {
  return request.post({ url: '/dataset-tree/detail-with-permission', data }).then(res => {
    ;(res?.data || []).forEach(ele => {
      originNameHandleBackWithArr(ele, ['dimensionList', 'quotaList'])
    })
    return res?.data
  })
}
export const sqlParams = async (data): Promise<ParamsDetail[]> => {
  return request.post({ url: '/dataset-tree/sql-params', data }).then(res => {
    return res?.data
  })
}

export const datasetSyncTask = async (datasetGroupId): Promise<DatasetSyncTask> => {
  return request.get({ url: `/dataset-sync/task/${datasetGroupId}` }).then(res => {
    return res?.data
  })
}

export const datasetSyncTaskPage = async (
  page: number,
  pageSize: number,
  data: DatasetSyncTaskRequest = {}
): Promise<DatasetSyncTaskPage> => {
  return request.post({ url: `/dataset-sync/tasks/${page}/${pageSize}`, data }).then(res => {
    return res?.data
  })
}

export const datasetSyncDependencies = async (
  visualizationId: string | number
): Promise<DatasetSyncTask[]> => {
  return request.get({ url: `/dataset-sync/dependencies/${visualizationId}` }).then(res => {
    return res?.data || []
  })
}

export const saveDatasetSyncTask = async (data: DatasetSyncTask): Promise<DatasetSyncTask> => {
  return request.post({ url: '/dataset-sync/record', data }).then(res => {
    return res?.data
  })
}

export const executeDatasetSync = async (datasetGroupId): Promise<DatasetSyncTask> => {
  return request.post({ url: `/dataset-sync/runs/${datasetGroupId}`, data: {} }).then(res => {
    return res?.data
  })
}

export const stopDatasetSync = async (datasetGroupId): Promise<void> => {
  return request
    .post({ url: `/dataset-sync/lifecycle/stop/${datasetGroupId}`, data: {} })
    .then(res => {
      return res?.data
    })
}

export const pauseDatasetSync = async (datasetGroupId): Promise<DatasetSyncTask> => {
  return request
    .post({ url: `/dataset-sync/lifecycle/pause/${datasetGroupId}`, data: {} })
    .then(res => {
      return res?.data
    })
}

export const resumeDatasetSync = async (datasetGroupId): Promise<DatasetSyncTask> => {
  return request
    .post({ url: `/dataset-sync/lifecycle/resume/${datasetGroupId}`, data: {} })
    .then(res => {
      return res?.data
    })
}

export const retryDatasetSync = async (datasetGroupId): Promise<DatasetSyncTask> => {
  return request
    .post({ url: `/dataset-sync/lifecycle/retry/${datasetGroupId}`, data: {} })
    .then(res => {
      return res?.data
    })
}
export const datasetSyncLogs = async (datasetGroupId): Promise<DatasetSyncLog[]> => {
  return request.get({ url: `/dataset-sync/logs/${datasetGroupId}` }).then(res => {
    return res?.data || []
  })
}
// 分页查询行权限规则。
export const rowPermissionList = (page: number, limit: number, datasetId: number) =>
  request.get({ url: '/dataset/row-permissions/page/' + datasetId + '/' + page + '/' + limit })

// 分页查询列权限规则。
export const columnPermissionList = (page: number, limit: number, datasetId: number) =>
  request.get({ url: '/dataset/column-permissions/page/' + datasetId + '/' + page + '/' + limit })

// 查询行权限可授权对象。
export const rowPermissionTargetObjList = (datasetId: number, type: string) =>
  request.get({ url: '/dataset/row-permissions/authorized-objects/' + datasetId + '/' + type })

// 查询数据集字段列表。
export const listFieldByDatasetGroup = (datasetId: number) => {
  return request.post({ url: '/dataset-field/by-dataset-group/' + datasetId }).then(res => {
    originNameHandleBack(res?.data)
    return res
  })
}

// 批量查询权限字段取值。
export const multFieldValuesForPermissions = (data = {}) => {
  return request.post({ url: '/dataset-field/permission-field-values', data })
}

// 查询带权限信息的数据集字段。
export const listFieldsWithPermissions = (datasetId: number) => {
  return request.get({ url: '/dataset-field/with-permissions/' + datasetId }).then(res => {
    originNameHandleBack(res?.data)
    return res
  })
}

// 查询智能助手可用字段。
export const copilotFields = (datasetId: number) => {
  return request.post({ url: '/dataset-field/copilot-fields/' + datasetId })
}

// 保存行权限规则。
export const saveRowPermission = (data = {}) => {
  return request.post({ url: '/dataset/row-permissions/record', data })
}

// 保存列权限规则。
export const saveColumnPermission = (data = {}) => {
  return request.post({ url: '/dataset/column-permissions/record', data })
}

// 删除行权限规则。
export const deleteRowPermission = (data = {}) => {
  return request.delete({ url: '/dataset/row-permissions', data })
}

// 删除列权限规则。
export const deleteColumnPermission = (data = {}) => {
  return request.delete({ url: '/dataset/column-permissions', data })
}

// 查询行权限白名单用户。
export const whiteListUsersForPermissions = (data = {}) => {
  return request.post({ url: '/dataset/row-permissions/allowlist-users', data })
}

export const saveField = async (data): Promise<DatasetDetail[]> => {
  return request.post({ url: '/dataset-field/record', data }).then(res => {
    return res?.data
  })
}

export const deleteField = async (id): Promise<DatasetDetail[]> => {
  return request.delete({ url: `/dataset-field/${id}`, data: {} }).then(res => {
    return res?.data
  })
}

export const chartFieldRemovalId = async (id): Promise<DatasetDetail[]> => {
  return request.delete({ url: `/dataset-field/charts/${id}`, data: {} }).then(res => {
    return res?.data
  })
}

export const getEnumValue = async (data): Promise<DatasetDetail[]> => {
  return request.post({ url: '/dataset-data/enum-values', data }).then(res => {
    return res?.data
  })
}

export const getFunction = async (): Promise<DatasetDetail[]> => {
  return request.post({ url: '/dataset-field/functions', data: {} }).then(res => {
    return res?.data
  })
}

// 查询导出任务统计记录。
export const exportTasksRecords = () =>
  request.post({ url: `/export-center/export-tasks/records`, data: {} })

// 分页查询导出任务列表。
export const exportTasks = (page = 1, limit = 10, status = 'ALL') =>
  request.post({ url: `/export-center/export-tasks/${status}/${page}/${limit}`, data: {} })

export const exportRetry = async (id): Promise<IResponse> => {
  return request.post({ url: '/export-center/retry/' + id, data: {} }).then(res => {
    return res?.data
  })
}

export const downloadFile = async (id): Promise<Blob> => {
  return request.get({ url: 'export-center/download/' + id, responseType: 'blob' }).then(res => {
    return res?.data
  })
}

export const exportDelete = async (id): Promise<IResponse> => {
  return request.delete({ url: '/export-center/' + id }).then(res => {
    return res?.data
  })
}

export const generateDownloadUri = async (id): Promise<IResponse> => {
  return request.get({ url: '/export-center/download-tickets/' + id }).then(res => {
    return res?.data
  })
}

export const exportDeleteAll = async (type, data): Promise<IResponse> => {
  return request.delete({ url: '/export-center/all/' + type, data }).then(res => {
    return res?.data
  })
}

export const exportDeletePost = async (data): Promise<IResponse> => {
  return request.delete({ url: '/export-center', data }).then(res => {
    return res?.data
  })
}

export const listByDsIds = async (data): Promise<IResponse> => {
  return request.post({ url: 'dataset-field/by-dataset-ids', data }).then(res => {
    return res?.data
  })
}

export const fieldTree = async (data): Promise<IResponse> => {
  return request.post({ url: 'dataset-data/field-tree', data }).then(res => {
    return res?.data
  })
}

export const copilotChat = async (data): Promise<IResponse> => {
  return request.post({ url: '/copilot/chat', data }).then(res => {
    return res?.data
  })
}

export const getListCopilot = async (): Promise<IResponse> => {
  return request.post({ url: '/copilot/list' }).then(res => {
    return res?.data
  })
}

export const clearAllCopilot = async (): Promise<IResponse> => {
  return request.post({ url: '/copilot/all/clear' }).then(res => {
    return res?.data
  })
}
