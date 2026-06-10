const loginPaths = new Set(['/login', '/admin-login'])

export const resolveAuthFailureNavigation = to => {
  if (loginPaths.has(to.path)) {
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
