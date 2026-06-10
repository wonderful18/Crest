import request from '@/config/axios'

export const variableCreateApi = data => request.post({ url: '/sys-variable', data })

export const variableEditApi = data => request.put({ url: '/sys-variable', data })

export const variableDetailApi = id => request.get({ url: '/sys-variable/detail/' + id })

export const variableDeletelApi = id => request.delete({ url: '/sys-variable/' + id })

export const searchVariableApi = async data => request.post({ url: '/sys-variable/list', data })

export const valueSelectedForVariableApi = (page: number, limit: number, data) =>
  request.post({ url: `/sys-variable/value/selected/${page}/${limit}`, data })

export const valueForVariable = id => request.get({ url: '/sys-variable/value/selected/' + id })

export const variableValueCreateApi = data => request.post({ url: '/sys-variable/value', data })

export const variableValueDeletelApi = id => request.delete({ url: '/sys-variable/value/' + id })

export const variableValueEditApi = data => request.put({ url: '/sys-variable/value', data })

export const batchDelApi = data => request.delete({ url: '/sys-variable/value/batch', data })
