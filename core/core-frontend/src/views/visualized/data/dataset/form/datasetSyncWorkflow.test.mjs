import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  buildDatasetSyncTaskPayload,
  executeDatasetSyncNowFlow,
  formatDatasetSyncRowCount,
  getDatasetSyncNowDisabledReason,
  getDatasetSyncStatusText,
  getIncrementalDisabledReason,
  normalizeDatasetSyncInfo
} from './datasetSyncWorkflow.mjs'

describe('dataset sync workflow', () => {
  it('saves the current dataset before saving the sync task and running it', async () => {
    const calls = []

    const result = await executeDatasetSyncNowFlow({
      currentDatasetId: '982004',
      syncRunning: false,
      warn: message => calls.push(['warn', message]),
      saveCurrentDataset: async id => calls.push(['saveDataset', id]),
      persistDatasetSync: async id => calls.push(['saveTask', id]),
      executeDatasetSync: async id => {
        calls.push(['execute', id])
        return { id: 'task-1' }
      },
      loadDatasetSyncLogs: async id => {
        calls.push(['logs', id])
        return [{ id: 'log-1' }]
      },
      applyTask: task => calls.push(['applyTask', task.id]),
      applyLogs: logs => calls.push(['applyLogs', logs.length]),
      startSyncPolling: () => calls.push(['poll']),
      success: message => calls.push(['success', message])
    })

    assert.equal(result.submitted, true)
    assert.deepEqual(calls, [
      ['saveDataset', '982004'],
      ['saveTask', '982004'],
      ['execute', '982004'],
      ['applyTask', 'task-1'],
      ['logs', '982004'],
      ['applyLogs', 1],
      ['poll'],
      ['success', '缓存更新已提交']
    ])
  })

  it('does not submit when the dataset has not been saved yet', async () => {
    const calls = []

    const result = await executeDatasetSyncNowFlow({
      currentDatasetId: '',
      syncRunning: false,
      warn: message => calls.push(['warn', message]),
      saveCurrentDataset: async () => calls.push(['saveDataset']),
      persistDatasetSync: async () => calls.push(['saveTask']),
      executeDatasetSync: async () => calls.push(['execute']),
      loadDatasetSyncLogs: async () => [],
      applyTask: () => undefined,
      applyLogs: () => undefined,
      startSyncPolling: () => undefined,
      success: () => undefined
    })

    assert.equal(result.submitted, false)
    assert.equal(result.reason, 'missingDatasetId')
    assert.deepEqual(calls, [['warn', '请先保存数据集']])
  })

  it('does not submit while another sync task is running', async () => {
    const calls = []

    const result = await executeDatasetSyncNowFlow({
      currentDatasetId: '982004',
      syncRunning: true,
      warn: message => calls.push(['warn', message]),
      saveCurrentDataset: async () => calls.push(['saveDataset']),
      persistDatasetSync: async () => calls.push(['saveTask']),
      executeDatasetSync: async () => calls.push(['execute']),
      loadDatasetSyncLogs: async () => [],
      applyTask: () => undefined,
      applyLogs: () => undefined,
      startSyncPolling: () => undefined,
      success: () => undefined
    })

    assert.equal(result.submitted, false)
    assert.equal(result.reason, 'running')
    assert.deepEqual(calls, [['warn', '缓存更新正在执行']])
  })

  it('builds a config-only payload when saving the sync task', () => {
    const payload = buildDatasetSyncTaskPayload({
      syncTask: {
        id: 'task-1',
        updateType: 'add_scope',
        incrementalFieldId: 'field-1',
        incrementalLastValue: 'old-watermark',
        startTime: 1781087872814,
        syncRate: 'SIMPLE_CRON',
        cron: '0 0/30 * * * ? *',
        simpleCronValue: 30,
        simpleCronType: 'minute',
        simpleCronHour: 0,
        lastExecTime: 1781080000000,
        lastExecStatus: 'Error',
        taskStatus: 'Stopped',
        cacheReady: 0,
        schemaHash: 'old-schema',
        fullSyncIntervalHours: 24,
        lastFullSyncTime: 1781080000000,
        verifyEnabled: 1,
        lastVerifyTime: 1781080000000,
        lastVerifyStatus: 'WARNING',
        lastVerifyMessage: 'old message',
        lastSourceRowCount: 10,
        lastCacheRowCount: 9,
        cacheExpireHours: 26,
        taskTimeoutMinutes: 360,
        consecutiveFailures: 1,
        failureWarnThreshold: 1,
        cacheExpired: true,
        failureWarned: true
      },
      datasetGroupId: '982004',
      datasetName: '角色分布透视'
    })

    assert.deepEqual(payload, {
      id: 'task-1',
      datasetGroupId: '982004',
      name: '角色分布透视',
      updateType: 'add_scope',
      incrementalFieldId: 'field-1',
      startTime: 1781087872814,
      syncRate: 'SIMPLE_CRON',
      cron: '0 0/30 * * * ? *',
      simpleCronValue: 30,
      simpleCronType: 'minute',
      simpleCronHour: 0,
      endTime: undefined,
      fullSyncIntervalHours: 24,
      verifyEnabled: 1,
      cacheExpireHours: 26,
      taskTimeoutMinutes: 360,
      failureWarnThreshold: 1
    })
    assert.equal('taskStatus' in payload, false)
    assert.equal('lastExecStatus' in payload, false)
    assert.equal('cacheReady' in payload, false)
    assert.equal('lastFullSyncTime' in payload, false)
    assert.equal('lastVerifyTime' in payload, false)
    assert.equal('consecutiveFailures' in payload, false)
  })

  it('hides in-progress placeholder zero but keeps a completed zero row count', () => {
    assert.equal(formatDatasetSyncRowCount({ taskStatus: 'UnderExecution', rowCount: 0 }), '')
    assert.equal(formatDatasetSyncRowCount({ taskStatus: 'Completed', rowCount: 0 }), '0 行')
    assert.equal(formatDatasetSyncRowCount({ taskStatus: 'Completed', rowCount: 79 }), '79 行')
    assert.equal(formatDatasetSyncRowCount(), '')
  })

  it('uses clear cache status text before cache data is generated', () => {
    assert.equal(getDatasetSyncStatusText({ syncMode: 0 }), '未开启')
    assert.equal(getDatasetSyncStatusText({ syncMode: 1, hasDatasetId: false }), '待保存')
    assert.equal(getDatasetSyncStatusText({ syncMode: 1, hasDatasetId: true }), '待更新')
    assert.equal(
      getDatasetSyncStatusText({
        syncMode: 1,
        hasDatasetId: true,
        lastExecStatus: 'Completed',
        cacheReady: 1
      }),
      '缓存可用'
    )
  })

  it('keeps the main cache status within the five product states', () => {
    const cases = [
      [{ syncMode: 1, hasDatasetId: false }, '待保存'],
      [{ syncMode: 1, hasDatasetId: true, taskStatus: 'WaitingForExecution' }, '待更新'],
      [{ syncMode: 1, hasDatasetId: true, taskStatus: 'Suspend' }, '待更新'],
      [{ syncMode: 1, hasDatasetId: true, cacheExpired: true }, '待更新'],
      [{ syncMode: 1, hasDatasetId: true, taskStatus: 'UnderExecution' }, '更新中'],
      [{ syncMode: 1, hasDatasetId: true, lastExecStatus: 'Completed', cacheReady: 1 }, '缓存可用'],
      [{ syncMode: 1, hasDatasetId: true, lastExecStatus: 'Error' }, '更新失败'],
      [{ syncMode: 1, hasDatasetId: true, failureWarned: true }, '更新失败'],
      [{ syncMode: 1, hasDatasetId: true, lastVerifyStatus: 'WARNING' }, '更新失败']
    ]

    cases.forEach(([task, status]) => {
      assert.equal(getDatasetSyncStatusText(task), status)
    })
  })

  it('explains why cache update cannot be submitted', () => {
    assert.equal(
      getDatasetSyncNowDisabledReason({ hasDatasetId: false, syncRunning: false }),
      '请先保存数据集，再执行缓存更新'
    )
    assert.equal(
      getDatasetSyncNowDisabledReason({ hasDatasetId: true, syncRunning: true }),
      '缓存更新正在执行'
    )
    assert.equal(getDatasetSyncNowDisabledReason({ hasDatasetId: true, syncRunning: false }), '')
  })

  it('explains when incremental update is unavailable', () => {
    assert.equal(getIncrementalDisabledReason([]), '当前没有可用增量字段，请使用全量更新')
    assert.equal(getIncrementalDisabledReason([{ id: 'field-1' }]), '')
  })

  it('normalizes historical reconcile copy to user-facing cache wording', () => {
    assert.equal(normalizeDatasetSyncInfo('对账通过'), '源数据与缓存一致')
    assert.equal(normalizeDatasetSyncInfo('对账执行失败：连接超时'), '数据一致性校验失败：连接超时')
    assert.equal(normalizeDatasetSyncInfo('同步完成'), '同步完成')
  })
})
