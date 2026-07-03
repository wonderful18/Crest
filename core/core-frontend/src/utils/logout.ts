import { useUserStoreWithOut } from '@/store/modules/user'
import router from '@/router'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { interactiveStoreWithOut } from '@/store/modules/interactive'
import { useCache } from '@/hooks/web/useCache'

const { wsCache } = useCache()
const permissionStore = usePermissionStoreWithOut()
const userStore = useUserStoreWithOut()
const interactiveStore = interactiveStoreWithOut()

// 整理输入数据并返回工具处理结果
export const logoutHandler = (justClean?: boolean, save_platform_status = false) => {
  userStore.clear()
  userStore.$reset()
  permissionStore.clear()
  permissionStore.$reset()
  interactiveStore.clear()
  interactiveStore.$reset()
  removeCache()
  let queryRedirectPath = '/workbranch/index'
  // 如果redirect参数中有值
  if (router.currentRoute.value.fullPath) {
    queryRedirectPath = router.currentRoute.value.fullPath as string
  }
  let pathname = window.location.pathname
  if (pathname) {
    if (pathname.includes('oidcbi/')) {
      if (save_platform_status) {
        return
      }
      pathname = pathname.replace('oidcbi/', '')
      if (pathname.includes('mobile.html')) {
        pathname = pathname.replace('mobile.html', '')
      }
      pathname = pathname.substring(0, pathname.length - 1)
      window.location.assign(buildSameOriginUrl(pathname, '/oidcbi/oidc/logout'))
      return
    } else if (pathname.includes('casbi/')) {
      if (save_platform_status) {
        return
      }
      pathname = pathname.replace('casbi/', '')
      if (pathname.includes('mobile.html')) {
        pathname = pathname.replace('mobile.html', '')
      }
      pathname = pathname.substring(0, pathname.length - 1)
      const logoutUrl = new URL(buildSameOriginUrl(pathname, '/casbi/cas/logout'))
      logoutUrl.searchParams.set('service', window.location.href)
      window.location.assign(logoutUrl.toString())
      return
    }
    pathname = pathname.substring(0, pathname.length - 1)
  }
  const customLogoutUrl = safeLogoutUrl(wsCache.get('custom_auth_logout_url'))
  if (customLogoutUrl) {
    window.location.assign(customLogoutUrl)
  }
  router.push(justClean ? queryRedirectPath : `/login?redirect=${queryRedirectPath}`)
}

export const buildSameOriginUrl = (basePath: string, logoutPath: string) => {
  return new URL(`${basePath}${logoutPath}`, window.location.origin).toString()
}

export const safeLogoutUrl = (url?: string) => {
  if (!url) return null
  try {
    const target = new URL(url, window.location.origin)
    if (!['http:', 'https:'].includes(target.protocol)) {
      return null
    }
    if (target.username || target.password) {
      return null
    }
    return target.href
  } catch {
    return null
  }
}

// 移除当前数据并同步关联状态
const removeCache = () => {
  const keys = Object.keys(wsCache['storage'])
  keys.forEach(key => {
    if (
      key.startsWith('crest-plugin-') ||
      key === 'crest-platform-client' ||
      key === 'pwd-validity-period' ||
      key === 'pluginBridge-model-distributed'
    ) {
      wsCache.delete(key)
    }
  })
}
