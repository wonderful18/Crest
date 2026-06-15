import request from '@/config/axios'

interface IResourceCount {
  jobCount: number
  datasourceCount: number
  jobLogCount: number
}

// 读取当前业务数据并返回处理结果
export const getResourceCount = () => {
  return request
    .get({
      url: 'sync/summary/resource-count',
      method: 'get'
    })
    .then(res => {
      return res.data as IResourceCount
    })
}

// 读取当前业务数据并返回处理结果
export const getJobLogLienChartInfo = () => {
  return request.post({
    url: '/sync/summary/log-chart-data',
    method: 'post',
    data: ''
  })
}
