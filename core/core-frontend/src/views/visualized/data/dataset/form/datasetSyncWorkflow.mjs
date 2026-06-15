// 根据数据集缓存配置构建保存任务载荷
export const buildDatasetSyncTaskPayload = ({ syncTask, datasetGroupId, datasetName }) => ({
  id: syncTask.id,
  datasetGroupId,
  name: datasetName,
  updateType: syncTask.updateType,
  incrementalFieldId: syncTask.updateType === 'add_scope' ? syncTask.incrementalFieldId : null,
  startTime: syncTask.startTime,
  syncRate: syncTask.syncRate,
  cron: syncTask.cron,
  simpleCronValue: syncTask.simpleCronValue,
  simpleCronType: syncTask.simpleCronType,
  endTime: syncTask.endTime,
  fullSyncIntervalHours: syncTask.fullSyncIntervalHours,
  verifyEnabled: syncTask.verifyEnabled,
  cacheExpireHours: syncTask.cacheExpireHours,
  taskTimeoutMinutes: syncTask.taskTimeoutMinutes,
  failureWarnThreshold: syncTask.failureWarnThreshold
})

// 格式化同步日志影响行数
export const formatDatasetSyncRowCount = log => {
  if (!log || log.taskStatus === 'UnderExecution') {
    return ''
  }
  if (log.rowCount === null || log.rowCount === undefined) {
    return ''
  }
  return `${log.rowCount} 行`
}

// 标准化后端同步日志描述，统一页面展示文案
export const normalizeDatasetSyncInfo = info => {
  if (!info) {
    return ''
  }
  return String(info)
    .replaceAll('对账通过', '源数据与缓存一致')
    .replaceAll('对账执行失败', '数据一致性校验失败')
}

// 根据同步任务状态计算页面状态文案
export const getDatasetSyncStatusText = syncTask => {
  if (syncTask?.syncMode !== 1) return '未开启'
  if (!syncTask?.hasDatasetId) return '待保存'
  if (syncTask?.taskStatus === 'UnderExecution') return '更新中'
  if (syncTask?.failureWarned) return '更新失败'
  if (syncTask?.lastVerifyStatus === 'WARNING') return '更新失败'
  if (syncTask?.lastExecStatus === 'Completed') {
    return syncTask?.cacheReady === 1 && !syncTask?.cacheExpired ? '缓存可用' : '待更新'
  }
  if (syncTask?.lastExecStatus === 'Error') return '更新失败'
  return '待更新'
}

// 计算立即执行缓存更新按钮的禁用原因
export const getDatasetSyncNowDisabledReason = ({ hasDatasetId, syncRunning }) => {
  if (!hasDatasetId) return '请先保存数据集，再执行缓存更新'
  if (syncRunning) return '缓存更新正在执行'
  return ''
}

// 计算增量更新不可用原因
export const getIncrementalDisabledReason = incrementalFieldOptions => {
  return Array.isArray(incrementalFieldOptions) && incrementalFieldOptions.length
    ? ''
    : '当前没有可用增量字段，请使用全量更新'
}

// 执行立即更新缓存的完整流程
export const executeDatasetSyncNowFlow = async ({
  currentDatasetId,
  syncRunning,
  warn,
  saveCurrentDataset,
  persistDatasetSync,
  executeDatasetSync,
  loadDatasetSyncLogs,
  applyTask,
  applyLogs,
  startSyncPolling,
  success
}) => {
  if (!currentDatasetId) {
    warn('请先保存数据集')
    return { submitted: false, reason: 'missingDatasetId' }
  }
  if (syncRunning) {
    warn('缓存更新正在执行')
    return { submitted: false, reason: 'running' }
  }

  await saveCurrentDataset(currentDatasetId)
  await persistDatasetSync(currentDatasetId)
  const task = await executeDatasetSync(currentDatasetId)
  if (task) {
    applyTask(task)
  }
  const logs = await loadDatasetSyncLogs(currentDatasetId)
  applyLogs(logs)
  startSyncPolling()
  success('缓存更新已提交')
  return { submitted: true, task, logs }
}
