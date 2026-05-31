import request from '@/config/axios'
import { nameTrim } from '@/utils/utils'

export interface DatasetOrFolder {
  name: string
  id?: number | string
  pid?: number | string
  nodeType: 'folder' | 'dataset'
  union?: Array<{}>
  allFields?: Array<{}>
}

interface Fields {
  fields: Array<{}>
  data: Array<{}>
}
export interface FieldData {
  allFields: Array<{}>
  data: Fields
}

export interface Dataset {
  id: string
  pid: string
  name: string
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

export const listDatasources = data => {
  return request
    .post({ url: '/datasource/tree', data: { ...data, ...{ busiFlag: 'datasource' } } })
    .then(res => {
      return res?.data
    })
}

export const listDatasourceType = async (data = {}): Promise<IResponse> => {
  return request.post({ url: '/datasource/types', data }).then(res => {
    return res?.data
  })
}
export const getTableField = (data = {}) => request.post({ url: '/datasource/table-fields', data })

export const syncApiTable = (data = {}) => request.post({ url: '/datasource/api-tables/sync', data })

export const syncApiDs = (data = {}) => request.post({ url: '/datasource/api-data-sources/sync', data })

export const listDatasourceTables = async (data = {}): Promise<IResponse> => {
  return request.post({ url: '/datasource/tables', data }).then(res => {
    return res
  })
}

export const tableStatus = async (data = {}): Promise<IResponse> => {
  return request.post({ url: '/datasource/table-status', data }).then(res => {
    return res
  })
}

export const getSchema = (data = {}) => {
  return request.post({ url: '/datasource/schemas', data })
}

export const previewData = (data = {}) => {
  return request.post({ url: '/datasource/preview-data', data }).then(res => {
    return res?.data
  })
}

export const excelDataPage = (data = {}) => {
  return request.post({ url: '/datasource/excel-data/page', data }).then(res => {
    return res?.data
  })
}

export const saveExcelData = (data = {}) => {
  return request.post({ url: '/datasource/excel-data/record', data, loading: true }).then(res => {
    return res?.data
  })
}

export const validate = (data = {}) => {
  return request.post({ url: '/datasource/validate', data })
}

export const isShowFinishPage = async () => {
  return request.get({ url: '/datasource/finish-page/visible' })
}

export const setShowFinishPage = (data = {}) => {
  return request.post({ url: '/datasource/finish-page/visible', data })
}
export const latestUse = async (data = {}) => {
  return request.post({ url: '/datasource/latest-use', data })
}

export const validateById = (id: number) => request.get({ url: '/datasource/validate/' + id })

export const save = async (data = {}): Promise<Dataset> => {
  nameTrim(data)
  return request.post({ url: '/datasource/record', data }).then(res => {
    return res?.data
  })
}

export const perDeleteDatasource = async (id): Promise<boolean> => {
  return request.post({ url: `/datasource/deletion-impact/${id}`, data: {} }).then(res => {
    return res?.data
  })
}

export const update = async (data = {}): Promise<Dataset> => {
  nameTrim(data)
  return request.put({ url: '/datasource', data }).then(res => {
    return res?.data
  })
}

export const move = async (data = {}): Promise<Dataset> => {
  return request.post({ url: '/datasource/move', data }).then(res => {
    return res?.data
  })
}

export const reName = async (data = {}): Promise<Dataset> => {
  nameTrim(data)
  return request.post({ url: '/datasource/rename', data }).then(res => {
    return res?.data
  })
}

export const createFolder = async (data = {}): Promise<Dataset> => {
  nameTrim(data)
  return request.post({ url: '/datasource/folders', data }).then(res => {
    return res?.data
  })
}

export const checkRepeat = async (data = {}): Promise<Dataset> => {
  return request.post({ url: '/datasource/duplicate-check', data }).then(res => {
    return res?.data
  })
}

export const checkApiItem = async (data = {}): Promise<IResponse> => {
  return request.post({ url: '/datasource/api-data-source-check', data }).then(res => {
    return res
  })
}

export const datasetTree = async (data = {}): Promise<IResponse> => {
  return request
    .post({ url: '/dataset-tree/tree', data: { ...data, ...{ busiFlag: 'dataset' } } })
    .then(res => {
      return res?.data
    })
}

export const getDsTree = async (data = {}): Promise<IResponse> => {
  return request
    .post({ url: '/datasource/tree', data: { ...data, ...{ busiFlag: 'datasource' } } })
    .then(res => {
      return res?.data
    })
}

export const deleteById = (id: number) => request.delete({ url: '/datasource/' + id })

export const getById = (id: number) => request.get({ url: '/datasource/detail/' + id })

export const getHidePwById = (id: number) => request.get({ url: '/datasource/password/masked/' + id })

export const getSimpleDs = (id: number) => request.get({ url: '/datasource/simple-info/' + id })

export const uploadFile = async (data): Promise<IResponse> => {
  return request
    .post({
      url: '/datasource/files/upload',
      data,
      loading: true,
      headersType: 'multipart/form-data;'
    })
    .then(res => {
      return res
    })
}

export const loadRemoteFile = async (data = {}) => {
  return request.post({ url: '/datasource/remote-files/load', data })
}

export const listSyncRecord = (page: number, limit: number, dsId: number | string) =>
  request.post({ url: '/datasource/sync-records/' + dsId + '/' + page + '/' + limit })

export const getEngineDatasource = () => request.get({ url: '/engine/current' })

export const supportSetKey = () => request.get({ url: '/engine/support-set-key' })
