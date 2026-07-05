const dropBrowserOriginHeader = proxy => {
  proxy.on('proxyReq', proxyReq => {
    proxyReq.removeHeader('origin')
  })
}
const DEV_PROXY_TARGET = ['http://localhost', ':8100'].join('')

export default {
  server: {
    proxy: {
      '/api/f': {
        target: DEV_PROXY_TARGET,
        changeOrigin: true,
        configure: dropBrowserOriginHeader,
        rewrite: path => path.replace(/^\/api\/f/, '')
      },
      // 使用 proxy 实例
      '/api': {
        target: DEV_PROXY_TARGET,
        changeOrigin: true,
        configure: dropBrowserOriginHeader
      }
    },
    port: 8080
  }
}
