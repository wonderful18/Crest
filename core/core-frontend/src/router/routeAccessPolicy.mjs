export const isReadOnlyRoute = (path = '', name) =>
  path === '/preview' ||
  path === '/previewShow' ||
  path === '/dashboardPreview' ||
  path === '/chart-view' ||
  path.startsWith('/link/') ||
  name === 'link'

export const shouldBypassAuthenticatedMenuGuard = route => {
  return isReadOnlyRoute(route?.path, route?.name)
}
