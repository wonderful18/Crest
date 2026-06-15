import { useAppStoreWithOut } from '@/store/modules/app'

const appStore = useAppStoreWithOut()

// 标记异步操作的加载状态
export const usePageLoading = () => {
  // 读取当前业务数据并返回处理结果
  const loadStart = () => {
    appStore.setPageLoading(true)
  }

  // 读取当前业务数据并返回处理结果
  const loadDone = () => {
    appStore.setPageLoading(false)
  }

  return {
    loadStart,
    loadDone
  }
}
