// 编辑器自定义事件
const events = {
  redirect(url) {
    const target = safeRedirectUrl(url)
    if (target) {
      window.location.href = target
    }
  },

  alert(msg) {
    if (msg) {
      alert(msg)
    }
  }
}

const safeRedirectUrl = (url?: string) => {
  const text = String(url || '').trim()
  if (!text || /[\u0000-\u001f\u007f]/.test(text)) return null
  try {
    const target = new URL(text, window.location.href)
    return ['http:', 'https:'].includes(target.protocol) ? text : null
  } catch {
    return null
  }
}

const mixins = {
  methods: events
}

const eventList = [
  {
    key: 'redirect',
    label: '跳转事件',
    event: events.redirect,
    param: ''
  },
  {
    key: 'alert',
    label: 'alert 事件',
    event: events.alert,
    param: ''
  }
]

export { mixins, events, eventList }
