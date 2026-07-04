import { DEFAULT_COLOR_CASE } from '@/views/chart/components/editor/util/chart'

type PlainRecord = Record<string, any>

const REQUIRED_CHART_COLOR_SECTIONS = [
  'basicStyle',
  'misc',
  'tableHeader',
  'tableCell',
  'label',
  'tooltip'
] as const

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

export const normalizeCanvasChartColor = (
  chartColor?: DeepPartial<ChartAttr> | null
): DeepPartial<ChartAttr> => {
  const source = isPlainRecord(chartColor) ? chartColor : {}
  const result = cloneValue(source)

  REQUIRED_CHART_COLOR_SECTIONS.forEach(section => {
    result[section] = mergeWithDefaults(DEFAULT_COLOR_CASE[section], source[section])
  })

  return result as DeepPartial<ChartAttr>
}
