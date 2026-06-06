<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router_2'
import router from '@/router'
import crestLogo from '@/assets/svg/logo.svg?url'
import { portalResourceApi } from '@/api/dataPortal'
import EmptyBackground from '@/components/empty-background/src/EmptyBackground.vue'
import PortalAccount from './PortalAccount.vue'

interface PortalResource {
  id: string
  name: string
  type: string
}

const route = useRoute()
const resource = ref<PortalResource | null>(null)
const loading = ref(false)
const error = ref('')
const frameKey = ref(0)
const frameRef = ref<HTMLIFrameElement>()

const resourceId = computed(() => String(route.params.id || ''))
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
    <header class="portal-header view-header">
      <div class="brand">
        <img class="logo" :src="crestLogo" alt="Crest" @click="back" />
        <span class="divider" />
        <span class="portal-title">研发效能指标管理系统</span>
      </div>
      <div class="resource-title">
        <strong :title="resource?.name || '数据查看'">{{ resource?.name || '数据查看' }}</strong>
      </div>
      <div class="header-actions">
        <el-tooltip effect="dark" content="返回门户" placement="bottom">
          <button class="top-action" type="button" @click="back">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M15 18 9 12l6-6M10 12h10"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </el-tooltip>
        <el-tooltip effect="dark" content="刷新" placement="bottom">
          <button class="top-action" type="button" @click="reload">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M19 8a7 7 0 0 0-12.1-3.9L5 6m0 0V2m0 4h4m-4 6a7 7 0 0 0 12.1 3.9L19 14m0 0v4m0-4h-4"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </el-tooltip>
        <el-tooltip effect="dark" content="全屏" placement="bottom">
          <button class="top-action" type="button" @click="fullScreen">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M8 4H4v4m0 8v4h4m8-16h4v4m0 8v4h-4"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </el-tooltip>
        <PortalAccount />
      </div>
    </header>

    <section class="resource-strip">
      <div class="resource-strip-inner">
        <div>
          <strong>{{ resource?.name || '数据查看' }}</strong>
        </div>
      </div>
    </section>

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
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  overflow: hidden;
  color: #0f172a;
  background: #f8fafc;
  font-family: var(--crest-font-sans), 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.portal-header {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(120px, 1fr) auto;
  column-gap: clamp(28px, 4vw, 56px);
  align-items: center;
  height: 60px;
  padding: 0 clamp(28px, 5vw, 72px);
  overflow: hidden;
  background: linear-gradient(180deg, #edf2fb 0%, #fafbfe 100%);
  border-bottom: 1px solid #e2e8f0;

  &::before {
    position: absolute;
    inset: 0;
    pointer-events: none;
    content: '';
    background-image: radial-gradient(circle at 1px 1px, #3b6fd0 1px, transparent 0);
    background-size: 20px 20px;
    opacity: 0.05;
  }

  > * {
    position: relative;
    z-index: 1;
  }
}

.brand {
  flex: none;
  display: flex;
  align-items: center;

  .logo {
    width: 188px;
    height: 44px;
    margin-right: clamp(44px, 4vw, 54px);
    cursor: pointer;
    object-fit: contain;
  }
}

.divider {
  width: 1px;
  height: 24px;
  margin-right: clamp(26px, 2vw, 32px);
  background: #e2e8f0;
}

.portal-title {
  flex: none;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.resource-title {
  display: flex;
  justify-content: center;
  min-width: 0;

  strong {
    max-width: min(100%, 360px);
    overflow: hidden;
    color: #0f172a;
    font-size: 15px;
    font-weight: 700;
    line-height: 22px;
    text-align: center;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.header-actions {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;

  :deep(.ed-button) {
    height: 34px;
    padding: 0 12px;
    color: #64748b;
    font-family: var(--crest-font-sans);
    font-size: 14px;
    font-weight: 600;
    background: transparent;
    border: 0;
    border-radius: 8px;
    transition: color 0.14s ease, background 0.14s ease;
  }

  :deep(.ed-button:hover),
  :deep(.ed-button:focus) {
    color: #0f172a;
    background: #f1f5f9;
  }
}

.top-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 8px;
  transition: color 0.14s ease, background 0.14s ease;

  &:hover {
    color: #0f172a;
    background: #f1f5f9;
  }
}

.resource-strip {
  display: none;
  padding: 12px 16px 0;
  background: #f8fafc;
}

.resource-strip-inner {
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;

  div {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  strong {
    overflow: hidden;
    color: #0f172a;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: #64748b;
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
  height: calc(100vh - 60px);
  display: block;
  border: 0;
  background: #ffffff;
}

@media (max-width: 720px) {
  .portal-view {
    overflow: auto;
  }

  .portal-header {
    height: auto;
    min-height: 60px;
    grid-template-columns: minmax(0, 1fr) auto;
    padding: 10px 16px;
  }

  .brand .logo {
    width: 150px;
    margin-right: 18px;
  }

  .divider,
  .portal-title,
  .resource-title {
    display: none;
  }

  .header-actions {
    flex-wrap: wrap;
    gap: 8px;
  }

  .header-actions :deep(.account-name) {
    display: none;
  }

  .resource-strip {
    display: block;
  }

  .preview-frame {
    height: calc(100vh - 144px);
  }
}
</style>
