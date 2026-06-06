import request from '@/config/axios'

export interface DataPortalResourceRequest {
  keyword?: string
  type?: string
  page?: number
  pageSize?: number
  asc?: boolean
}

export const portalOverviewApi = () => request.get({ url: '/data-portal/overview' })

export const portalResourcesApi = (data: DataPortalResourceRequest) =>
  request.post({ url: '/data-portal/resources', data })

export const portalResourceApi = (id: string | number) =>
  request.get({ url: `/data-portal/resource/${id}` })
