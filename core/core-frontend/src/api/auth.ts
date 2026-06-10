import request from '@/config/axios'

export const queryUserApi = data => request.post({ url: '/user/by-current-org', data })
export const queryUserOptionsApi = () => request.get({ url: '/user/org/option' })
export const queryRoleApi = data => request.post({ url: '/role/by-current-org', data })

export const resourceTreeApi = (flag: string) =>
  request.get({ url: '/auth/business-resources/' + flag })

export const menuTreeApi = () => request.get({ url: '/auth/menu-resources' })

export const resourcePerApi = data => request.post({ url: '/auth/business-permissions', data })

export const menuPerApi = data => request.post({ url: '/auth/menu-permissions', data })

export const busiPerSaveApi = data => request.put({ url: '/auth/business-permissions', data })
export const menuPerSaveApi = data => request.put({ url: '/auth/menu-permissions', data })

export const resourceTargetPerApi = data =>
  request.post({ url: '/auth/business-target-permissions', data })

export const menuTargetPerApi = data => request.post({ url: '/auth/menu-target-permissions', data })

export const busiTargetPerSaveApi = data =>
  request.put({ url: '/auth/business-target-permissions', data })
export const menuTargetPerSaveApi = data =>
  request.put({ url: '/auth/menu-target-permissions', data })
