import { useCache } from '@/hooks/web/useCache'
const { wsCache } = useCache()
export const clearCache = () => {
  const keys = [
    'CrestKey',
    'TreeSort-backend',
    'app.desktop',
    'crest-global-refresh',
    'open-backend',
    'panel-weight',
    'screen-weight',
    'user.exp',
    'user.language',
    'user.name',
    'user.oid',
    'user.time',
    'user.token',
    'user.uid'
  ]
  keys.forEach(key => {
    wsCache.delete(key)
  })
}
