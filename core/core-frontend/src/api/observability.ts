import request from '@/config/axios'

export const observabilityStatusApi = () => request.get({ url: '/observability/status' })
