import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  EXPORT_DATASET_RESULT,
  resolveExportDatasetResult
} from './exportDatasetResult.mjs'

describe('dataset export result resolver', () => {
  it('treats an empty 200 response as a queued export task', async () => {
    const result = await resolveExportDatasetResult({
      status: 200,
      headers: {},
      data: new Blob([])
    })

    assert.deepEqual(result, { status: EXPORT_DATASET_RESULT.QUEUED })
  })

  it('does not report canceled requests as queued tasks', async () => {
    const result = await resolveExportDatasetResult({
      code: 499,
      canceled: true,
      msg: 'request canceled'
    })

    assert.deepEqual(result, {
      status: EXPORT_DATASET_RESULT.CANCELED,
      message: 'request canceled'
    })
  })

  it('surfaces JSON errors returned through blob responses', async () => {
    const result = await resolveExportDatasetResult({
      status: 200,
      headers: { 'content-type': 'application/json;charset=UTF-8' },
      data: new Blob([JSON.stringify({ code: 50000, msg: '系统内部错误，请联系管理员' })], {
        type: 'application/json'
      })
    })

    assert.deepEqual(result, {
      status: EXPORT_DATASET_RESULT.FAILED,
      message: '系统内部错误，请联系管理员'
    })
  })

  it('surfaces direct business error responses', async () => {
    const result = await resolveExportDatasetResult({
      code: 50000,
      msg: '导出失败'
    })

    assert.deepEqual(result, {
      status: EXPORT_DATASET_RESULT.FAILED,
      message: '导出失败'
    })
  })
})
