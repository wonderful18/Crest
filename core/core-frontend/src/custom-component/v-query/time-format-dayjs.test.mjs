import assert from 'node:assert/strict'
import { mock, test } from 'node:test'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn.js'
import { getCustomRange } from './time-format-dayjs.ts'

const format = date => dayjs(date).format('YYYY-MM-DD HH:mm:ss')

test('thisWeek range starts on Monday and ends on Sunday under zh-cn locale', () => {
  mock.timers.enable({
    apis: ['Date'],
    now: new Date('2026-07-04T12:00:00+08:00')
  })
  dayjs.locale('zh-cn')

  const [startTime, endTime] = getCustomRange('thisWeek')

  assert.equal(format(startTime), '2026-06-29 00:00:00')
  assert.equal(format(endTime), '2026-07-05 23:59:59')
  mock.timers.reset()
})
