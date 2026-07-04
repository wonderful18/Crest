import {
  COMMON_COMPONENT_BACKGROUND_DARK,
  COMMON_COMPONENT_BACKGROUND_LIGHT
} from '@/custom-component/component-list'

type PlainRecord = Record<string, any>

const isPlainRecord = (value: unknown): value is PlainRecord => {
  return !!value && typeof value === 'object' && !Array.isArray(value)
}

const cloneValue = <T>(value: T): T => {
  if (Array.isArray(value)) {
    return value.map(item => cloneValue(item)) as T
  }
  if (!isPlainRecord(value)) {
    return value
  }
  const result: PlainRecord = {}
  Object.keys(value).forEach(key => {
    result[key] = cloneValue(value[key])
  })
  return result as T
}

const mergeWithDefaults = (defaults: unknown, value: unknown): PlainRecord => {
  const result = isPlainRecord(defaults) ? cloneValue(defaults) : {}
  if (!isPlainRecord(value)) {
    return result
  }
  Object.keys(value).forEach(key => {
    const nextValue = value[key]
    if (isPlainRecord(result[key]) && isPlainRecord(nextValue)) {
      result[key] = mergeWithDefaults(result[key], nextValue)
    } else {
      result[key] = cloneValue(nextValue)
    }
  })
  return result
}

const USER_VIEW_STYLE_DEFAULTS = {
  adaptation: 'adaptation',
  width: 600,
  height: 300
}

const USER_VIEW_PROP_VALUE_DEFAULTS = {
  textValue: '',
  urlList: []
}

export const normalizeCanvasComponentDefaults = (
  componentItem: Record<string, any>,
  theme?: string
): void => {
  if (!isPlainRecord(componentItem)) {
    return
  }
  const backgroundDefaults =
    theme === 'light' ? COMMON_COMPONENT_BACKGROUND_LIGHT : COMMON_COMPONENT_BACKGROUND_DARK

  componentItem.style = isPlainRecord(componentItem.style) ? componentItem.style : {}
  componentItem.commonBackground = mergeWithDefaults(
    backgroundDefaults,
    componentItem.commonBackground
  )

  if (componentItem.component === 'UserView') {
    componentItem.style = mergeWithDefaults(USER_VIEW_STYLE_DEFAULTS, componentItem.style)
    componentItem.propValue = mergeWithDefaults(
      USER_VIEW_PROP_VALUE_DEFAULTS,
      componentItem.propValue
    )
    componentItem.matrixStyle = isPlainRecord(componentItem.matrixStyle)
      ? componentItem.matrixStyle
      : {}
  }
}
