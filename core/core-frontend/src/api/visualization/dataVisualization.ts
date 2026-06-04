import request from '@/config/axios'
import type { BusiTreeRequest } from '@/models/tree/TreeNode'
import { originNameHandleWithArr } from '@/utils/CalculateFields'
import { cloneDeep } from 'lodash-es'
export interface ResourceOrFolder {
  name: string
  id?: number | string
  pid?: number | string
  nodeType: 'folder' | 'leaf'
  type: string
  mobileLayout: boolean
  status: boolean
}

export interface Panel {
  name: string
  type: string
  updateTime: number
  createBy: string
  updateBy: string
}

export const findCopyResource = async (dvId, busiFlag): Promise<IResponse> => {
  return request.get({ url: '/data-visualization/copy-resource/' + dvId + '/' + busiFlag })
}

export const findById = async (
  dvId,
  busiFlag,
  attachInfo = { source: 'main', taskId: null }
): Promise<IResponse> => {
  let busiFlagResult = busiFlag
  if (!busiFlagResult) {
    await findDvType(dvId).then(res => {
      busiFlagResult = res.data
    })
  }
  const data = { id: dvId, busiFlag: busiFlagResult, ...attachInfo }
  return request.post({ url: '/data-visualization/detail', data })
}

export const updateCheckVersion = dvId =>
  request.get({ url: `/data-visualization/check-version/${dvId}` })

export const queryTreeApi = async (data: BusiTreeRequest): Promise<IResponse> => {
  return request.post({ url: '/data-visualization/tree', data }).then(res => {
    return res?.data
  })
}

export const queryBusiTreeApi = async (data): Promise<IResponse> => {
  return request.post({ url: '/data-visualization/interactive-tree', data }).then(res => {
    return res?.data
  })
}

export const findDvType = async dvId =>
  request.get({ url: `/data-visualization/type/${dvId}` })

export const save = data => request.post({ url: '/data-visualization/save', data })

export const checkCanvasChange = data =>
  request.post({ url: '/data-visualization/canvas-change-impact', data, loading: true })

export const saveCanvas = data =>
  request.post({ url: '/data-visualization/canvas', data, loading: true })

export const updatePublishStatus = data =>
  request.post({ url: '/data-visualization/publish-status', data, loading: false })

export const recoverToPublished = data =>
  request.post({ url: '/data-visualization/published-state/recover', data, loading: true })
export const appCanvasNameCheck = async data =>
  request.post({ url: '/data-visualization/canvas-name-check', data, loading: false })

export const updateBase = data => request.put({ url: '/data-visualization/base', data })

export const updateCanvas = data => {
  const copyData = cloneDeep(data)
  const fields = [
    'xAxis',
    'xAxisExt',
    'yAxis',
    'yAxisExt',
    'extBubble',
    'extLabel',
    'extStack',
    'extTooltip',
    'extColor'
  ]

  for (const key in copyData.canvasViewInfo) {
    originNameHandleWithArr(copyData.canvasViewInfo[key], fields)
  }
  return request.put({ url: '/data-visualization/canvas', data: copyData, loading: true })
}

export const moveResource = data => request.post({ url: '/data-visualization/move', data })

export const copyResource = data => request.post({ url: '/data-visualization/copy', data })

export const deleteLogic = (dvId, busiFlag) =>
  request.delete({ url: '/data-visualization/trash/' + dvId + '/' + busiFlag })

export const querySubjectWithGroupApi = data =>
  request.post({ url: '/visualization-subject/subjects-with-groups', data })

export const saveOrUpdateSubject = data =>
  request.put({ url: '/visualization-subject', data })

export const deleteSubject = id => request.delete({ url: '/visualization-subject/' + id })

export const dvNameCheck = async data => request.post({ url: '/data-visualization/name-check', data })

export const storeApi = (data): Promise<IResponse> => {
  return request.post({ url: '/store/runs', data })
}

export const storeStatusApi = (id: string): Promise<IResponse> => {
  return request.get({ url: `/store/favorited/${id}` })
}

export const decompression = async data =>
  request.post({ url: '/data-visualization/decompression', data, loading: true })

export const viewDetailList = dvId => {
  return request.get({
    url: '/data-visualization/view-detail-list/' + dvId,
    method: 'get',
    loading: false
  })
}

export const getComponentInfo = dvId => {
  return request.get({
    url: '/panel/view/getComponentInfo/' + dvId,
    loading: false
  })
}

export const export2AppCheck = params => {
  return request.post({
    url: '/data-visualization/app-export-check',
    data: params,
    loading: true
  })
}

export const queryOuterParamsDsInfo = async dvId => {
  return request.get({
    url: '/outer-params/datasources/by-visualization/' + dvId,
    method: 'get',
    loading: false
  })
}

export const queryShareBaseApi = () => {
  return request.get({
    url: '/sys-parameter/share-base',
    loading: false
  })
}

export const exportLogApp = data => request.post({ url: '/data-visualization/export-logs/app', data })

export const exportLogTemplate = data =>
  request.post({ url: '/data-visualization/export-logs/template', data })

export const exportLogPDF = data => request.post({ url: '/data-visualization/export-logs/pdf', data })

export const exportLogImg = data => request.post({ url: '/data-visualization/export-logs/image', data })
