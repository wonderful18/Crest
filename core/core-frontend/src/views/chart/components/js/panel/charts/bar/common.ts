export const BAR_EDITOR_PROPERTY: EditorProperty[] = [
  'background-overall-component',
  'border-style',
  'basic-style-selector',
  'label-selector',
  'tooltip-selector',
  'x-axis-selector',
  'y-axis-selector',
  'title-selector',
  'legend-selector',
  'function-cfg',
  'assist-line',
  'jump-set',
  'linkage',
  'threshold'
]
export const BAR_RANGE_EDITOR_PROPERTY: EditorProperty[] = [
  'background-overall-component',
  'border-style',
  'basic-style-selector',
  'label-selector',
  'tooltip-selector',
  'x-axis-selector',
  'y-axis-selector',
  'title-selector',
  'legend-selector',
  'function-cfg',
  'jump-set',
  'linkage',
  'threshold'
]

export const BAR_EDITOR_PROPERTY_INNER: EditorPropertyInner = {
  'background-overall-component': ['all'],
  'border-style': ['all'],
  'basic-style-selector': ['colors', 'alpha', 'gradient', 'radiusColumnBar', 'columnWidthRatio'],
  'label-selector': ['fontSize', 'color', 'labelFormatter'],
  'tooltip-selector': ['fontSize', 'color', 'tooltipFormatter', 'show'],
  'x-axis-selector': [
    'name',
    'color',
    'fontSize',
    'axisLine',
    'splitLine',
    'axisForm',
    'axisLabel',
    'position'
  ],
  'y-axis-selector': [
    'name',
    'color',
    'fontSize',
    'axisValue',
    'axisLine',
    'splitLine',
    'axisForm',
    'axisLabel',
    'position'
  ],
  'title-selector': [
    'title',
    'fontSize',
    'color',
    'hPosition',
    'isItalic',
    'isBolder',
    'remarkShow',
    'fontFamily',
    'letterSpace',
    'fontShadow'
  ],
  'legend-selector': ['icon', 'orient', 'fontSize', 'color', 'hPosition', 'vPosition'],
  'function-cfg': ['slider', 'emptyDataStrategy'],
  threshold: ['lineThreshold']
}

export const BAR_AXIS_TYPE: AxisType[] = [
  'xAxis',
  'yAxis',
  'filter',
  'drill',
  'extLabel',
  'extTooltip'
]

/**
 * 按汇总方式计算总数：求和/平均/计数/最大/最小
 * @param values 数值数组（允许 null/undefined）
 * @param calcType 汇总方式，默认 sum
 */
export const calcTotalValue = (
  values: number[],
  calcType: TotalCalcType = 'sum'
): number => {
  const nums = values
    .map(v => Number(v ?? 0))
    .filter(n => !Number.isNaN(n))
  if (!nums.length) {
    return 0
  }
  switch (calcType) {
    case 'avg':
      return nums.reduce((a, b) => a + b, 0) / nums.length
    case 'count':
      return nums.length
    case 'max':
      return Math.max(...nums)
    case 'min':
      return Math.min(...nums)
    case 'sum':
    default:
      return nums.reduce((a, b) => a + b, 0)
  }
}

