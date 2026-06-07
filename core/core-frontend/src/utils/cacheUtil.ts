import { useCache } from '@/hooks/web/useCache'
import { clearAuthCache } from '@/utils/authCache'
const { wsCache } = useCache()
export const clearCache = () => {
  clearAuthCache()
  const keys = [
    'CrestKey',
    'TreeSort-backend',
    'app.desktop',
    'open-backend',
    'panel-weight',
    'screen-weight',
    'user.language',
    'user.name',
    'user.oid',
    'user.uid'
  ]
  keys.forEach(key => {
    wsCache.delete(key)
  })
}
