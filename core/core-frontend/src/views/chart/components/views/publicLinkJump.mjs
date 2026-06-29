export const isPublicLinkHash = hash => String(hash || '').startsWith('#/link/')

export const buildPublicLinkTargetUrl = ({
  hash,
  publicJumpId,
  targetDvId,
  targetDvType,
  attachParamsInfo = '',
  jumpInfoParam = '',
  editPreviewParams = ''
}) => {
  const currentHash = String(hash || '')
  const [linkPath, queryText = ''] = currentHash.split('?')
  const currentParams = new URLSearchParams(queryText)
  const params = new URLSearchParams()
  const targetLinkPath = publicJumpId ? `#/link/${publicJumpId}` : linkPath
  const ticket = publicJumpId ? '' : currentParams.get('ticket')
  if (ticket) {
    params.set('ticket', ticket)
  }
  params.set('targetDvId', String(targetDvId))
  if (targetDvType) {
    params.set('targetDvType', String(targetDvType))
  }
  params.set('fromLink', 'true')
  let url = `${targetLinkPath}?${params.toString()}`
  if (attachParamsInfo) {
    url += attachParamsInfo
  } else {
    url += '&ignoreParams=true'
  }
  return url + jumpInfoParam + editPreviewParams
}
