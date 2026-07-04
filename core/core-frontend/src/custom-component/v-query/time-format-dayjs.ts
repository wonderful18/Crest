import dayjs from 'dayjs'
import type { Dayjs, ManipulateType } from 'dayjs'

const dateTimeFormat = 'YYYY/MM/DD HH:mm:ss'

// 周范围固定按业务习惯使用周一到周日，避免受 dayjs 全局 locale 影响。
function getWeekStart(current: Dayjs = dayjs()) {
  return current.startOf('day').subtract((current.day() + 6) % 7, 'day')
}

function getWeekEnd(current: Dayjs = dayjs()) {
  return getWeekStart(current).add(6, 'day').endOf('day')
}

function toDate(current: Dayjs) {
  return new Date(current.format(dateTimeFormat))
}

// 获取当前时间单位的起始时间
function getThisStart(val = 'month' as ManipulateType | 'quarter') {
  return toDate(dayjs().startOf(val))
}

// 获取当前时间单位的结束时间
function getThisEnd(val = 'month' as ManipulateType | 'quarter') {
  return toDate(dayjs().endOf(val))
}

// 获取上一时间单位的起始时间
function getLastStart(val = 'month' as ManipulateType) {
  return toDate(dayjs().subtract(1, val).startOf(val))
}

// 获取上一时间单位的结束时间
function getLastEnd(val = 'month' as ManipulateType) {
  return toDate(dayjs().subtract(1, val).endOf(val))
}

// 获取相对当前时间偏移后的结束时间
function getAround(val = 'month' as ManipulateType, type: 'add' | 'subtract' = 'add', num = 0) {
  const current = dayjs()[type](num, val)
  if (val === 'week') {
    return toDate(getWeekEnd(current))
  }
  return toDate(current.endOf('day'))
}

// 获取相对当前时间偏移后的起始时间
function getAroundStart(
  val = 'month' as ManipulateType,
  type: 'add' | 'subtract' = 'add',
  num = 0
) {
  const current = dayjs()[type](num, val)
  if (val === 'week') {
    return toDate(getWeekStart(current))
  }
  return toDate(current.startOf('day'))
}

// 获取本周的起止时间范围
function getThisWeek(): [Date, Date] {
  return [toDate(getWeekStart()), toDate(getWeekEnd())]
}

// 根据预设范围标识计算自定义时间范围
function getCustomRange(relativeToCurrentRange: string): [Date, Date] {
  switch (relativeToCurrentRange) {
    case 'thisYear':
      return [getThisStart('year'), getThisEnd('year')]
    case 'lastYear':
      return [getLastStart('year'), getLastEnd('year')]
    case 'thisMonth':
      return [getThisStart('month'), getThisEnd('month')]
    case 'lastMonth':
      return [getLastStart('month'), getLastEnd('month')]
    case 'thisQuarter':
      return [getThisStart('quarter'), getThisEnd('quarter')]
    case 'thisWeek':
      return getThisWeek()
    case 'LastThreeMonths':
      return [toDate(dayjs().subtract(2, 'month').startOf('month')), getThisEnd('day')]
    case 'LastSixMonths':
      return [toDate(dayjs().subtract(5, 'month').startOf('month')), getThisEnd('day')]
    case 'LastTwelveMonths':
      return [toDate(dayjs().subtract(11, 'month').startOf('month')), getThisEnd('day')]
    case 'YearToThisMonth':
      return [toDate(dayjs().startOf('year')), getThisEnd('month')]
    case 'monthToYesterday':
      const sm = toDate(dayjs().startOf('month'))
      const ld = getLastEnd('day')
      if (+sm > +ld) {
        return [sm, getThisEnd('day')]
      }
      return [sm, ld]
    case 'today':
      return [getThisStart('day'), getThisEnd('day')]
    case 'yesterday':
      return [getLastStart('day'), getLastEnd('day')]
    case 'LastThreeDays':
      return [toDate(dayjs().subtract(2, 'day').startOf('day')), getThisEnd('day')]
    case 'monthBeginning':
      return [getThisStart('month'), getThisEnd('day')]
    case 'yearBeginning':
      return [getThisStart('year'), getThisEnd('day')]
    default:
      return [new Date(), new Date()]
  }
}
export {
  getThisStart,
  getThisEnd,
  getLastStart,
  getLastEnd,
  getAround,
  getCustomRange,
  getAroundStart
}
