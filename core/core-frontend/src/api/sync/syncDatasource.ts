import request from '@/config/axios'

export const sourceDsPageApi = (page: number, limit: number, data) => {
  return request.post({ url: `/sync/datasource/source/page/${page}/${limit}`, data })
}

export const targetDsPageApi = (page: number, limit: number, data) => {
  return request.post({ url: `/sync/datasource/target/page/${page}/${limit}`, data })
}
export const latestUseApi = (sourceType: string) => {
  return request.post({ url: `/sync/datasource/latest-use/${sourceType}`, data: {} })
}

export const validateApi = data => {
  return request.post({ url: '/sync/datasource/validate', data })
}

export const getSchemaApi = data => {
  return request.post({ url: '/sync/datasource/schemas', data })
}

export const saveApi = data => {
  return request.post({ url: '/sync/datasource/record', data })
}

export const getByIdApi = (id: string) => {
  return request.get({ url: `/sync/datasource/detail/${id}` })
}

export const updateApi = data => {
  return request.put({ url: '/sync/datasource', data })
}

export const deleteByIdApi = (id: string) => {
  return request.delete({ url: `/sync/datasource/${id}` })
}

export const batchDelApi = (ids: string[]) => {
  return request.delete({ url: `/sync/datasource/batch`, data: ids })
}

/**
 * 获取源数据库字段集合以及目标数据库数据类型集合
 */
export const getFieldListApi = data => {
  return request.post({ url: `/sync/datasource/fields`, data })
}

export const validateByIdApi = (id: string) => {
  return request.get({ url: `/sync/datasource/validate/${id}` })
}
