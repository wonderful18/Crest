import SockJS from 'sockjs-client/dist/sockjs.min.js'
import Stomp from 'stompjs'
import { useCache } from '@/hooks/web/useCache'
import { useEmitt } from '@/hooks/web/useEmitt'
const { wsCache } = useCache()
let stompClient: Stomp.Client
import dev from '../../config/dev'
const env = import.meta.env
const basePath = env.VITE_API_BASEPATH

const devProxyTarget = () => {
  const proxy = dev.server.proxy
  const exactProxy = proxy[basePath]
  if (exactProxy?.target) {
    return exactProxy.target
  }
  const matchedProxy = Object.entries(proxy).find(([path]) => basePath?.startsWith(path))?.[1]
  return matchedProxy?.target
}

export default {
  install() {
    const channels = [
      {
        topic: '/task-export-topic',
        event: 'task-export-topic-call'
      },
      {
        topic: '/report-notice',
        event: 'report-notice-call'
      }
    ]
    function isLoginStatus() {
      if (wsCache.get('app.desktop')) {
        return true
      }
      return wsCache.get('user.token') && wsCache.get('user.uid')
    }

    function connection() {
      if (!isLoginStatus()) {
        return
      }
      if (stompClient && stompClient.connected) {
        return
      }
      let prefix = '/'
      if (window.CrestBi?.baseUrl) {
        prefix = window.CrestBi.baseUrl
      } else {
        const path = location.pathname.endsWith('/')
          ? location.pathname
          : location.pathname.replace(/\/[^/]*$/, '/')
        prefix = location.origin + path
        if (env.MODE === 'dev') {
          prefix = (devProxyTarget() || location.origin) + '/'
        }
      }
      if (!prefix.endsWith('/')) {
        prefix += '/'
      }
      const userId = wsCache.get('app.desktop') ? 1 : wsCache.get('user.uid')
      const socket = new SockJS(prefix + 'websocket?userId=' + userId)
      stompClient = Stomp.over(socket)
      const heads = {
        userId: userId
      }
      stompClient.connect(
        heads,
        () => {
          channels.forEach(channel => {
            stompClient.subscribe('/user/' + userId + channel.topic, res => {
              res && res.body && useEmitt().emitter.emit(channel.event, res.body)
            })
          })
        },
        error => {
          disconnect()
          console.error('连接失败: ' + error)
        }
      )
    }

    function disconnect() {
      if (stompClient && stompClient.connected) {
        stompClient.disconnect(
          function () {
            console.info('断开连接')
          },
          function (error) {
            console.info('断开连接失败: ' + error)
          }
        )
      }
      stompClient = null
    }

    function initialize() {
      connection()
      const timeInterval = setInterval(() => {
        if (!isLoginStatus()) {
          disconnect()
          return
        }
        if (!stompClient || !stompClient.connected) {
          connection()
        }
      }, 5000)
    }
    initialize()
  }
}
