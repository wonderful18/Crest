const dropBrowserOriginHeader = proxy => {
  proxy.on('proxyReq', proxyReq => {
    proxyReq.removeHeader('origin')
  })
}

export default {
  server: {
    proxy: {
      '/api/f': {
        target: 'http://localhost:8100',
        changeOrigin: true,
        configure: dropBrowserOriginHeader,
        rewrite: path => path.replace(/^\/api\/f/, '')
      },
      // 使用 proxy 实例
      '/api': {
        target: 'http://localhost:8100',
        changeOrigin: true,
        configure: dropBrowserOriginHeader
      }
    },
    port: 8080
  }
}
