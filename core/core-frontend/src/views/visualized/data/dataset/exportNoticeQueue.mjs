const DEFAULT_DELAY = 1000
const DEFAULT_TTL = 60 * 1000

const toStatus = task => String(task?.exportStatus || '').toUpperCase()

const toTaskKey = task => `task:${task?.id || ''}:${toStatus(task)}`

const prune = (cache, now, ttl) => {
  cache.forEach((time, key) => {
    if (now - time > ttl) {
      cache.delete(key)
    }
  })
}

export const createExportNoticeQueue = ({ delay = DEFAULT_DELAY, ttl = DEFAULT_TTL, now = Date.now, emit }) => {
  const seen = new Map()
  const pending = []
  let timer

  const schedule = () => {
    if (timer) {
      return
    }
    if (delay <= 0) {
      return
    }
    timer = setTimeout(() => {
      timer = undefined
      flush()
    }, delay)
  }

  const flush = () => {
    if (timer) {
      clearTimeout(timer)
      timer = undefined
    }
    if (!pending.length) {
      return
    }
    const grouped = pending.splice(0).reduce((result, task) => {
      const status = toStatus(task)
      if (!result[status]) {
        result[status] = []
      }
      result[status].push(task)
      return result
    }, {})

    Object.keys(grouped).forEach(status => {
      const tasks = grouped[status]
      emit?.({
        status,
        count: tasks.length,
        firstName: tasks[0]?.exportFromName || '',
        tasks
      })
    })
  }

  const push = task => {
    const status = toStatus(task)
    if (!['SUCCESS', 'FAILED'].includes(status)) {
      return false
    }

    const current = now()
    prune(seen, current, ttl)
    const taskKey = toTaskKey(task)
    if (seen.has(taskKey)) {
      return false
    }
    seen.set(taskKey, current)
    pending.push(task)
    schedule()
    return true
  }

  const clear = () => {
    if (timer) {
      clearTimeout(timer)
      timer = undefined
    }
    pending.splice(0)
    seen.clear()
  }

  return {
    push,
    flush,
    clear
  }
}

export const formatExportNoticeMessage = (
  notice,
  successText,
  failedText,
  bulkSuccessText,
  bulkFailedText
) => {
  if (notice.count <= 1) {
    const actionText = notice.status === 'FAILED' ? failedText : successText
    return `${notice.firstName} ${actionText}`.trim()
  }
  const template = notice.status === 'FAILED' ? bulkFailedText : bulkSuccessText
  if (template) {
    return template.replace('{count}', String(notice.count))
  }
  return notice.status === 'FAILED'
    ? `${notice.count} 个导出任务失败，前往`
    : `${notice.count} 个导出任务已成功，前往`
}
