import request from '@/config/axios'

export const loginApi = data => request.post({ url: '/login/local-login', data })

export const queryPublicKey = () => request.get({ url: 'public-key' })

export const querySymmetricKey = () => request.get({ url: 'symmetric-key' })

export const modelApi = () => request.get({ url: 'model' })

export const platformLoginApi = origin => request.post({ url: '/login/platform-login/' + origin })

export const logoutApi = () => request.get({ url: '/logout' })

export const refreshApi = (time?: any) => request.get({ url: '/login/refresh', params: { time } })

export const uiLoadApi = () => request.get({ url: '/sys-parameter/ui' })

export const loginCategoryApi = () => request.get({ url: '/sys-parameter/default-login' })

export const ssoStatusApi = () => request.get({ url: '/sso/public/status' })

export const ssoTokenApi = (ticket: string) => request.get({ url: `/sso/token/${ticket}` })
