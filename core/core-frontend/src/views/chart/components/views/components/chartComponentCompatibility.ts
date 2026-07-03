export const hasConfiguredDrillFields = (view?: Record<string, any> | null): boolean => {
  return Array.isArray(view?.drillFields) && view.drillFields.length > 0
}
