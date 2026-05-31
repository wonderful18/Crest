import request from '@/config/axios'

export const findBaseParams = async () => request.get({ url: '/ai-base/target-url' })
