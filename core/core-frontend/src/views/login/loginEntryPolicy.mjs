export const NORMAL_LOGIN_PATH = '/login'
export const ADMIN_LOGIN_PATH = '/admin-login'
export const PORTAL_HOME = '/portal'
export const BACKEND_HOME = '/workbranch/index'

const loginPaths = new Set([NORMAL_LOGIN_PATH, ADMIN_LOGIN_PATH])

export const shouldShowLocalLogin = (ssoStatus, path) => {
  return !ssoStatus.enabled || ssoStatus.allowLocalLogin || loginPaths.has(path)
}

export const shouldUseEmergencyLogin = path => {
  return loginPaths.has(path)
}

export const resolveDefaultLoginPath = backendAccess => {
  return backendAccess ? BACKEND_HOME : PORTAL_HOME
}

export const parseLoginRedirect = redirectPath => {
  let queryRedirectPath = redirectPath || PORTAL_HOME
  try {
    let decodedRedirectPath = queryRedirectPath
    for (let i = 0; i < 5; i++) {
      const nextPath = decodeURIComponent(decodedRedirectPath)
      if (nextPath === decodedRedirectPath) {
        break
      }
      decodedRedirectPath = nextPath
    }
    queryRedirectPath = decodedRedirectPath
  } catch {
    // Keep the original path when an invalid escape sequence is passed in.
  }
  queryRedirectPath = String(queryRedirectPath).trim()
  if (!isLocalRedirectPath(queryRedirectPath)) {
    queryRedirectPath = PORTAL_HOME
  }
  const [path, search = ''] = queryRedirectPath.split('?')
  const query = {}
  new URLSearchParams(search).forEach((value, key) => {
    query[key] = value
  })
  return {
    path,
    query
  }
}

export const stringifyLoginRedirect = redirectPath => {
  const redirect = parseLoginRedirect(redirectPath)
  const search = new URLSearchParams(redirect.query).toString()
  return search ? `${redirect.path}?${search}` : redirect.path
}

const isLocalRedirectPath = redirectPath => {
  return (
    redirectPath.startsWith('/') &&
    !redirectPath.startsWith('//') &&
    !redirectPath.includes('\\') &&
    !/^[a-z][a-z\d+.-]*:/i.test(redirectPath) &&
    !/[\u0000-\u001f\u007f]/.test(redirectPath)
  )
}
