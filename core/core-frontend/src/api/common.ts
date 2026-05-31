import request from '@/config/axios'

// 获取权限路由
export const getRoleRouters = async (): Promise<Array<AppCustomRouteRecordRaw>> => {
  return request.get({ url: '/menu/list' }).then(res => {
    return res?.data
  })
}

// 获取默认排序
export const getDefaultSettings = async (): Promise<IResponse> => {
  return request.get({ url: '/sys-parameter/default-settings' }).then(res => {
    return res?.data
  })
}
