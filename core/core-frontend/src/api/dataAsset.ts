import request from '@/config/axios'

export interface DataAssetRequest {
  keyword?: string
  assetType?: string
  certified?: boolean
  recommended?: boolean
  deprecated?: boolean
  ownerId?: string | number
}

export interface DataAsset {
  assetType: string
  assetTypeLabel?: string
  assetId: string
  name: string
  extraType?: string
  parentAssetType?: string
  parentAssetId?: string
  description?: string
  tags?: string
  certified?: boolean
  recommended?: boolean
  deprecated?: boolean
  canManage?: boolean
  upstreamCount?: number
  downstreamCount?: number
  ownerId?: string | number
  ownerName?: string
  creatorId?: string | number
  creatorName?: string
  orgId?: string | number
  orgName?: string
  createTime?: number
  updateTime?: number
}

export interface DataAssetImpactItem {
  assetType: string
  assetTypeLabel?: string
  assetId: string
  name: string
  relation?: string
  updateTime?: number
}

export interface DataAssetImpact {
  assetType: string
  assetId: string
  summary: Record<string, number>
  items: DataAssetImpactItem[]
}

export interface DataAssetDetail {
  asset: DataAsset
  upstream: DataAssetImpactItem[]
  downstream: DataAssetImpactItem[]
  impact: DataAssetImpact
}

export interface DataAssetOwner {
  id: string | number
  name: string
  account?: string
}

export interface DataAssetProfileRequest {
  assetType: string
  assetId: string
  description?: string
  tags?: string
  ownerId?: string | number | null
  certified?: boolean
  recommended?: boolean
  deprecated?: boolean
}

export const dataAssetPageApi = (page: number, pageSize: number, data: DataAssetRequest = {}) =>
  request.post({ url: `/data-assets/page/${page}/${pageSize}`, data })

export const dataAssetDetailApi = (assetType: string, assetId: string | number) =>
  request.get({ url: `/data-assets/${assetType}/${assetId}` })

export const dataAssetImpactApi = (assetType: string, assetId: string | number) =>
  request.get({ url: `/data-assets/${assetType}/${assetId}/impact` })

export const saveDataAssetProfileApi = (data: DataAssetProfileRequest) =>
  request.post({ url: '/data-assets/profile', data })

export const dataAssetOwnersApi = () => request.get({ url: '/data-assets/owners' })
