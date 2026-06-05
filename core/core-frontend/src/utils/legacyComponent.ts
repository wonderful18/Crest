const LEGACY_COMPONENT_NAME_MAP: Record<string, string> = {
  DeDecoration: 'Decoration',
  DeFrame: 'Frame',
  DeGraphical: 'Graphical',
  DeScreen: 'Screen',
  DeStreamMedia: 'StreamMedia',
  DeTabs: 'Tabs',
  DeTimeClock: 'TimeClock',
  DeVideo: 'Video'
}

export function normalizeLegacyComponentName(name?: string) {
  if (!name) {
    return name
  }
  return LEGACY_COMPONENT_NAME_MAP[name] || name
}

export function normalizeLegacyComponent(component) {
  if (!component || typeof component !== 'object') {
    return component
  }

  component.component = normalizeLegacyComponentName(component.component)
  component.innerType = normalizeLegacyComponentName(component.innerType)

  if (component.component === 'Tabs') {
    component.propValue?.forEach(tabItem => {
      normalizeLegacyComponentData(tabItem.componentData)
    })
  } else if (component.component === 'Group') {
    normalizeLegacyComponentData(component.propValue)
  }

  return component
}

export function normalizeLegacyComponentData<T>(componentData: T[]): T[] {
  componentData?.forEach(component => normalizeLegacyComponent(component))
  return componentData
}

