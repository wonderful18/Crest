const loginPaths = new Set(['/login', '/admin-login'])

export const isLoginPath = path => {
  return loginPaths.has(path)
}

export const resolveAuthFailureNavigation = to => {
  if (isLoginPath(to.path)) {
    return {
      type: 'allow'
    }
  }
  return {
    type: 'redirect',
    location: {
      path: '/login',
      query: {
        redirect: to.fullPath || to.path
      }
    }
  }
}
