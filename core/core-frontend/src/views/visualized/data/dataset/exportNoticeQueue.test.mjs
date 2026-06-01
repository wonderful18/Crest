import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  createExportNoticeQueue,
  formatExportNoticeMessage
} from './exportNoticeQueue.mjs'

describe('export notice queue', () => {
  it('aggregates several successful tasks into one notice', () => {
    const notices = []
    const queue = createExportNoticeQueue({
      delay: 0,
      now: () => 1000,
      emit: notice => notices.push(notice)
    })

    queue.push({
      id: 'task-1',
      exportStatus: 'SUCCESS',
      exportFromName: '部署需求累积流图'
    })
    queue.push({
      id: 'task-2',
      exportStatus: 'SUCCESS',
      exportFromName: '阶段周期漏斗'
    })
    queue.push({
      id: 'task-3',
      exportStatus: 'SUCCESS',
      exportFromName: '人力投入-需求产出矩阵'
    })
    queue.flush()

    assert.equal(notices.length, 1)
    assert.equal(notices[0].status, 'SUCCESS')
    assert.equal(notices[0].count, 3)
    assert.equal(formatExportNoticeMessage(notices[0], '导出成功，前往', '导出失败，前往'), '3 个导出任务已成功，前往')
  })

  it('deduplicates the same task status', () => {
    const notices = []
    const queue = createExportNoticeQueue({
      delay: 0,
      now: () => 1000,
      emit: notice => notices.push(notice)
    })

    queue.push({
      id: 'task-1',
      exportStatus: 'SUCCESS',
      exportFromName: '部署需求累积流图'
    })
    queue.push({
      id: 'task-1',
      exportStatus: 'SUCCESS',
      exportFromName: '部署需求累积流图'
    })
    queue.flush()

    assert.equal(notices.length, 1)
    assert.equal(notices[0].count, 1)
    assert.equal(
      formatExportNoticeMessage(notices[0], '导出成功，前往', '导出失败，前往'),
      '部署需求累积流图 导出成功，前往'
    )
  })
})
