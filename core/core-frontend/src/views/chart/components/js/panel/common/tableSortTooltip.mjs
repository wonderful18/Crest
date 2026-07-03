export function isSortTooltipRequest(showOptions = {}) {
  if (showOptions.sortMenu === true) {
    return true
  }
  return isCustomSortIconName(showOptions.iconName) || isCustomSortIconName(showOptions.name)
}

export function getTooltipPointerPosition(event = {}) {
  const { clientX, clientY } = event
  if (typeof clientX !== 'number' || typeof clientY !== 'number') {
    return null
  }
  return { x: clientX, y: clientY }
}

export function hasTooltipContent(showOptions = {}) {
  return showOptions.content !== undefined && showOptions.content !== null && showOptions.content !== ''
}

function isCustomSortIconName(iconName) {
  return typeof iconName === 'string' && iconName.startsWith('customSort')
}
