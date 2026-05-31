import request from '@/config/axios'

export const searchApi = data => request.post({ url: '/org/page/tree', data })
export const saveApi = data => request.post({ url: '/org/page', data })
export const updateApi = data => request.put({ url: '/org/page', data })
export const resourceExistApi = oid => request.get({ url: '/org/resource-exists/' + oid })
export const deleteApi = oid => request.delete({ url: '/org/page/' + oid })
