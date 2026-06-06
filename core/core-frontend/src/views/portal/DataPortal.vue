<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import dayjs from 'dayjs'
import router from '@/router'
import crestLogo from '@/assets/img/crest-logo-horizontal-192h.png'
import { logoutApi } from '@/api/login'
import { portalOverviewApi, portalResourcesApi } from '@/api/dataPortal'
import { useUserStoreWithOut } from '@/store/modules/user'
import { logoutHandler } from '@/utils/logout'
import EmptyBackground from '@/components/empty-background/src/EmptyBackground.vue'

interface PortalResource {
  id: string
  name: string
  type: string
  creatorName?: string
  updateTime?: number
  chartCount?: number
}

const userStore = useUserStoreWithOut()

const state = reactive({
  loading: false,
  keyword: '',
  type: 'all',
  page: 1,
  pageSize: 24,
  total: 0,
  records: [] as PortalResource[],
  overview: {
    total: 0,
    screenCount: 0,
    dashboardCount: 0,
    latestUpdateTime: null as number | null,
    backendAccess: false
  }
})

const backendAccess = computed(() => userStore.getBackendAccess || state.overview.backendAccess)
const userName = computed(() => userStore.getName || '用户')
const userInitial = computed(() => userName.value.substring(0, 1))
const featuredResource = computed(() => state.records[0] || null)
const galleryResources = computed(() => state.records.slice(1))
const openCount = computed(() => state.total || state.overview.total)

const tabs = computed(() => [
  { label: '全部', value: 'all', count: state.overview.total },
  { label: '大屏', value: 'dataV', count: state.overview.screenCount },
  { label: '仪表盘', value: 'dashboard', count: state.overview.dashboardCount }
])

const typeLabel = (type: string) => (type === 'dashboard' ? '仪表盘' : '数据大屏')
const typeAccent = (type: string) => (type === 'dashboard' ? '#3b82f6' : '#10b981')
const formatTime = (time?: number | null, formatter = 'YYYY-MM-DD HH:mm') =>
  time ? dayjs(time).format(formatter) : '-'
const latestTime = computed(() => formatTime(state.overview.latestUpdateTime, 'HH:mm'))
const latestDateTime = computed(() => formatTime(state.overview.latestUpdateTime))

const loadOverview = async () => {
  const res = await portalOverviewApi()
  Object.assign(state.overview, res?.data || {})
}

const loadResources = async () => {
  state.loading = true
  try {
    const res = await portalResourcesApi({
      keyword: state.keyword,
      type: state.type,
      page: state.page,
      pageSize: state.pageSize
    })
    state.records = res?.data?.records || []
    state.total = Number(res?.data?.total || 0)
  } finally {
    state.loading = false
  }
}

const reload = async () => {
  await loadOverview()
  await loadResources()
}

const search = () => {
  state.page = 1
  loadResources()
}

const switchType = (type: string) => {
  state.type = type
  state.page = 1
  loadResources()
}

const pageChange = (page: number) => {
  state.page = page
  loadResources()
}

const openResource = (resource: PortalResource) => {
  router.push({
    path: `/portal/view/${resource.id}`,
    query: { dvType: resource.type }
  })
}

const enterBackend = () => {
  router.push('/workbranch/index')
}

const logout = async () => {
  await logoutApi()
  logoutHandler()
}

onMounted(reload)
</script>

<template>
  <main class="portal-page">
    <header class="portal-header">
      <div class="brand">
        <img :src="crestLogo" alt="Crest" />
        <span class="divider" />
        <span class="portal-mark">
          <strong>数据门户</strong>
          <small>DATA PORTAL</small>
        </span>
      </div>
      <div class="header-actions">
        <span class="sync-pill"><i />数据同步</span>
        <button class="user-pill" type="button">
          <span class="avatar">{{ userInitial }}</span>
          <span>{{ userName }}</span>
        </button>
        <el-button v-if="backendAccess" secondary @click="enterBackend">进入后台</el-button>
        <button class="logout-btn" type="button" @click="logout">退出</button>
      </div>
    </header>

    <section class="portal-tools">
      <div class="summary">
        <h1>
          你好，<span>{{ userName }}</span>
        </h1>
        <p>
          已开放 <b>{{ openCount }}</b> 项 · 最近更新 <b>{{ latestTime }}</b>
        </p>
      </div>
      <div class="stats">
        <div class="stat-item blue">
          <strong>{{ state.overview.screenCount }}</strong>
          <span>大屏</span>
        </div>
        <div class="stat-item green">
          <strong>{{ state.overview.dashboardCount }}</strong>
          <span>仪表盘</span>
        </div>
        <div class="stat-item amber">
          <strong>{{ state.overview.latestUpdateTime ? '实时' : '-' }}</strong>
          <span>状态</span>
        </div>
      </div>
      <div class="filters">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          :class="{ active: state.type === tab.value }"
          @click="switchType(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="search-box">
        <el-input
          v-model.trim="state.keyword"
          placeholder="搜索名称"
          clearable
          @clear="search"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </section>

    <section v-loading="state.loading" class="portal-wall">
      <button
        v-if="featuredResource"
        class="feature-card"
        type="button"
        @click="openResource(featuredResource)"
      >
        <span
          class="screen-preview large"
          :style="{ '--accent': typeAccent(featuredResource.type) }"
        >
          <span class="preview-head">
            <i />
            <strong>{{ featuredResource.name }}</strong>
            <em>实时</em>
          </span>
          <span class="preview-kpis">
            <span>
              <label>组件</label>
              <b>{{ featuredResource.chartCount || 0 }}</b>
            </span>
            <span>
              <label>类型</label>
              <b>{{ typeLabel(featuredResource.type) }}</b>
            </span>
            <span>
              <label>状态</label>
              <b>已发布</b>
            </span>
          </span>
          <svg viewBox="0 0 520 160" preserveAspectRatio="none" aria-hidden="true">
            <path d="M26 118 L128 84 L236 98 L348 62 L494 78" />
            <path class="line-alt" d="M26 92 L128 104 L236 66 L348 94 L494 48" />
          </svg>
        </span>
        <span class="feature-info">
          <span class="badge"><i />重点内容</span>
          <strong>{{ featuredResource.name }}</strong>
          <span class="meta">
            {{ typeLabel(featuredResource.type) }} · 组件 {{ featuredResource.chartCount || 0 }} ·
            更新 {{ formatTime(featuredResource.updateTime) }}
          </span>
          <span class="enter-btn">进入查看</span>
        </span>
      </button>

      <div v-if="galleryResources.length" class="wall-grid">
        <button
          v-for="item in galleryResources"
          :key="item.id"
          class="poster-card"
          type="button"
          @click="openResource(item)"
        >
          <span class="screen-preview" :style="{ '--accent': typeAccent(item.type) }">
            <span class="preview-head">
              <i />
              <strong>{{ item.name }}</strong>
              <em>实时</em>
            </span>
            <span class="preview-kpis">
              <span>
                <label>组件</label>
                <b>{{ item.chartCount || 0 }}</b>
              </span>
              <span>
                <label>状态</label>
                <b>已发布</b>
              </span>
            </span>
            <svg viewBox="0 0 320 120" preserveAspectRatio="none" aria-hidden="true">
              <path d="M18 78 L82 54 L152 68 L218 40 L302 58" />
              <path class="line-alt" d="M18 58 L82 70 L152 42 L218 64 L302 34" />
            </svg>
          </span>
          <span class="poster-info">
            <span class="title">{{ item.name }}</span>
            <span class="meta">
              <span class="type-dot" :style="{ background: typeAccent(item.type) }" />
              {{ typeLabel(item.type) }} · {{ formatTime(item.updateTime, 'MM-DD HH:mm') }}
            </span>
          </span>
          <span class="poster-go">进入</span>
        </button>
      </div>

      <empty-background
        v-if="!state.loading && !state.records.length"
        description="暂无内容"
        img-type="noneWhite"
      />

      <div v-if="state.total > state.pageSize" class="pager">
        <el-pagination
          layout="prev, pager, next"
          :current-page="state.page"
          :page-size="state.pageSize"
          :total="state.total"
          @current-change="pageChange"
        />
      </div>
    </section>
  </main>
</template>

<style scoped lang="less">
.portal-page {
  min-height: 100vh;
  color: #0f172a;
  background: radial-gradient(circle at 1px 1px, rgba(59, 130, 246, 0.08) 1px, transparent 0) 0 0/24px
      24px,
    #f6f8fb;
  font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
}

.portal-header {
  height: 66px;
  padding: 0 40px;
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e6ebf2;
  background: radial-gradient(circle at 1px 1px, rgba(59, 130, 246, 0.12) 1px, transparent 0) 0 0/24px
      24px,
    linear-gradient(118deg, #e9f1fe 0%, #f3f8ff 46%, #ffffff 100%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 16px;
  img {
    height: 34px;
  }
}

.divider {
  width: 1px;
  height: 24px;
  background: #e6ebf2;
}

.portal-mark {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
  strong {
    font-size: 15px;
  }
  small {
    color: #64748b;
    font-size: 11px;
    letter-spacing: 0.04em;
  }
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.sync-pill {
  height: 30px;
  padding: 0 13px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border-radius: 999px;
  color: #10b981;
  background: #e5f7f0;
  font-size: 12px;
  font-weight: 600;
  i {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #10b981;
    box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.12);
  }
}

.user-pill,
.logout-btn {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.user-pill {
  height: 40px;
  padding: 5px 14px 5px 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #e6ebf2;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  color: #0f172a;
  font-weight: 600;
}

.avatar {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #ffffff;
  background: linear-gradient(135deg, #34d399, #0ea5e9);
}

.logout-btn {
  height: 36px;
  padding: 0 10px;
  color: #334155;
  font-weight: 600;
  &:hover {
    color: #0f172a;
  }
}

.portal-tools {
  padding: 20px 40px;
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
}

.summary {
  h1 {
    margin: 0;
    font-size: 21px;
    line-height: 28px;
    font-weight: 700;
    span {
      color: #3b82f6;
    }
  }
  p {
    margin: 3px 0 0;
    color: #64748b;
    font-size: 12px;
    b {
      color: #334155;
      font-weight: 700;
    }
  }
}

.stats {
  padding-left: 24px;
  display: flex;
  gap: 26px;
  border-left: 1px solid #e6ebf2;
}

.stat-item {
  strong,
  span {
    display: block;
  }
  strong {
    font-size: 22px;
    line-height: 1;
    font-weight: 800;
  }
  span {
    margin-top: 5px;
    color: #64748b;
    font-size: 11px;
  }
  &.blue strong {
    color: #3b82f6;
  }
  &.green strong {
    color: #10b981;
  }
  &.amber strong {
    color: #f59e0b;
  }
}

.filters {
  margin-left: auto;
  display: flex;
  gap: 8px;
  button {
    height: 34px;
    padding: 0 15px;
    border: 1px solid #e6ebf2;
    border-radius: 9px;
    color: #334155;
    background: #ffffff;
    font-weight: 600;
    cursor: pointer;
    &.active {
      color: #ffffff;
      border-color: #0f172a;
      background: #0f172a;
    }
  }
}

.search-box {
  width: 320px;
  display: flex;
  gap: 8px;
}

.portal-wall {
  min-height: 420px;
  padding: 4px 40px 48px;
}

.feature-card,
.poster-card {
  width: 100%;
  padding: 0;
  border: 1px solid #e6ebf2;
  overflow: hidden;
  text-align: left;
  background: #ffffff;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(15, 23, 42, 0.03);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  &:hover {
    transform: translateY(-3px);
    border-color: #a6c7fa;
    box-shadow: 0 20px 44px -18px rgba(11, 20, 38, 0.42);
  }
}

.feature-card {
  margin-bottom: 20px;
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 1fr);
  border-radius: 18px;
}

.screen-preview {
  --accent: #3b82f6;
  min-height: 188px;
  padding: 14px 16px;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #e2e8f0;
  background: radial-gradient(120% 80% at 0% 0%, rgba(59, 130, 246, 0.16), transparent 60%),
    linear-gradient(160deg, #0e1a30 0%, #0b1426 100%);
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    background-image: radial-gradient(
      circle at 1px 1px,
      rgba(255, 255, 255, 0.06) 1px,
      transparent 0
    );
    background-size: 18px 18px;
  }
  &.large {
    min-height: 280px;
    padding: 18px 20px;
  }
  svg {
    flex: 1;
    min-height: 88px;
    margin-top: 8px;
    position: relative;
    z-index: 1;
    path {
      fill: none;
      stroke: var(--accent);
      stroke-width: 3;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
    .line-alt {
      stroke: #60a5fa;
      opacity: 0.82;
    }
  }
}

.preview-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  i {
    width: 7px;
    height: 7px;
    flex: 0 0 auto;
    border-radius: 50%;
    background: var(--accent);
  }
  strong {
    min-width: 0;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 12px;
  }
  em {
    color: #34d399;
    font-size: 10px;
    font-style: normal;
  }
}

.preview-kpis {
  margin-top: 12px;
  position: relative;
  z-index: 1;
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  label {
    display: block;
    color: #8fa0b6;
    font-size: 10px;
  }
  b {
    display: block;
    margin-top: 2px;
    color: var(--accent);
    font-size: 18px;
    line-height: 1.15;
  }
}

.feature-info {
  padding: 30px 34px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  .badge {
    width: fit-content;
    height: 26px;
    padding: 0 12px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    border-radius: 999px;
    color: #2f6bd4;
    background: #f1f6fe;
    font-size: 12px;
    font-weight: 700;
    i {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #3b82f6;
    }
  }
  strong {
    margin-top: 16px;
    color: #0f172a;
    font-size: 25px;
    line-height: 1.18;
    font-weight: 800;
  }
  .meta {
    margin-top: 12px;
    color: #64748b;
    line-height: 22px;
  }
}

.enter-btn {
  width: fit-content;
  height: 42px;
  margin-top: auto;
  padding: 0 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  color: #ffffff;
  background: #3b82f6;
  font-weight: 700;
  box-shadow: 0 8px 20px -8px rgba(59, 130, 246, 0.6);
}

.wall-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.poster-card {
  position: relative;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
}

.poster-info {
  min-height: 82px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  .title {
    overflow: hidden;
    color: #0f172a;
    font-size: 15px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .meta {
    display: flex;
    align-items: center;
    gap: 7px;
    color: #94a3b8;
    font-size: 12px;
  }
}

.type-dot {
  width: 6px;
  height: 6px;
  flex: 0 0 auto;
  border-radius: 50%;
}

.poster-go {
  position: absolute;
  right: 14px;
  bottom: 14px;
  color: #64748b;
  font-weight: 700;
  transition: color 0.15s ease;
}

.poster-card:hover .poster-go {
  color: #3b82f6;
}

.pager {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .feature-card {
    grid-template-columns: 1fr;
  }
  .wall-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .filters {
    margin-left: 0;
  }
}

@media (max-width: 760px) {
  .portal-header,
  .portal-tools,
  .portal-wall {
    padding-left: 16px;
    padding-right: 16px;
  }
  .portal-header {
    height: auto;
    min-height: 66px;
    padding-top: 12px;
    padding-bottom: 12px;
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
  .header-actions,
  .portal-tools,
  .stats {
    align-items: flex-start;
    flex-direction: column;
  }
  .stats {
    padding-left: 0;
    border-left: 0;
  }
  .search-box {
    width: 100%;
  }
  .wall-grid {
    grid-template-columns: 1fr;
  }
}
</style>
