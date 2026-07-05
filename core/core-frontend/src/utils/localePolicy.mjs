export const DEFAULT_LOCALE = 'zh-CN'

const BUILT_IN_LOCALE_MAP = {
  'zh': 'zh-CN',
  'zh-cn': 'zh-CN',
  'zh_cn': 'zh-CN',
  'zh-hans': 'zh-CN',
  'zh-hans-cn': 'zh-CN',
  'zh-tw': 'tw',
  'zh_tw': 'tw',
  'zh-hk': 'tw',
  'zh_hk': 'tw',
  'zh-hant': 'tw',
  en: 'en',
  'en-us': 'en',
  'en_us': 'en',
  'en-gb': 'en',
  'en_gb': 'en',
  tw: 'tw'
}

export const normalizeBuiltInLocale = locale => {
  if (!locale || typeof locale !== 'string') {
    return null
  }
  const normalized = locale.trim()
  if (!normalized) {
    return null
  }
  return BUILT_IN_LOCALE_MAP[normalized.toLowerCase()] || null
}

export const resolveInitialLocale = locale => {
  if (!locale || typeof locale !== 'string' || !locale.trim()) {
    return DEFAULT_LOCALE
  }
  return normalizeBuiltInLocale(locale) || locale.trim()
}
