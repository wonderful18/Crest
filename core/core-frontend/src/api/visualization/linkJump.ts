import request from '@/config/axios'

export function getTableFieldWithViewId(viewId) {
  return request.get({
    url: '/link-jump/table-fields/by-view/' + viewId
  })
}
export function queryWithViewId(dvId, viewId) {
  return request.get({
    url: '/link-jump/by-view/' + dvId + '/' + viewId
  })
}

export function updateJumpSet(requestInfo) {
  return request.post({
    url: '/link-jump/jump-set',
    data: requestInfo,
    loading: true
  })
}
export function queryTargetVisualizationJumpInfo(requestInfo) {
  return request.post({
    url: '/link-jump/target-visualization-jump-info',
    data: requestInfo,
    loading: true
  })
}

export function queryVisualizationJumpInfo(dvId, resourceTable = 'snapshot') {
  return request.get({
    url: '/link-jump/visualization-jump-info/' + dvId + '/' + resourceTable,
    loading: false
  })
}

export function viewTableDetailList(dvId, resourceTable = 'snapshot') {
  return request.get({
    url: '/link-jump/view-table-detail-list/' + dvId + '/' + resourceTable,
    loading: false
  })
}

export function updateJumpSetActive(requestInfo) {
  return request.post({
    url: '/link-jump/jump-set-active',
    data: requestInfo,
    loading: true
  })
}

export function deleteJumpSet(requestInfo) {
  return request.delete({
    url: '/link-jump/jump-set',
    data: requestInfo,
    loading: true
  })
}
