import request from '@/config/axios'

export const mountedOrg = (keyword?: string) =>
  request.post({ url: '/org/mounted', data: { keyword } })

export const switchOrg = (id: number | string) => request.post({ url: `/user/switch/${id}` })

export const userInfo = () => request.get({ url: '/user/info' })

export const searchRoleApi = (keyword: string) =>
  request.post({ url: '/role/list', data: { keyword } })

export const userOptionForRoleApi = data => request.post({ url: '/user/role/option', data })

export const userSelectedForRoleApi = (page: number, limit: number, data) =>
  request.post({ url: `/user/role/selected/${page}/${limit}`, data })

export const userPageApi = (page: number, limit: number, data) =>
  request.post({ url: `/user/page/${page}/${limit}`, data })

export const personSysVariableInfoApi = uid =>
  request.get({ url: `/user/person-sys-variable-info/${uid}` })

export const userCreateApi = data => request.post({ url: '/user', data })

export const userEditApi = data => request.put({ url: '/user', data })

export const personEditApi = data => request.post({ url: '/user/person-edit', data })

export const roleOptionForUserApi = data => request.post({ url: '/role/user/option', data })

export const userDelApi = uid => request.delete({ url: `/user/${uid}` })

export const queryFormApi = uid => request.get({ url: `/user/detail/${uid}` })

export const personInfoApi = () => request.get({ url: `/user/person-info` })

export const ipInfoApi = () => request.get({ url: `/user/ip-info` })

export const roleCreateApi = data => request.post({ url: '/role', data })

export const roleEditApi = data => request.put({ url: '/role', data })

export const roleDetailApi = rid => request.get({ url: `/role/detail/${rid}` })

export const roleDelApi = rid => request.delete({ url: `/role/${rid}` })

export const beforeUnmountInfoApi = data => request.post({ url: '/role/unmount-info', data })

export const unMountUserApi = data => request.post({ url: '/role/users/unmount', data })

export const mountUserApi = data => request.post({ url: '/role/users', data })

export const searchExternalUserApi = keyword =>
  request.get({ url: '/role/external-users/search/' + keyword })

export const mountExternalUserApi = data => request.post({ url: '/role/external-users', data })

export const switchLangApi = data => request.post({ url: '/user/switch-language', data })

export const downExcelTemplateApi = () =>
  request.post({ url: '/user/excel-template', responseType: 'blob' })

export const importUserApi = data =>
  request.post({
    url: '/user/batch-import',
    headersType: 'multipart/form-data',
    data
  })

export const downErrorRecordApi = (key: string) =>
  request.get({ url: `/user/error-record/${key}`, responseType: 'blob' })

export const clearErrorApi = (key: string) => {
  request.get({ url: `/user/clear-error-record/${key}` })
}

export const batchDelApi = data => request.delete({ url: '/user/batch', data })

export const resetPwdApi = uid => request.post({ url: `/user/reset-password/${uid}` })

export const switchEnableApi = data => request.post({ url: '/user/enable', data })
