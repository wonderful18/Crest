const ATTR_FIELD_KEY = 'key'

export const positionData = [
  { [ATTR_FIELD_KEY]: 'left', label: '左', min: -1000, max: 20000, step: 1 },
  { [ATTR_FIELD_KEY]: 'width', label: '宽', min: 2, max: 20000, step: 1 },
  { [ATTR_FIELD_KEY]: 'top', label: '上', min: -1000, max: 20000, step: 1 },
  { [ATTR_FIELD_KEY]: 'height', label: '高', min: 2, max: 20000, step: 1 }
]

export const multiDimensionalData = [
  { [ATTR_FIELD_KEY]: 'x', label: 'X', min: -360, max: 360, step: 1 },
  { [ATTR_FIELD_KEY]: 'y', label: 'W', min: -360, max: 360, step: 1 },
  { [ATTR_FIELD_KEY]: 'z', label: 'Y', min: -360, max: 360, step: 1 }
]

export const styleData = [
  { [ATTR_FIELD_KEY]: 'lineHeight', label: '行高', min: 0, max: 50, step: 1 },
  { [ATTR_FIELD_KEY]: 'opacity', label: '不透明度', min: 0, max: 1, step: 0.1 },
  { [ATTR_FIELD_KEY]: 'borderWidth', label: '边框宽度', min: 0, max: 20, step: 1 },
  { [ATTR_FIELD_KEY]: 'borderStyle', label: '边框风格' },
  { [ATTR_FIELD_KEY]: 'borderColor', label: '边框颜色' },
  { [ATTR_FIELD_KEY]: 'borderRadius', label: '圆角', min: 0, max: 50, step: 1 },
  { [ATTR_FIELD_KEY]: 'letterSpacing', label: '字间距', min: 0, max: 50, step: 1 },
  { [ATTR_FIELD_KEY]: 'fontSize', label: '字体大小', min: 0, max: 128, step: 1 },
  { [ATTR_FIELD_KEY]: 'activeFontSize', label: '激活字体大小', min: 0, max: 128, step: 1 },
  { [ATTR_FIELD_KEY]: 'headFontColor', label: '标题字体颜色' },
  { [ATTR_FIELD_KEY]: 'headFontActiveColor', label: '标题字体激活颜色' },
  { [ATTR_FIELD_KEY]: 'headBorderColor', label: '标题边框颜色' },
  { [ATTR_FIELD_KEY]: 'headBorderActiveColor', label: '标题激活边框颜色' },
  { [ATTR_FIELD_KEY]: 'headHorizontalPosition', label: '标题位置' },
  { [ATTR_FIELD_KEY]: 'fontWeight', label: '字体粗细', min: 100, max: 900, step: 100 },
  { [ATTR_FIELD_KEY]: 'textAlign', label: '左右对齐' },
  { [ATTR_FIELD_KEY]: 'verticalAlign', label: '上下对齐' },
  { [ATTR_FIELD_KEY]: 'color', label: '颜色' },
  { [ATTR_FIELD_KEY]: 'backgroundColor', label: '背景色' }
]

export const styleMap = {
  left: 'x 坐标',
  top: 'y 坐标',
  rotate: '旋转角度',
  width: '宽',
  height: '高',
  color: '颜色',
  backgroundColor: '背景色',
  borderWidth: '边框宽度',
  borderStyle: '边框风格',
  borderColor: '边框颜色',
  borderRadius: '圆角',
  fontSize: '字体大小',
  fontWeight: '字体粗细',
  lineHeight: '行高',
  letterSpacing: '字间距',
  textAlign: '左右对齐',
  verticalAlign: '上下对齐',
  opacity: '不透明度'
}

export const textAlignOptions = [
  {
    label: '左对齐',
    value: 'left'
  },
  {
    label: '居中',
    value: 'center'
  },
  {
    label: '右对齐',
    value: 'right'
  }
]

export const borderStyleOptions = [
  {
    label: '实线',
    value: 'solid'
  },
  {
    label: '虚线',
    value: 'dashed'
  }
]

export const verticalAlignOptions = [
  {
    label: '上对齐',
    value: 'top'
  },
  {
    label: '居中对齐',
    value: 'middle'
  },
  {
    label: '下对齐',
    value: 'bottom'
  }
]

export const selectKey = ['textAlign', 'borderStyle', 'verticalAlign']

export const horizontalPosition = ['headHorizontalPosition']

export const fieldType = ['text', 'time', 'value', 'value', 'value', 'location', 'binary', 'url']
export const fieldTypeText = [
  '文本',
  '时间',
  '数值',
  '数值(小数)',
  '数值',
  '地理位置',
  '文件',
  'URL'
]

export const optionMap = {
  textAlign: textAlignOptions,
  borderStyle: borderStyleOptions,
  verticalAlign: verticalAlignOptions
}
