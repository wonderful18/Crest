import request from '@/config/axios'

export const viewLinkageGather = data => request.post({ url: '/linkage/linkage-gather', data })

export const viewLinkageGatherArray = data =>
  request.post({ url: '/linkage/linkage-gather-array', data })

export const saveLinkage = data => request.post({ url: '/linkage/linkage', data })

export const getPanelAllLinkageInfo = (dvId, resourceTable = 'snapshot') =>
  request.get({ url: '/linkage/visualization-linkage-info/' + dvId + '/' + resourceTable })

export const updateLinkageActive = data =>
  request.post({ url: '/linkage/linkage-active-state', data })

export const deleteLinkage = data => request.delete({ url: '/linkage/linkage', data })
