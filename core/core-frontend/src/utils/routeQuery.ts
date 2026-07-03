type RouteQueryValue = string | number | boolean | null | undefined | Array<string | number | boolean | null | undefined>

export const resolveRouteQueryValue = (
  query: Record<string, RouteQueryValue> | undefined,
  hash: string,
  key: string
): string => {
  const queryValue = query?.[key]
  if (Array.isArray(queryValue)) {
    const [firstValue] = queryValue
    return firstValue === null || firstValue === undefined ? '' : String(firstValue)
  }
  if (queryValue !== null && queryValue !== undefined && queryValue !== '') {
    return String(queryValue)
  }

  const queryStart = hash.indexOf('?')
  if (queryStart === -1) {
    return ''
  }

  return new URLSearchParams(hash.slice(queryStart + 1)).get(key) || ''
}
