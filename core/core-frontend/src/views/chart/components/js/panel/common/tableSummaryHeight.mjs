export const resolveS2ConfiguredHeight = sheet => {
  const candidates = [sheet?.options?.height, sheet?.container?.cfg?.height]
  for (const candidate of candidates) {
    const height = Number(candidate)
    if (Number.isFinite(height) && height > 0) {
      return height
    }
  }
  return 0
}

export const compactSummaryTableHeight = (contentHeight, configuredHeight, padding = 8) => {
  if (!Number.isFinite(contentHeight) || !Number.isFinite(configuredHeight)) {
    return null
  }
  if (contentHeight <= 0 || configuredHeight <= 0 || contentHeight >= configuredHeight) {
    return null
  }
  return contentHeight < configuredHeight - padding ? contentHeight + padding : contentHeight
}
