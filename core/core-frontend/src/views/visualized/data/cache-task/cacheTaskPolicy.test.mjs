import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  cacheTaskStatusText,
  cacheTaskFailureReason,
  cacheTaskActions,
  cacheTaskRunningSnapshot,
  formatCacheTaskDuration,
  shouldContinueCacheTaskPolling
} from './cacheTaskPolicy.mjs'

describe('cache task policy', () => {
  it('keeps task center status within the five cache states', () => {
    assert.equal(cacheTaskStatusText({}), '待更新')
    assert.equal(cacheTaskStatusText({ taskStatus: 'UnderExecution' }), '更新中')
    assert.equal(cacheTaskStatusText({ taskStatus: 'Suspend' }), '待更新')
    assert.equal(cacheTaskStatusText({ cacheExpired: true, lastExecStatus: 'Completed' }), '待更新')
    assert.equal(cacheTaskStatusText({ cacheReady: 1, lastExecStatus: 'Completed' }), '缓存可用')
    assert.equal(cacheTaskStatusText({ lastExecStatus: 'Error' }), '更新失败')
    assert.equal(cacheTaskStatusText({ lastVerifyStatus: 'WARNING' }), '更新失败')
  })

  it('derives available operations from task status', () => {
    assert.deepEqual(
      cacheTaskActions({ syncRate: 'SIMPLE_CRON', taskStatus: 'WaitingForExecution' }),
      {
        canExecute: true,
        canPause: true,
        canResume: false,
        canRetry: false
      }
    )
    assert.deepEqual(cacheTaskActions({ syncRate: 'SIMPLE_CRON', taskStatus: 'Suspend' }), {
      canExecute: true,
      canPause: false,
      canResume: true,
      canRetry: false
    })
    assert.deepEqual(cacheTaskActions({ taskStatus: 'UnderExecution', lastExecStatus: 'Error' }), {
      canExecute: false,
      canPause: true,
      canResume: false,
      canRetry: false
    })
    assert.deepEqual(
      cacheTaskActions({ taskStatus: 'WaitingForExecution', lastExecStatus: 'Error' }),
      {
        canExecute: true,
        canPause: false,
        canResume: false,
        canRetry: true
      }
    )
  })

  it('formats duration and failure reason for table display', () => {
    assert.equal(formatCacheTaskDuration(500), '0.5 秒')
    assert.equal(formatCacheTaskDuration(65_000), '1 分 5 秒')
    assert.equal(formatCacheTaskDuration(), '-')
    assert.equal(cacheTaskFailureReason({ failureReason: '连接失败' }), '连接失败')
    assert.equal(
      cacheTaskFailureReason({ lastExecStatus: 'Completed', lastLogInfo: '同步完成' }),
      '-'
    )
  })

  it('keeps the row in progress after submitting an async update', () => {
    const running = cacheTaskRunningSnapshot(
      { datasetGroupId: '982004', taskStatus: 'WaitingForExecution', cacheReady: 1 },
      1781230000000
    )

    assert.equal(running.taskStatus, 'UnderExecution')
    assert.equal(running.lastLogStatus, 'UnderExecution')
    assert.equal(running.lastLogStartTime, 1781230000000)
    assert.equal(cacheTaskStatusText(running), '更新中')
  })

  it('polls through the async task settle window and while the task is running', () => {
    assert.equal(
      shouldContinueCacheTaskPolling({
        task: { taskStatus: 'WaitingForExecution', cacheReady: 1, lastExecStatus: 'Completed' },
        attempts: 1,
        settleAttempts: 2
      }),
      true
    )
    assert.equal(
      shouldContinueCacheTaskPolling({
        task: { taskStatus: 'UnderExecution' },
        attempts: 3,
        settleAttempts: 2
      }),
      true
    )
    assert.equal(
      shouldContinueCacheTaskPolling({
        task: { taskStatus: 'WaitingForExecution', cacheReady: 1, lastExecStatus: 'Completed' },
        attempts: 3,
        settleAttempts: 2
      }),
      false
    )
  })
})
