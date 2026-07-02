import { secureRandomInt } from '@/utils/secureRandom'

export type ImportedTemplateCanvasType = 'dataV' | 'dashboard' | string

export const isSupportedVisualizationTemplateFile = (fileName?: string): boolean => {
  return /\.(det2|det2app)$/i.test(fileName || '')
}

export const buildImportedTemplateCacheKey = (now = Date.now): string => {
  return `CREST-VISUALIZATION-IMPORT-${now()}-${secureRandomInt(0x100000000)
    .toString(36)
    .padStart(7, '0')}`
}

export const buildImportedTemplateEditorUrl = (
  curCanvasType: ImportedTemplateCanvasType,
  cacheKey: string
): string => {
  const query = `opt=create&createType=uploadedTemplate&templatePayloadKey=${encodeURIComponent(
    cacheKey
  )}`
  return curCanvasType === 'dataV' ? `#/dvCanvas?${query}` : `#/dashboard?${query}`
}

export const navigateImportedTemplateEditor = (
  curCanvasType: ImportedTemplateCanvasType,
  cacheKey: string,
  openWindow: typeof window.open = window.open
): string => {
  const url = buildImportedTemplateEditorUrl(curCanvasType, cacheKey)
  openWindow(url, '_self')
  return url
}

export const resetNativeFileInput = (input?: { value: string } | null): void => {
  if (input) {
    input.value = ''
  }
}
