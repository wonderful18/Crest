<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { TabsPaneContext } from 'element-plus-secondary'
import dayjs from 'dayjs'
import router from '@/router'
import crestMark from '@/assets/svg/crest-mark.svg?url'
import iconCollectionOutlined from '@/assets/svg/icon_collection_outlined.svg'
import userImg from '@/assets/svg/user-img.svg'
import visualStar from '@/assets/svg/visual-star.svg'
import GridTable from '@/components/grid-table/src/GridTable.vue'
import { portalOverviewApi, portalResourcesApi } from '@/api/dataPortal'
import { storeApi } from '@/api/visualization/dataVisualization'
import { useAppearanceStoreWithOut } from '@/store/modules/appearance'
import { useUserStoreWithOut } from '@/store/modules/user'
import { useEmitt } from '@/hooks/web/useEmitt'
import PortalAccount from './PortalAccount.vue'

interface PortalResource {
  id: string | number
  name: string
  type: string
  creatorName?: string
  updateTime?: number
  chartCount?: number
  favorite?: boolean
}

const ALL_RESOURCES = 'all'
const FAVORITES = 'store'
const RECENT = 'recent'
const appearanceStore = useAppearanceStoreWithOut()
const userStore = useUserStoreWithOut()
const activeName = ref(FAVORITES)
const orderDesc = ref(true)
const favoriteLoadingId = ref<string | number | null>(null)
let resourceRequestSeq = 0

const state = reactive({
  loading: false,
  keyword: '',
  type: 'all',
  page: 1,
  pageSize: 20,
  total: 0,
  records: [] as PortalResource[],
  overview: {
    total: 0,
    screenCount: 0,
    dashboardCount: 0,
    latestUpdateTime: null as number | null,
    backendAccess: false
  },
  filterCounts: {
    total: 0,
    screenCount: 0,
    dashboardCount: 0
  }
})

const paneList = [
  { title: '我的收藏', name: FAVORITES },
  { title: '全部资源', name: ALL_RESOURCES },
  { title: '最近使用', name: RECENT }
]

const quickCreationBaseList = [
  {
    icon: '/svg/icon_dashboard.svg',
    name: '仪表盘',
    color: '#3B82F6',
    code: 'DASHBOARD',
    action: 'create-dashboard'
  },
  {
    icon: '/svg/icon_data-visualization.svg',
    name: '数据大屏',
    color: '#1FB6A6',
    code: 'SCREEN',
    action: 'create-screen'
  },
  {
    icon: '/svg/icon_dataset.svg',
    name: '数据集',
    color: '#6E62E8',
    code: 'DATASET',
    action: 'create-dataset'
  },
  {
    icon: '/svg/icon_database.svg',
    name: '数据源',
    color: '#F5A623',
    code: 'SOURCE',
    action: 'create-source'
  }
]

const portalEntryBaseList = [
  {
    icon: '/svg/icon_dataset_outlined.svg?v=20260606',
    name: '全部资源',
    color: '#3B82F6',
    action: 'all',
    disabled: false
  },
  {
    icon: '/svg/icon_data-visualization.svg',
    name: '数据大屏',
    color: '#1FB6A6',
    action: 'dataV',
    disabled: false
  },
  {
    icon: '/svg/icon_dashboard.svg',
    name: '仪表盘',
    color: '#3B82F6',
    action: 'dashboard',
    disabled: false
  },
  {
    icon: '/svg/icon_download_outlined.svg?v=20260606',
    name: '导出中心',
    color: '#F5A623',
    action: 'export',
    disabled: false
  }
]

const backendAccess = computed(() => userStore.getBackendAccess || state.overview.backendAccess)
const systemTitle = computed(() => appearanceStore.getSiteTitle)
const userName = computed(() => userStore.getName || '用户')
const userId = computed(() => userStore.getUid || '-')

const quickCreationList = computed(() =>
  quickCreationBaseList.map(item => ({
    ...item,
    disabled: !backendAccess.value
  }))
)
const actionCardTitle = computed(() => (backendAccess.value ? '快速创建' : '常用入口'))
const actionCardList = computed(() =>
  backendAccess.value ? quickCreationList.value : portalEntryBaseList
)

const typeFilters = computed(() => [
  { label: '全部', value: 'all', count: state.filterCounts.total, color: '#3B82F6' },
  { label: '数据大屏', value: 'dataV', count: state.filterCounts.screenCount, color: '#1FB6A6' },
  {
    label: '仪表盘',
    value: 'dashboard',
    count: state.filterCounts.dashboardCount,
    color: '#3B82F6'
  }
])

const stats = computed(() => [
  {
    name: '数据大屏',
    code: 'SCREEN',
    color: '#1FB6A6',
    value: state.overview.screenCount
  },
  {
    name: '仪表盘',
    code: 'DASHBOARD',
    color: '#3B82F6',
    value: state.overview.dashboardCount
  },
  {
    name: '可访问资源',
    code: 'RESOURCE',
    color: '#6E62E8',
    value: state.overview.total
  }
])

const typeLabel = (type: string) => (type === 'dashboard' ? '仪表盘' : '数据大屏')
const typeIcon = (type: string) =>
  type === 'dashboard' ? '/svg/icon_dashboard.svg' : '/svg/icon_data-visualization.svg'
const typeColor = (type: string) => (type === 'dashboard' ? '#3B82F6' : '#1FB6A6')
const typeCode = (type: string | number) => String(type || '').toUpperCase()
const formatTime = (time?: number | null, formatter = 'YYYY-MM-DD HH:mm') =>
  time ? dayjs(time).format(formatter) : '-'
const resourceQueryFrom = () => (activeName.value === ALL_RESOURCES ? undefined : activeName.value)
const emptyDescription = computed(() => {
  if (state.keyword) {
    return '暂无相关内容'
  }
  if (activeName.value === FAVORITES) {
    return '暂无收藏'
  }
  return activeName.value === ALL_RESOURCES ? '暂无资源' : '暂无最近使用'
})

const getSparkValues = (value: number) => {
  const total = Number(value || 0)
  if (!total) {
    return new Array(7).fill(0)
  }
  return [0, 0.16, 0.32, 0.48, 0.66, 0.84, 1].map(rate => Math.ceil(total * rate))
}

const getSparkPoints = (value: number) => {
  const values = getSparkValues(value)
  const max = Math.max(...values)
  const min = Math.min(...values)
  const range = max - min || 1
  return values
    .map((item, index) => {
      const x = (46 / 6) * index
      const y = max === min ? (max ? 9 : 15) : 16 - ((item - min) / range) * 14
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

const getSparkLastPoint = (value: number) => {
  const point = getSparkPoints(value).split(' ').pop() || '46,9'
  const [x, y] = point.split(',')
  return { x, y }
}

const loadOverview = async () => {
  const res = await portalOverviewApi()
  Object.assign(state.overview, res?.data || {})
}

const loadResources = async () => {
  const requestSeq = ++resourceRequestSeq
  state.loading = true
  try {
    const res = await portalResourcesApi({
      keyword: state.keyword,
      type: state.type,
      page: state.page,
      pageSize: state.pageSize,
      asc: !orderDesc.value,
      queryFrom: resourceQueryFrom()
    })
    if (requestSeq !== resourceRequestSeq) {
      return
    }
    state.records = res?.data?.records || []
    state.total = Number(res?.data?.total || 0)
    state.filterCounts.total =
      Number(res?.data?.screenCount || 0) + Number(res?.data?.dashboardCount || 0)
    state.filterCounts.screenCount = Number(res?.data?.screenCount || 0)
    state.filterCounts.dashboardCount = Number(res?.data?.dashboardCount || 0)
  } finally {
    if (requestSeq === resourceRequestSeq) {
      state.loading = false
    }
  }
}

const reload = async () => {
  await Promise.all([loadOverview(), loadResources()])
}

const search = () => {
  state.page = 1
  loadResources()
}

const handleTabClick = (pane: TabsPaneContext) => {
  if (pane.paneName === activeName.value) {
    state.page = 1
    loadResources()
  }
}

const selectType = (type: string) => {
  if (state.type === type) {
    return
  }
  state.type = type
  state.page = 1
  loadResources()
}

const openAllResources = (type: string) => {
  const shouldReload = activeName.value !== ALL_RESOURCES || state.type !== type
  activeName.value = ALL_RESOURCES
  state.type = type
  state.page = 1
  if (shouldReload) {
    loadResources()
  }
}

const sortChange = (param: { order?: string }) => {
  if (!param?.order) {
    orderDesc.value = true
  } else {
    orderDesc.value = param.order.startsWith('desc')
  }
  search()
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

const storeResourceType = (type: string) => (type === 'dashboard' ? 'panel' : 'screen')

const toggleFavorite = async (resource: PortalResource) => {
  if (favoriteLoadingId.value === resource.id) {
    return
  }
  favoriteLoadingId.value = resource.id
  try {
    await storeApi({
      id: resource.id,
      type: storeResourceType(resource.type)
    })
    await loadResources()
  } finally {
    favoriteLoadingId.value = null
  }
}

const backHome = () => {
  router.push('/portal')
}

const quickCreate = (index: number) => {
  if (!backendAccess.value) {
    return
  }
  const routes = [
    '#/dashboard?opt=create',
    '#/dvCanvas?opt=create',
    '#/dataset-form',
    '#/data/datasource?opt=create'
  ]
  window.open(routes[index], '_self')
}

const openExportCenter = () => {
  useEmitt().emitter.emit('data-export-center', { activeName: 'ALL' })
}

const openPortalEntry = (action: string, index: number) => {
  if (backendAccess.value) {
    quickCreate(index)
    return
  }
  if (action === 'export') {
    openExportCenter()
    return
  }
  openAllResources(action)
}

const enterBackend = () => {
  router.push('/workbranch/index')
}

onMounted(reload)
</script>

<template>
  <main class="portal-page">
    <header class="portal-header">
      <button class="brand" type="button" @click="backHome">
        <img class="brand-mark" :src="crestMark" alt="" aria-hidden="true" />
        <span class="portal-title">{{ systemTitle }}</span>
      </button>
      <div class="header-actions">
        <el-button v-if="backendAccess" plain @click="enterBackend">进入后台</el-button>
        <PortalAccount />
      </div>
    </header>

    <section class="workbranch portal-workbranch">
      <div class="info-quick-creation">
        <div class="user-info work-card">
          <div class="profile-row">
            <el-icon class="main-color user-icon-container">
              <Icon name="user-img"><userImg class="svg-icon" /></Icon>
            </el-icon>
            <div class="info">
              <div class="name-role flex-align-center">
                <span :title="userName" class="name ellipsis">{{ userName }}</span>
              </div>
              <span class="id">ID: {{ userId }}</span>
            </div>
          </div>
          <div v-for="item in stats" :key="item.code" class="stat-item">
            <span class="stat-bar" :style="{ backgroundColor: item.color }" />
            <span class="stat-meta">
              <span class="name">{{ item.name }}</span>
              <span class="code">{{ item.code }}</span>
            </span>
            <svg class="sparkline" viewBox="0 0 46 18" aria-hidden="true">
              <polyline
                :points="getSparkPoints(item.value)"
                fill="none"
                :stroke="item.color"
                stroke-width="1.6"
                stroke-linecap="round"
                stroke-linejoin="round"
                opacity="0.85"
              />
              <circle
                :cx="getSparkLastPoint(item.value).x"
                :cy="getSparkLastPoint(item.value).y"
                r="2"
                :fill="item.color"
              />
            </svg>
            <span class="num">{{ item.value }}</span>
          </div>
        </div>

        <div class="quick-creation work-card">
          <span class="label">{{ actionCardTitle }}</span>
          <div class="item-creation">
            <div
              v-for="(item, index) in actionCardList"
              :key="item.name"
              class="item"
              :class="{ 'quick-create-disabled': item.disabled }"
              :style="{ '--accent': item.color }"
              @click="openPortalEntry(item.action || '', index)"
            >
              <el-tooltip v-if="item.disabled" effect="dark" content="无创建权限" placement="top">
                <div class="empty-tooltip-container" />
              </el-tooltip>
              <span class="quick-icon" :style="{ backgroundColor: item.color }">
                <img :src="item.icon" :alt="item.name" />
              </span>
              <span class="name">{{ item.name }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="workbranch-content">
        <el-scrollbar class="workbranch-content-scroll">
          <div class="dashboard-type border-radius-12" v-loading="state.loading">
            <el-tabs v-model="activeName" class="dashboard-type-tabs" @tab-click="handleTabClick">
              <el-tab-pane
                v-for="item in paneList"
                :key="item.name"
                :label="item.title"
                :name="item.name"
              />
            </el-tabs>

            <div class="asset-toolbar">
              <div class="type-chip-list">
                <button
                  v-for="item in typeFilters"
                  :key="item.value"
                  type="button"
                  class="type-chip"
                  :class="{ active: state.type === item.value }"
                  @click="selectType(item.value)"
                >
                  <span
                    v-if="item.value !== 'all'"
                    class="type-dot"
                    :style="{ backgroundColor: item.color }"
                  />
                  <span>{{ item.label }}</span>
                  <em>{{ item.count }}</em>
                </button>
              </div>
              <div class="search">
                <el-input
                  v-model.trim="state.keyword"
                  clearable
                  placeholder="搜索名称"
                  @clear="search"
                  @change="search"
                  @keyup.enter="search"
                >
                  <template #prefix>
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2" />
                      <path
                        d="M16.5 16.5 21 21"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                      />
                    </svg>
                  </template>
                </el-input>
              </div>
            </div>

            <div class="panel-table">
              <GridTable
                :show-pagination="false"
                :table-data="state.records"
                :data-loading="state.loading"
                :empty-desc="emptyDescription"
                empty-img="noneWhite"
                class="workbranch-grid"
                @sort-change="sortChange"
                @cell-click="openResource"
              >
                <el-table-column key="name" min-width="420" prop="name" label="名称">
                  <template #default="{ row }">
                    <div class="name-content asset-name jump-active">
                      <span class="asset-icon" :style="{ backgroundColor: typeColor(row.type) }">
                        <img :src="typeIcon(row.type)" :alt="typeLabel(row.type)" />
                      </span>
                      <span class="asset-title-wrap">
                        <el-tooltip placement="top">
                          <template #content>{{ row.name }}</template>
                          <span class="asset-title ellipsis">{{ row.name }}</span>
                        </el-tooltip>
                        <span class="asset-code">{{ typeCode(row.id) }}</span>
                      </span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="类型" width="130">
                  <template #default="{ row }">
                    <span class="asset-tag" :style="{ '--tag-color': typeColor(row.type) }">
                      <span class="tag-dot" />
                      {{ typeLabel(row.type) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column
                  prop="updateTime"
                  label="更新时间"
                  min-width="180"
                  sortable="custom"
                >
                  <template #default="{ row }">
                    <span class="time-cell">
                      <span class="time-full">{{ formatTime(row.updateTime) }}</span>
                    </span>
                  </template>
                </el-table-column>
                <el-table-column
                  width="104"
                  fixed="right"
                  label="操作"
                  align="center"
                  header-align="center"
                  class-name="operation-column"
                  label-class-name="operation-column-header"
                >
                  <template #default="{ row }">
                    <div class="table-actions">
                      <el-tooltip
                        effect="dark"
                        :content="row.favorite ? '取消收藏' : '收藏'"
                        placement="top"
                      >
                        <button
                          type="button"
                          class="opbtn favorite-btn"
                          :class="{ 'is-favorite': row.favorite }"
                          :disabled="favoriteLoadingId === row.id"
                          @click.stop="toggleFavorite(row)"
                        >
                          <component
                            :is="row.favorite ? visualStar : iconCollectionOutlined"
                            class="favorite-svg"
                          />
                        </button>
                      </el-tooltip>
                      <el-tooltip effect="dark" content="查看" placement="top">
                        <button type="button" class="opbtn" @click.stop="openResource(row)">
                          <svg
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            aria-hidden="true"
                          >
                            <path
                              d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7z"
                              stroke="currentColor"
                              stroke-width="1.7"
                            />
                            <circle
                              cx="12"
                              cy="12"
                              r="3"
                              stroke="currentColor"
                              stroke-width="1.7"
                            />
                          </svg>
                        </button>
                      </el-tooltip>
                    </div>
                  </template>
                </el-table-column>
              </GridTable>
              <div class="table-footer">
                <span>共 {{ state.total }} 项</span>
                <el-pagination
                  v-if="state.total > state.pageSize"
                  layout="prev, pager, next"
                  :current-page="state.page"
                  :page-size="state.pageSize"
                  :total="state.total"
                  @current-change="pageChange"
                />
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>
    </section>
  </main>
</template>

<style scoped lang="less">
.portal-page {
  min-height: 100vh;
  overflow: hidden;
  color: #0f172a;
  background: #f8fafc;
  font-family: var(--crest-font-sans), 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.portal-header {
  position: relative;
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 clamp(28px, 5vw, 72px);
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border-bottom: 1px solid #e2e8f0;

  &::before {
    position: absolute;
    inset: 0;
    pointer-events: none;
    content: '';
    background-image: radial-gradient(circle at 1px 1px, #3b6fd0 1px, transparent 0);
    background-size: 20px 20px;
    opacity: 0.035;
  }

  > * {
    position: relative;
    z-index: 1;
  }
}

.brand {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  justify-self: start;
  height: 40px;
  min-width: 0;
  width: max-content;
  padding: 0;
  cursor: pointer;
  background: transparent;
  border: 0;
  top: 0.5px;
}

.brand-mark {
  flex: none;
  display: block;
  width: 34px;
  height: 32px;
  object-fit: contain;
}

.portal-title {
  display: block;
  flex: none;
  color: #0f172a;
  font-family: var(--crest-font-sans), 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 20px;
  font-weight: 700;
  line-height: 32px;
  letter-spacing: 0;
  white-space: nowrap;
}

.header-actions {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;

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

.workbranch {
  display: grid;
  grid-template-columns: clamp(320px, 18vw, 360px) minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
  width: 100%;
  height: calc(100vh - 64px);
  min-height: 640px;
  padding: clamp(22px, 2vw, 32px) clamp(28px, 4vw, 72px);
  overflow: hidden;
  font-family: var(--crest-font-sans);
  background: #f8fafc;
  --workbranch-card-radius: 14px;

  .work-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: var(--workbranch-card-radius);
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  }

  .info-quick-creation {
    display: flex;
    flex-direction: column;
    gap: 16px;
    width: 100%;
    height: 100%;
    min-height: 0;

    .main-color {
      color: #3b82f6;
      background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
    }

    .user-info {
      display: flex;
      flex-direction: column;
      gap: 11px;
      padding: 22px 24px 18px;

      .profile-row {
        display: flex;
        align-items: center;
        gap: 14px;
        margin-bottom: 7px;
      }

      .user-icon-container {
        position: relative;
        flex: none;
        width: 52px !important;
        height: 52px !important;

        &::after {
          position: absolute;
          right: -2px;
          bottom: -2px;
          width: 14px;
          height: 14px;
          content: '';
          background: #22c55e;
          border: 2.5px solid #ffffff;
          border-radius: 50%;
        }
      }

      .ed-icon {
        padding: 0;
        font-size: 22px;
        border-radius: 50%;
      }

      .info {
        display: flex;
        flex: 1;
        flex-wrap: wrap;
        align-items: flex-start;
        min-width: 0;

        .name-role {
          width: 100%;
          margin-bottom: 3px;
          color: #0f172a;
          font-style: normal;

          .name {
            max-width: 210px;
            font-size: 17px;
            font-weight: 600;
            line-height: 24px;
          }
        }

        .id {
          width: 200px;
          color: #64748b;
          font-family: var(--crest-font-mono);
          font-size: 12px;
          font-weight: 400;
          line-height: 18px;
        }
      }

      .stat-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 6px 0;
        font-style: normal;

        &:first-of-type {
          padding-top: 16px;
          border-top: 1px solid #f1f5f9;
        }

        .stat-bar {
          flex: none;
          width: 7px;
          height: 24px;
          border-radius: 2px;
        }

        .stat-meta {
          display: flex;
          flex: 1;
          flex-direction: column;
          min-width: 0;
        }

        .name {
          color: #64748b;
          font-size: 12.5px;
          font-weight: 400;
          line-height: 18px;
        }

        .code {
          color: #94a3b8;
          font-family: var(--crest-font-mono);
          font-size: 10.5px;
          line-height: 16px;
        }

        .sparkline {
          flex: none;
          width: 46px;
          height: 18px;
        }

        .num {
          min-width: 34px;
          color: #0f172a;
          font-size: 22px;
          font-weight: 700;
          line-height: 22px;
          text-align: right;
          font-variant-numeric: tabular-nums;
        }
      }
    }

    .quick-creation {
      display: flex;
      flex: 1;
      flex-direction: column;
      min-height: 0;
      padding: 18px 22px 22px;

      .label {
        color: #334155;
        font-size: 13.5px;
        font-weight: 600;
        line-height: 20px;
      }

      .item-creation {
        display: grid;
        flex: 1;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
        align-content: start;
        margin-top: 14px;

        .item {
          position: relative;
          display: flex;
          align-items: center;
          gap: 11px;
          min-height: 64px;
          padding: 12px 13px;
          overflow: hidden;
          cursor: pointer;
          background: #ffffff;
          border: 1px solid #e2e8f0;
          border-radius: 10px;
          transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;

          &::after {
            position: absolute;
            inset: 0;
            pointer-events: none;
            content: '';
            background: linear-gradient(135deg, var(--accent, #3b82f6) 0%, transparent 38%);
            opacity: 0;
            transition: opacity 0.2s ease;
          }

          &:hover {
            border-color: transparent;
            box-shadow: 0 4px 14px -4px rgba(15, 23, 42, 0.1), 0 0 0 1.5px var(--accent, #3b82f6);
            transform: translateY(-1px);
          }

          &:hover::after {
            opacity: 0.08;
          }

          .quick-icon {
            position: relative;
            z-index: 1;
            display: flex;
            flex: none;
            align-items: center;
            justify-content: center;
            width: 36px;
            height: 36px;
            border-radius: 8px;

            img {
              width: 22px;
              height: 22px;
              object-fit: contain;
            }
          }

          .name {
            position: relative;
            z-index: 1;
            min-width: 0;
            color: #0f172a;
            font-size: 13.5px;
            font-weight: 600;
            line-height: 20px;
          }
        }

        .quick-create-disabled {
          color: var(--ed-color-info-light-5);
          cursor: not-allowed;
          background-color: var(--ed-color-info-light-9);
          border-color: var(--ed-color-info-light-8);

          .name {
            color: var(--ed-color-info-light-5) !important;
          }

          .quick-icon {
            background-color: var(--ed-color-primary-light-8) !important;
            border-color: var(--ed-color-info-light-8) !important;
          }

          .empty-tooltip-container {
            position: absolute;
            width: 146px;
            height: 52px;
            margin-left: -16px;
          }
        }
      }
    }
  }

  .workbranch-content {
    min-width: 0;
    height: 100%;
    min-height: 0;
    overflow: hidden;
    border-radius: var(--workbranch-card-radius);

    .workbranch-content-scroll {
      height: 100%;
      border-radius: inherit;

      :deep(.ed-scrollbar__wrap),
      :deep(.ed-scrollbar__view) {
        height: 100%;
        border-radius: inherit;
      }
    }
  }
}

.dashboard-type {
  height: 100%;
  min-height: 0;
  padding: 18px 24px 0;
  overflow: hidden;
  font-family: var(--crest-font-sans);
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

  :deep(.ed-loading-mask) {
    border-radius: inherit;
  }

  .dashboard-type-tabs {
    position: relative;
    margin-bottom: 0;

    &::after {
      position: absolute;
      right: 0;
      bottom: 0;
      left: 0;
      height: 1px;
      content: '';
      background: #f1f5f9;
    }

    :deep(.ed-tabs__header) {
      margin: 0;
    }

    :deep(.ed-tabs__nav-wrap::after) {
      display: none;
    }

    :deep(.ed-tabs__active-bar) {
      height: 2px;
      background: #3b82f6;
      border-radius: 2px 2px 0 0;
    }

    :deep(.ed-tabs__item) {
      height: 34px;
      padding: 0 26px 14px 0;
      color: #64748b;
      font-size: 14.5px;
      font-weight: 500;
    }

    :deep(.ed-tabs__item.is-active) {
      color: #3b82f6;
      font-weight: 600;
    }
  }

  .asset-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    padding: 14px 0;
  }

  .type-chip-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    min-width: 0;
  }

  .type-chip {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    height: 30px;
    padding: 0 11px;
    color: #334155;
    font-size: 12.5px;
    font-weight: 500;
    cursor: pointer;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 7px;
    transition: color 0.14s ease, background 0.14s ease, border-color 0.14s ease;

    em {
      color: #94a3b8;
      font-style: normal;
      font-weight: 600;
    }

    &:hover {
      border-color: #cbd5e1;
    }

    &.active {
      color: #ffffff;
      background: #3b82f6;
      border-color: #3b82f6;

      em {
        color: rgba(255, 255, 255, 0.78);
      }
    }
  }

  .type-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  .search {
    flex: none;
    text-align: right;

    .ed-input {
      width: 240px;
    }

    :deep(.ed-input__wrapper) {
      height: 34px;
      padding: 0 12px;
      border-radius: 8px;
      box-shadow: 0 0 0 1px #e2e8f0 inset;
      transition: box-shadow 0.14s ease, background 0.14s ease;
    }

    :deep(.ed-input__wrapper.is-focus) {
      box-shadow: 0 0 0 1px #3b82f6 inset, 0 0 0 3px rgba(59, 130, 246, 0.1);
    }

    :deep(.ed-input__prefix) {
      color: #64748b;
    }
  }

  .panel-table {
    display: flex;
    flex-direction: column;
    height: calc(100% - 90px);
    min-height: 0;

    :deep(.ed-table) {
      --ed-table-header-bg-color: #ffffff;
      --ed-table-tr-bg-color: #ffffff;
      color: #334155;
      font-family: var(--crest-font-sans);
      font-size: 13.5px;
    }

    :deep(.ed-table__inner-wrapper::before) {
      display: none;
    }

    :deep(.ed-table th.ed-table__cell) {
      padding: 10px 0;
      color: #94a3b8;
      font-family: var(--crest-font-mono);
      font-size: 11.5px;
      font-weight: 500;
      letter-spacing: 0;
      background: #ffffff;
      border-bottom: 1px solid #f1f5f9;
    }

    :deep(.ed-table td.ed-table__cell) {
      padding: 12px 0;
      border-bottom: 1px solid #f1f5f9;
    }

    :deep(.ed-table__row) {
      cursor: pointer;
      transition: background 0.12s ease;
    }

    :deep(.ed-table__row:hover > td.ed-table__cell) {
      background: #fafbfc;
    }

    .workbranch-grid {
      flex: 1;
      min-height: 0;
    }

    .name-content {
      display: flex;
      align-items: center;
    }
  }
}

.asset-name {
  min-width: 0;
}

.asset-icon {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-right: 12px;
  border-radius: 8px;

  img {
    width: 18px;
    height: 18px;
    object-fit: contain;
  }
}

.asset-title-wrap {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.asset-title {
  max-width: clamp(240px, 34vw, 560px);
  color: #0f172a;
  font-size: 13.5px;
  font-weight: 600;
  line-height: 18px;
}

.asset-code {
  margin-top: 2px;
  color: #94a3b8;
  font-family: var(--crest-font-mono);
  font-size: 11px;
  line-height: 15px;
}

.asset-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  color: var(--tag-color);
  font-size: 11.5px;
  font-weight: 600;
  background: color-mix(in srgb, var(--tag-color) 12%, white);
  border-radius: 5px;
}

.tag-dot {
  width: 5px;
  height: 5px;
  background: var(--tag-color);
  border-radius: 50%;
}

.time-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.time-full {
  margin-top: 1px;
  color: #94a3b8;
  font-family: var(--crest-font-mono);
  font-size: 11px;
}

.table-actions {
  display: flex;
  width: 100%;
  justify-content: center;
  gap: 4px;
  opacity: 1;
}

.workbranch-grid :deep(.operation-column .cell),
.workbranch-grid :deep(.operation-column-header .cell),
.workbranch-grid :deep(.ed-table-fixed-column--right .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-right: 0;
  padding-left: 0;
}

.opbtn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 6px;
  transition: color 0.12s ease, background 0.12s ease;

  &:hover {
    color: #3b82f6;
    background: #f1f5f9;
  }

  &:disabled {
    cursor: wait;
    opacity: 0.55;
  }
}

.favorite-btn {
  &.is-favorite,
  &:hover.is-favorite {
    color: #f59e0b;
  }
}

.favorite-svg {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0 14px;
  color: #64748b;
  font-size: 12.5px;
  border-top: 1px solid #f1f5f9;
}

.workbranch-grid :deep(.ed-empty) {
  padding: 80px 0 !important;

  .ed-empty__description {
    margin-top: 0;
    line-height: 20px !important;
  }
}

.jump-active {
  cursor: pointer;
}

@media (max-width: 1180px) {
  .workbranch {
    grid-template-columns: 300px minmax(0, 1fr);
    padding: 20px;
  }
}

@media (max-width: 900px) {
  .portal-page {
    overflow: auto;
  }

  .workbranch {
    grid-template-columns: 1fr;
    height: auto;
    min-height: calc(100vh - 64px);
    overflow: auto;
  }

  .workbranch .workbranch-content {
    height: 620px;
    min-height: 520px;
  }
}

@media (max-width: 680px) {
  .portal-header {
    padding: 0 16px;
  }

  .portal-title {
    max-width: calc(100vw - 120px);
    overflow: hidden;
    font-size: 17px;
    text-overflow: ellipsis;
  }

  .header-actions :deep(.account-name) {
    display: none;
  }

  .dashboard-type {
    padding: 16px 16px 0;

    .asset-toolbar {
      align-items: stretch;
      flex-direction: column;
    }

    .search .ed-input {
      width: 100%;
    }
  }
}
</style>
