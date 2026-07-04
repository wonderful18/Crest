export const DEFAULT_DYNAMIC_DATE_TERM = 'eq'

const isBlank = value => value === '' || value === null || value === undefined

const isDynamicDateRule = rule => {
  return rule?.fieldType === 1 && rule?.filterTypeTime === 'dynamicDate'
}

const hasDynamicDateSetting = rule => {
  return Boolean(rule?.dynamicTimeSetting?.relativeToCurrent)
}

export const normalizeDynamicDateFilterRule = rule => {
  if (!rule || !isDynamicDateRule(rule) || !isBlank(rule.term) || !hasDynamicDateSetting(rule)) {
    return rule
  }
  return {
    ...rule,
    term: DEFAULT_DYNAMIC_DATE_TERM
  }
}
