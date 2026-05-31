import request from '@/config/axios'

export interface Font {
  id: string
  name: string
  fileName: string
  fileTransName?: string
  isDefault: boolean
  isBuiltin?: boolean
}

export const list = () => {
  return request.get({ url: '/typeface/fonts' }).then(res => {
    return res?.data
  })
}

export const create = (data = {}) => {
  return request.post({ url: '/typeface', data }).then(res => {
    return res?.data
  })
}

export const edit = (data = {}) => {
  return request.put({ url: '/typeface', data }).then(res => {
    return res?.data
  })
}

export const deleteById = id => {
  return request.delete({ url: '/typeface/' + id, data: {} }).then(res => {
    return res?.data
  })
}

export const defaultFont = () => {
  return request.get({ url: '/typeface/default' }).then(res => {
    return res?.data
  })
}

export const uploadFontFile = async (data): Promise<IResponse> => {
  return request
    .post({
      url: '/typeface/files/upload',
      data,
      loading: true,
      headersType: 'multipart/form-data;'
    })
    .then(res => {
      return res
    })
}
