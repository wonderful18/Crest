import request from '@/config/axios'

// 读取当前业务数据并返回处理结果
export function queryAll() {
  return request.get({
    url: '/pdf-template/all',
    loading: false
  })
}
