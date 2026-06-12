import {
  getDatasetSyncStatusText,
  normalizeDatasetSyncInfo
} from '../dataset/form/datasetSyncWorkflow.mjs'

export const cacheTaskStatusText = task =>
  getDatasetSyncStatusText({
    ...task,
    syncMode: 1,
    hasDatasetId: true
  })

export const cacheTaskActions = task => {
  const running = task?.taskStatus === 'UnderExecution'
  const suspended = task?.taskStatus === 'Suspend'
  const scheduled = !!task?.syncRate && task.syncRate !== 'RIGHTNOW'
  const failed = task?.lastExecStatus === 'Error' || task?.lastLogStatus === 'Error'
  return {
    canExecute: !running,
    canPause: !suspended && (scheduled || running),
    canResume: suspended,
    canRetry: failed && !running
  }
}

export const cacheTaskRunningSnapshot = (task, now = Date.now()) => ({
  ...task,
  taskStatus: 'UnderExecution',
  lastLogStatus: 'UnderExecution',
  lastLogStartTime: now,
  lastLogEndTime: null,
  durationMillis: 0,
  failureReason: null
})

export const shouldContinueCacheTaskPolling = ({ task, attempts, settleAttempts = 2 }) => {
  if (attempts < settleAttempts) {
    return true
  }
  return cacheTaskStatusText(task) === '更新中'
}

export const resolveCacheTaskPollingError = ({ attempts, maxAttempts }) => {
  if (attempts >= maxAttempts) {
    return {
      shouldRetry: false,
      message: '状态刷新失败，请手动刷新'
    }
  }
  return {
    shouldRetry: true,
    message: ''
  }
}

export const formatCacheTaskDuration = durationMillis => {
  if (durationMillis === null || durationMillis === undefined) {
    return '-'
  }
  const seconds = Math.max(0, Math.round(Number(durationMillis) / 100) / 10)
  if (seconds < 60) {
    return `${seconds} 秒`
  }
  const minutes = Math.floor(seconds / 60)
  const restSeconds = Math.round(seconds % 60)
  return restSeconds ? `${minutes} 分 ${restSeconds} 秒` : `${minutes} 分`
}

export const cacheTaskFailureReason = task => {
  if (task?.failureReason) {
    return normalizeDatasetSyncInfo(task.failureReason)
  }
  if (cacheTaskStatusText(task) !== '更新失败') {
    return '-'
  }
  return normalizeDatasetSyncInfo(task?.lastLogInfo || task?.lastVerifyMessage || '-')
}
