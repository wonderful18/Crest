type ComponentConfig = Record<string, any>
export type ChartGroupLayoutMode = 'keep' | 'horizontal' | 'vertical' | 'grid'

const zeroEdgeValues = () => ({
  mode: 'uniform',
  top: 0,
  right: 0,
  bottom: 0,
  left: 0
})

const zeroCornerValues = () => ({
  mode: 'uniform',
  topLeft: 0,
  topRight: 0,
  bottomRight: 0,
  bottomLeft: 0
})

const isGroup = (component?: ComponentConfig) => component?.component === 'Group'

export function normalizeChartGroupContainer(group?: ComponentConfig) {
  if (!isGroup(group)) {
    return group
  }

  group.chartGroupContainer = {
    clipOverflow: group.chartGroupContainer?.clipOverflow !== false,
    seamless: group.chartGroupContainer?.seamless === true,
    layoutMode: group.chartGroupContainer?.layoutMode || 'keep',
    horizontalGap: Number(group.chartGroupContainer?.horizontalGap) || 0,
    verticalGap: Number(group.chartGroupContainer?.verticalGap) || 0
  }
  group.commonBackground = group.commonBackground || {}
  group.commonBackground.innerPadding = group.commonBackground.innerPadding || zeroEdgeValues()
  group.commonBackground.borderRadius = group.commonBackground.borderRadius || zeroCornerValues()
  group.propValue = Array.isArray(group.propValue) ? group.propValue : []
  return group
}

export function findChartGroupParent(componentData: ComponentConfig[] = [], componentId?: string) {
  if (!componentId) {
    return null
  }

  for (let groupIndex = 0; groupIndex < componentData.length; groupIndex += 1) {
    const component = componentData[groupIndex]
    if (component?.component === 'Group' && Array.isArray(component.propValue)) {
      const componentIndex = component.propValue.findIndex(child => child?.id === componentId)
      if (componentIndex > -1) {
        return {
          group: component,
          groupIndex,
          component: component.propValue[componentIndex],
          componentIndex,
          ownerComponents: componentData
        }
      }
      const nestedResult = findChartGroupParent(component.propValue, componentId)
      if (nestedResult) {
        return nestedResult
      }
    }
    if (component?.component === 'Tabs' && Array.isArray(component.propValue)) {
      for (const tabItem of component.propValue) {
        const nestedResult = findChartGroupParent(tabItem?.componentData || [], componentId)
        if (nestedResult) {
          return nestedResult
        }
      }
    }
  }

  return null
}

export function calculateChartGroupBounds(components: ComponentConfig[] = []) {
  const validComponents = components.filter(component => {
    const style = component?.style
    return (
      isFiniteNumber(style?.left) &&
      isFiniteNumber(style?.top) &&
      isFiniteNumber(style?.width) &&
      isFiniteNumber(style?.height)
    )
  })
  if (validComponents.length !== components.length || !validComponents.length) {
    return null
  }

  let left = Infinity
  let top = Infinity
  let right = -Infinity
  let bottom = -Infinity
  validComponents.forEach(component => {
    left = Math.min(left, component.style.left)
    top = Math.min(top, component.style.top)
    right = Math.max(right, component.style.left + component.style.width)
    bottom = Math.max(bottom, component.style.top + component.style.height)
  })

  return {
    left,
    top,
    width: right - left,
    height: bottom - top
  }
}

const visitChildren = (component: ComponentConfig, callback: (child: ComponentConfig) => void) => {
  if (!component) {
    return
  }
  if (Array.isArray(component.propValue)) {
    if (component.component === 'Group' || component.innerType === 'Group') {
      component.propValue.forEach(child => {
        callback(child)
        visitChildren(child, callback)
      })
      return
    }
    if (component.component === 'Tabs' || component.innerType === 'Tabs') {
      component.propValue.forEach(tab => {
        ;(tab?.componentData || []).forEach(child => {
          callback(child)
          visitChildren(child, callback)
        })
      })
    }
  }
}

const clearChildFrame = (component: ComponentConfig) => {
  if (component.commonBackground) {
    component.commonBackground.backgroundColorSelect = false
    component.commonBackground.backgroundImageEnable = false
    component.commonBackground.backdropFilterEnable = false
    component.commonBackground.innerPadding = zeroEdgeValues()
    component.commonBackground.borderRadius = zeroCornerValues()
  }
  if (component.style) {
    component.style.borderActive = false
    component.style.borderWidth = 0
    component.style.borderRadius = 0
  }
}

const isFiniteNumber = (value: unknown) => typeof value === 'number' && Number.isFinite(value)

const syncGroupStyle = (component: ComponentConfig, group: ComponentConfig) => {
  if (!component.groupStyle || !component.style || !group.style) {
    return
  }
  if (!isFiniteNumber(group.style.width) || !isFiniteNumber(group.style.height)) {
    return
  }
  if (!group.style.width || !group.style.height) {
    return
  }
  component.groupStyle.left = component.style.left / group.style.width
  component.groupStyle.top = component.style.top / group.style.height
  component.groupStyle.width = component.style.width / group.style.width
  component.groupStyle.height = component.style.height / group.style.height
}

const compactDirectChildren = (group: ComponentConfig) => {
  const children = group.propValue.filter(child => {
    const style = child?.style
    return (
      isFiniteNumber(style?.left) &&
      isFiniteNumber(style?.top) &&
      isFiniteNumber(style?.width) &&
      isFiniteNumber(style?.height)
    )
  })
  if (children.length < 2) {
    return
  }
  if (children.length !== group.propValue.length) {
    return
  }

  const sameRow = children.every(child => {
    return child.style.top === children[0].style.top && child.style.height === children[0].style.height
  })
  const sameColumn = children.every(
    child =>
      child.style.left === children[0].style.left && child.style.width === children[0].style.width
  )
  if (!sameRow && !sameColumn) {
    return
  }
  const horizontal = sameRow
  let cursor = 0

  children
    .slice()
    .sort((left, right) =>
      horizontal ? left.style.left - right.style.left : left.style.top - right.style.top
    )
    .forEach(child => {
      if (horizontal) {
        child.style.left = cursor
        cursor += child.style.width
      } else {
        child.style.top = cursor
        cursor += child.style.height
      }
      syncGroupStyle(child, group)
    })
}

const getLayoutChildren = (group: ComponentConfig) => {
  if (!Array.isArray(group.propValue)) {
    return []
  }
  const children = group.propValue.filter(child => {
    const style = child?.style
    return (
      isFiniteNumber(style?.left) &&
      isFiniteNumber(style?.top) &&
      isFiniteNumber(style?.width) &&
      isFiniteNumber(style?.height)
    )
  })
  return children.length === group.propValue.length ? children : []
}

const applyLinearLayout = (
  group: ComponentConfig,
  direction: 'horizontal' | 'vertical',
  gap: number
) => {
  const children = getLayoutChildren(group)
  if (children.length < 2) {
    return false
  }
  const horizontal = direction === 'horizontal'
  let cursor = 0
  children
    .slice()
    .sort((left, right) =>
      horizontal ? left.style.left - right.style.left : left.style.top - right.style.top
    )
    .forEach(child => {
      if (horizontal) {
        child.style.left = cursor
        cursor += child.style.width + gap
      } else {
        child.style.top = cursor
        cursor += child.style.height + gap
      }
      syncGroupStyle(child, group)
    })
  return true
}

const groupByPosition = (children: ComponentConfig[], key: 'top' | 'left') => {
  const values = Array.from(new Set(children.map(child => child.style[key]))).sort((a, b) => a - b)
  return values.map(value =>
    children
      .filter(child => child.style[key] === value)
      .sort((left, right) => left.style.left - right.style.left)
  )
}

const applyGridLayout = (group: ComponentConfig, horizontalGap: number, verticalGap: number) => {
  const children = getLayoutChildren(group)
  if (children.length < 4) {
    return false
  }
  const rows = groupByPosition(children, 'top')
  const columnCount = rows[0]?.length || 0
  if (rows.length < 2 || columnCount < 2 || rows.some(row => row.length !== columnCount)) {
    return false
  }

  const columnLefts = rows[0].map(child => child.style.left)
  const aligned = rows.every(row => row.every((child, index) => child.style.left === columnLefts[index]))
  if (!aligned) {
    return false
  }

  const columnWidths = rows[0].map(child => child.style.width)
  const rowHeights = rows.map(row => Math.max(...row.map(child => child.style.height)))
  rows.forEach((row, rowIndex) => {
    const top = rowHeights.slice(0, rowIndex).reduce((total, height) => total + height, 0)
    row.forEach((child, columnIndex) => {
      child.style.left =
        columnWidths.slice(0, columnIndex).reduce((total, width) => total + width, 0) +
        columnIndex * horizontalGap
      child.style.top = top + rowIndex * verticalGap
      syncGroupStyle(child, group)
    })
  })
  return true
}

export function applyChartGroupLayout(
  group: ComponentConfig,
  mode: ChartGroupLayoutMode = 'keep',
  options: { horizontalGap?: number; verticalGap?: number } = {}
) {
  if (!isGroup(group)) {
    return false
  }

  normalizeChartGroupContainer(group)
  const horizontalGap = Number(options.horizontalGap) || 0
  const verticalGap = Number(options.verticalGap) || 0
  if (mode === 'keep') {
    group.chartGroupContainer.layoutMode = mode
    group.chartGroupContainer.horizontalGap = horizontalGap
    group.chartGroupContainer.verticalGap = verticalGap
    return true
  }
  let applied = false
  if (mode === 'horizontal') {
    applied = applyLinearLayout(group, 'horizontal', horizontalGap)
  } else if (mode === 'vertical') {
    applied = applyLinearLayout(group, 'vertical', verticalGap)
  } else {
    applied = applyGridLayout(group, horizontalGap, verticalGap)
  }
  if (applied) {
    group.chartGroupContainer.layoutMode = mode
    group.chartGroupContainer.horizontalGap = horizontalGap
    group.chartGroupContainer.verticalGap = verticalGap
  }
  return applied
}

export function applyChartGroupSeamlessLayout(group: ComponentConfig) {
  if (!isGroup(group)) {
    return group
  }

  normalizeChartGroupContainer(group)
  group.chartGroupContainer.seamless = true
  group.chartGroupContainer.clipOverflow = true
  group.commonBackground.innerPadding = zeroEdgeValues()
  compactDirectChildren(group)

  group.propValue.forEach(child => {
    clearChildFrame(child)
    visitChildren(child, clearChildFrame)
  })

  return group
}
