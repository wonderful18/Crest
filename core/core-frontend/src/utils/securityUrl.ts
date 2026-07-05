export const safeSameOriginUrl = (
  url?: string | null,
  currentOrigin = window.location.origin
) => {
  if (!url) return '/'
  try {
    const target = new URL(url, currentOrigin)
    if (target.origin !== currentOrigin) {
      return '/'
    }
    return `${target.pathname}${target.search}${target.hash}`
  } catch {
    return '/'
  }
}

export const resolvePostMessageTargetOrigin = (
  referrer = document.referrer,
  currentOrigin = window.location.origin,
  allowedOriginList = import.meta.env.VITE_EMBEDDED_ORIGIN_LIST || ''
) => {
  if (!referrer) return currentOrigin
  try {
    const targetOrigin = new URL(referrer).origin
    if (targetOrigin === currentOrigin) return targetOrigin
    return allowedPostMessageOrigins(allowedOriginList).has(targetOrigin) ? targetOrigin : currentOrigin
  } catch {
    return currentOrigin
  }
}

const allowedPostMessageOrigins = (allowedOriginList: string) =>
  new Set(
    allowedOriginList
      .split(',')
      .map(origin => origin.trim())
      .filter(Boolean)
      .map(origin => {
        try {
          return new URL(origin).origin
        } catch {
          return ''
        }
      })
      .filter(Boolean)
  )
