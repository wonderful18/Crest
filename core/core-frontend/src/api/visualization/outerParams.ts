import request from '@/config/axios'

// 读取当前业务数据并返回处理结果
export function queryWithVisualizationId(dvId) {
  return request.get({
    url: '/outer-params/by-visualization/' + dvId
  })
}

// 更新当前配置并同步相关状态
export function updateOuterParamsSet(requestInfo) {
  return request.post({
    url: '/outer-params/outer-params-settings',
    data: requestInfo,
    loading: true
  })
}

// 读取当前业务数据并返回处理结果
export function getOuterParamsInfo(dvId) {
  return request.get({
    url: '/outer-params/outer-params-info/' + dvId,
    method: 'get',
    loading: false
  })
}
