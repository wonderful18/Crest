import { service } from './service'

import { config } from './config'

const { default_headers } = config
const AXIOS_METHOD = {
  GET: 'get',
  POST: 'post',
  DELETE: 'delete',
  PUT: 'put'
} as const

const request = (option: any) => {
  const { url, method, params, data, headersType, responseType, loading } = option
  return service({
    url: url,
    method,
    loading,
    params,
    data,
    responseType: responseType,
    headers: {
      'Content-Type': headersType || default_headers
    }
  })
}

export default {
  get: <T = any>(option: any) => {
    return request({ method: AXIOS_METHOD.GET, ...option }) as unknown as T
  },
  post: <T = any>(option: any) => {
    return request({ method: AXIOS_METHOD.POST, ...option }) as unknown as T
  },
  delete: <T = any>(option: any) => {
    return request({ method: AXIOS_METHOD.DELETE, ...option }) as unknown as T
  },
  put: <T = any>(option: any) => {
    return request({ method: AXIOS_METHOD.PUT, ...option }) as unknown as T
  }
}
