import { useCache } from '@/hooks/web/useCache'

const { wsCache: sessionCache } = useCache('sessionStorage')
const { wsCache: localCache } = useCache('localStorage')

const authKeys = ['user.token', 'user.exp', 'user.time', 'crest-global-refresh']

export const authCache = {
  get(key: string) {
    const sessionValue = sessionCache.get(key)
    if (sessionValue !== null && sessionValue !== undefined) {
      return sessionValue
    }
    if (!authKeys.includes(key)) {
      return sessionValue
    }
    const legacyValue = localCache.get(key)
    if (legacyValue !== null && legacyValue !== undefined) {
      sessionCache.set(key, legacyValue)
      localCache.delete(key)
    }
    return legacyValue
  },
  set(key: string, value: any, options?: any) {
    sessionCache.set(key, value, options)
    if (authKeys.includes(key)) {
      localCache.delete(key)
    }
  },
  delete(key: string) {
    sessionCache.delete(key)
    if (authKeys.includes(key)) {
      localCache.delete(key)
    }
  }
}

export const clearAuthCache = () => {
  authKeys.forEach(key => authCache.delete(key))
}
