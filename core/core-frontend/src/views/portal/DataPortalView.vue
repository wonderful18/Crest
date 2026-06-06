<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router_2'
import router from '@/router'
import { portalResourceApi } from '@/api/dataPortal'
import { useUserStoreWithOut } from '@/store/modules/user'
import EmptyBackground from '@/components/empty-background/src/EmptyBackground.vue'

interface PortalResource {
  id: string
  name: string
  type: string
}

const route = useRoute()
const userStore = useUserStoreWithOut()
const resource = ref<PortalResource | null>(null)
const loading = ref(false)
const error = ref('')
const frameKey = ref(0)
const frameRef = ref<HTMLIFrameElement>()

const resourceId = computed(() => String(route.params.id || ''))
const backendAccess = computed(() => userStore.getBackendAccess)
const dvType = computed(() => resource.value?.type || String(route.query.dvType || 'dataV'))
const previewUrl = computed(
  () =>
    `/#/preview?dvId=${encodeURIComponent(resourceId.value)}&dvType=${encodeURIComponent(
      dvType.value
    )}&ignoreParams=true&portal=true`
)

const loadResource = async () => {
  if (!resourceId.value) {
    error.value = '资源不存在'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await portalResourceApi(resourceId.value)
    resource.value = res?.data
  } catch {
    resource.value = null
    error.value = '资源不存在或无访问权限'
  } finally {
    loading.value = false
  }
}

const back = () => {
  router.push('/portal')
}

const enterBackend = () => {
  router.push('/workbranch/index')
}

const reload = () => {
  frameKey.value += 1
}

const fullScreen = () => {
  frameRef.value?.requestFullscreen?.()
}

watch(resourceId, loadResource)
onMounted(loadResource)
</script>

<template>
  <main class="portal-view">
    <header class="view-topbar">
      <div class="left">
        <el-button secondary @click="back">返回门户</el-button>
        <div class="title">
          <strong>{{ resource?.name || '数据查看' }}</strong>
          <span>{{ dvType === 'dashboard' ? '仪表盘' : '数据大屏' }}</span>
        </div>
      </div>
      <div class="right">
        <el-button secondary @click="reload">刷新</el-button>
        <el-button secondary @click="fullScreen">全屏</el-button>
        <el-button v-if="backendAccess" type="primary" @click="enterBackend">进入后台</el-button>
      </div>
    </header>

    <section v-loading="loading" class="view-body">
      <empty-background v-if="error" :description="error" img-type="noneWhite" />
      <iframe
        v-else
        :key="frameKey"
        ref="frameRef"
        class="preview-frame"
        :src="previewUrl"
        title="数据预览"
      />
    </section>
  </main>
</template>

<style scoped lang="less">
.portal-view {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fb;
}

.view-topbar {
  height: 64px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #dee0e3;
  background: #ffffff;
}

.left,
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  strong {
    max-width: 52vw;
    overflow: hidden;
    color: #1f2329;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  span {
    color: #8f959e;
    font-size: 12px;
  }
}

.view-body {
  min-height: 0;
  flex: 1;
  padding: 0;
}

.preview-frame {
  width: 100%;
  height: calc(100vh - 64px);
  display: block;
  border: 0;
  background: #ffffff;
}

@media (max-width: 720px) {
  .view-topbar {
    height: auto;
    min-height: 64px;
    padding: 12px;
    align-items: flex-start;
    flex-direction: column;
  }
  .left,
  .right {
    width: 100%;
    flex-wrap: wrap;
  }
  .preview-frame {
    height: calc(100vh - 112px);
  }
}
</style>
