import os from 'node:os'

// 移除浏览器 Origin 头，避免本地代理转发时触发后端跨域校验
const dropBrowserOriginHeader = proxy => {
  proxy.on('proxyReq', proxyReq => {
    proxyReq.removeHeader('origin')
  })
}

// 本地开发默认使用当前网卡地址，换网络时无需手工调整代理目标
const resolveNetworkAddress = () => {
  const preferredInterface = process.env.CREST_DEV_PROXY_INTERFACE
  const interfaces = os.networkInterfaces()
  const allEntries = Object.entries(interfaces)
  const entries = preferredInterface
    ? [[preferredInterface, interfaces[preferredInterface] || []]]
    : [
        ...allEntries.filter(([name]) => /^en\d+$/u.test(name)),
        ...allEntries.filter(([name]) => !/^en\d+$/u.test(name) && !/^lo\d*$/u.test(name))
      ]

  for (const [, addresses] of entries) {
    const matched = addresses.find(address => {
      const family = typeof address.family === 'string' ? address.family : `IPv${address.family}`
      return family === 'IPv4' && !address.internal
    })

    if (matched?.address) {
      return matched.address
    }
  }

  return 'localhost'
}

const backendProxyPort = process.env.CREST_DEV_PROXY_PORT || '8100'
const backendProxyTarget =
  process.env.CREST_DEV_PROXY_TARGET || `http://${resolveNetworkAddress()}:${backendProxyPort}`

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
