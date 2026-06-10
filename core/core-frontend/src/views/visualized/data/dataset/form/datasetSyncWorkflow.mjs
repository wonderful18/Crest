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

export const formatDatasetSyncRowCount = log => {
  if (!log || log.taskStatus === 'UnderExecution') {
    return ''
  }
  if (log.rowCount === null || log.rowCount === undefined) {
    return ''
  }
  return `${log.rowCount} 行`
}

export const normalizeDatasetSyncInfo = info => {
  if (!info) {
    return ''
  }
  return String(info)
    .replaceAll('对账通过', '源数据与缓存一致')
    .replaceAll('对账执行失败', '数据一致性校验失败')
}

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
    warn('已有同步任务正在执行')
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
  success('同步任务已提交')
  return { submitted: true, task, logs }
}
