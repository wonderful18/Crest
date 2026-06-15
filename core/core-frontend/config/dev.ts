// 处理拖拽交互并更新目标位置
const dropBrowserOriginHeader = proxy => {
  proxy.on('proxyReq', proxyReq => {
    proxyReq.removeHeader('origin')
  })
}

const backendProxyTarget = process.env.CREST_DEV_PROXY_TARGET || 'http://10.25.25.33:8100'

export default {
  server: {
    proxy: {
      '/api/f': {
        target: backendProxyTarget,
        changeOrigin: true,
        configure: dropBrowserOriginHeader,
        rewrite: path => path.replace(/^\/api\/f/, '')
      },
      // 使用 proxy 实例
      '/api': {
        target: backendProxyTarget,
        changeOrigin: true,
        configure: dropBrowserOriginHeader
      }
    },
    port: 8080
  }
}
