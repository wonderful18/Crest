import request from '@/config/axios'

// 读取当前业务数据并返回处理结果
export const queryVisualizationBackground = () =>
  request.get({ url: '/visualization-background/list' })
