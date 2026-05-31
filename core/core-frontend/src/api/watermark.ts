import request from '@/config/axios'

export const watermarkSave = params => request.post({ url: '/watermark/record', data: params })

export const watermarkFind = async () => request.get({ url: 'watermark/list' })
