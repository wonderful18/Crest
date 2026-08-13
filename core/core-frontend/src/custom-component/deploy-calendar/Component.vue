<template>
  <div class="deploy-calendar-main" :class="{ 'is-light': themes === 'light' }">
    <div class="cal-header">
      <span class="cal-title">{{ currentLabel }}</span>
      <div class="cal-nav">
        <el-icon class="cal-nav-btn" @click="changeMonth(-1)"><ArrowLeft /></el-icon>
        <el-icon class="cal-nav-btn" @click="goToday" :title="t('deploy_calendar.go_today')">
          <Calendar />
        </el-icon>
        <el-icon class="cal-nav-btn" @click="changeMonth(1)"><ArrowRight /></el-icon>
      </div>
    </div>
    <div class="cal-weekdays">
      <div v-for="w in weekdays" :key="w" class="cal-weekday">{{ w }}</div>
    </div>
    <div class="cal-grid">
      <div
        v-for="(cell, idx) in calendarCells"
        :key="idx"
        class="cal-cell"
        :class="{
          'is-other-month': !cell.inMonth,
          'is-today': cell.isToday,
          'has-deploy': cell.deployments.length > 0
        }"
      >
        <el-popover
          :width="280"
          placement="top"
          trigger="hover"
          :show-after="50"
          :hide-after="100"
          :disabled="cell.deployments.length === 0"
          popper-class="deploy-calendar-popover"
        >
          <template #reference>
            <div
              class="cell-inner"
              :class="{ 'is-selected': selectedKey === formatDate(cell.date) }"
              @click.stop="onDateClick(cell)"
            >
              <span class="cell-date">{{ cell.day }}</span>
              <div class="cell-dots">
                <span v-if="cell.hasWeekly" class="dot dot-weekly" :title="t('deploy_calendar.weekly')"></span>
                <span v-if="cell.hasMonthly" class="dot dot-monthly" :title="t('deploy_calendar.monthly')"></span>
              </div>
            </div>
          </template>
          <div class="pop-list">
            <div class="pop-title">{{ formatDate(cell.date) }}</div>
            <div v-for="(d, i) in cell.deployments" :key="i" class="pop-item">
              <span class="pop-type" :class="d.typeClass">{{ d.typeLabel }}</span>
              <span class="pop-content">{{ d.content }}</span>
            </div>
          </div>
        </el-popover>
      </div>
    </div>
    <div class="cal-legend">
      <span class="legend-item"><i class="dot dot-weekly"></i>{{ t('deploy_calendar.weekly') }}</span>
      <span class="legend-item"><i class="dot dot-monthly"></i>{{ t('deploy_calendar.monthly') }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeMount, reactive, ref, toRefs, watch, PropType } from 'vue'
import { Calendar, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { useI18n } from '@/hooks/web/useI18n'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { getData } from '@/api/chart'
import { storeToRefs } from 'pinia'
import ChartEmptyInfo from '@/views/chart/components/views/components/ChartEmptyInfo.vue'
import ChartError from '@/views/chart/components/views/components/ChartError.vue'

const { t } = useI18n()
const dvMainStore = dvMainStoreWithOut()
const { canvasViewInfo, mobileInPc, fullscreenFlag } = storeToRefs(dvMainStore)

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const props = defineProps({
  element: {
    type: Object,
    default() {
      return { propValue: null }
    }
  },
  themes: {
    type: String,
    default: 'dark'
  },
  showPosition: {
    required: false,
    type: String,
    default: 'preview'
  },
  searchCount: {
    type: Number,
    required: false,
    default: 0
  },
  view: {
    type: Object as PropType<ChartObj>,
    default() {
      return { propValue: null }
    }
  },
  suffixId: {
    type: String,
    required: false
  }
})

const { view, showPosition } = toRefs(props)
const isError = ref(false)
const errMsg = ref('')

const emit = defineEmits(['onPointClick'])

// 当前选中日期（用于高亮显示）
const selectedKey = ref<string | null>(null)

// 点日期格子：触发仪表板联动（日历→明细表）
// 参考 ChartComponentG2Plot 的联动逻辑：直接调 addViewTrackFilter
// 再次点击当前已选中日期：清除联动，恢复默认全部明细表
const onDateClick = (cell: any) => {
  const xField = view.value?.xAxis?.[0]
  if (!xField) return
  const key = formatDate(cell.date)
  const isDeselecting = selectedKey.value === key
  // 更新选中高亮：再次点击同一天 → 取消选中
  selectedKey.value = isDeselecting ? null : key
  if (isDeselecting) {
    // 清除本日历作为来源的所有联动条件，目标明细表重新查询恢复全部数据
    dvMainStore.clearViewLinkage(view.value.id)
    console.log('[DeployCalendar] 再次点击取消联动，恢复默认明细表', { date: key })
    return
  }
  // 联动传值：用数据集原始值（如 2024/05/27），而非 formatDate 的 YYYY-MM-DD
  // 这样目标明细表 eq 比较时能匹配到原始格式
  const rawDate = cell.deployments.length > 0 ? cell.deployments[0].rawDate : formatDate(cell.date)
  const dimensionList = [
    {
      id: xField.id,
      value: rawDate,
      name: xField.name || xField.originName
    }
  ]
  // option:'linkage' 触发目标图表筛选；同时 emit onPointClick 供嵌入/事件监听
  const linkageParam = {
    option: 'linkage' as const,
    name: xField.id,
    viewId: view.value.id,
    dimensionList,
    quotaList: []
  }
  dvMainStore.addViewTrackFilter(linkageParam)
  console.log('[DeployCalendar] 点击日期触发联动', { formatDate值: formatDate(cell.date), 传给明细表的值: rawDate, cellDeployments: cell.deployments })
  const clickParams = {
    option: 'pointClick',
    name: xField.id,
    viewId: view.value.id,
    dimensionList,
    quotaList: []
  }
  emit('onPointClick', clickParams)
}

// 当前展示的月份（year, month 0-11）
const cursor = reactive({ year: 0, month: 0 })

const state = reactive<any>({
  // 按日期字符串 'YYYY-MM-DD' 分组的投产记录
  // 每条: { content, type: 'weekly'|'monthly'|'both', raw }
  deployMap: new Map<string, any[]>(),
  // 字段名映射: { dateField, contentField, typeField }
  fieldMap: { dateField: null, contentField: null, typeField: null },
  useMock: true
})

const isEditMode = computed(
  () => showPosition.value.includes('canvas') && !mobileInPc.value && !fullscreenFlag.value
)

const today = new Date()
const initCursor = () => {
  cursor.year = today.getFullYear()
  cursor.month = today.getMonth()
}
initCursor()

const currentLabel = computed(() => `${cursor.year}年${cursor.month + 1}月`)

const changeMonth = (delta: number) => {
  let m = cursor.month + delta
  let y = cursor.year
  if (m < 0) {
    m = 11
    y--
  } else if (m > 11) {
    m = 0
    y++
  }
  cursor.year = y
  cursor.month = m
}

const goToday = () => initCursor()

const pad = (n: number) => String(n).padStart(2, '0')

const formatDate = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

// 判断版本类型：值含"周"为周版，含"月"为月版
const classifyType = (raw: any): 'weekly' | 'monthly' | 'both' | 'unknown' => {
  if (raw === null || raw === undefined) return 'unknown'
  const s = String(raw)
  const hasW = /周/.test(s)
  const hasM = /月/.test(s)
  if (hasW && hasM) return 'both'
  if (hasW) return 'weekly'
  if (hasM) return 'monthly'
  return 'unknown'
}

const typeLabelOf = (tp: string) => {
  if (tp === 'weekly') return t('deploy_calendar.weekly')
  if (tp === 'monthly') return t('deploy_calendar.monthly')
  return t('deploy_calendar.unknown')
}

const typeClassOf = (tp: string) => {
  if (tp === 'weekly') return 'pop-type-weekly'
  if (tp === 'monthly') return 'pop-type-monthly'
  return 'pop-type-unknown'
}

// 生成当前月日历单元格（含前补白）
const calendarCells = computed(() => {
  const y = cursor.year
  const m = cursor.month
  const first = new Date(y, m, 1)
  const firstDayWeek = first.getDay() // 0=日
  const daysInMonth = new Date(y, m + 1, 0).getDate()
  const cells: any[] = []
  // 前面补上月末尾
  const prevMonthDays = new Date(y, m, 0).getDate()
  for (let i = firstDayWeek - 1; i >= 0; i--) {
    const d = new Date(y, m - 1, prevMonthDays - i)
    cells.push(makeCell(d, false))
  }
  for (let day = 1; day <= daysInMonth; day++) {
    const d = new Date(y, m, day)
    cells.push(makeCell(d, true))
  }
  // 补齐到 6 行（42 格）
  let nextDay = 1
  while (cells.length < 42) {
    const d = new Date(y, m + 1, nextDay++)
    cells.push(makeCell(d, false))
  }
  return cells
})

const makeCell = (d: Date, inMonth: boolean) => {
  const key = formatDate(d)
  const records = state.deployMap.get(key) || []
  const hasWeekly = records.some(r => r.type === 'weekly' || r.type === 'both')
  const hasMonthly = records.some(r => r.type === 'monthly' || r.type === 'both')
  const todayStr = formatDate(today)
  return {
    date: d,
    day: d.getDate(),
    inMonth,
    isToday: key === todayStr,
    hasWeekly,
    hasMonthly,
    deployments: records.map(r => ({
      content: r.content,
      typeLabel: typeLabelOf(r.type),
      typeClass: typeClassOf(r.type),
      rawDate: r.rawDate
    }))
  }
}

// ===== mock 数据：近两月，每周五周版、每月1号月版 =====
const buildMock = () => {
  const map = new Map<string, any[]>()
  const base = new Date(today.getFullYear(), today.getMonth(), 1)
  for (let off = -1; off <= 1; off++) {
    const y = base.getFullYear() + (off === 0 ? 0 : 0)
    const m = base.getMonth() + off
    const monthDate = new Date(y, m, 1)
    const days = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0).getDate()
    for (let day = 1; day <= days; day++) {
      const d = new Date(monthDate.getFullYear(), monthDate.getMonth(), day)
      const key = formatDate(d)
      const records: any[] = []
      if (d.getDay() === 5) {
        // 周五 = 周版投产
        records.push({ content: `${d.getFullYear()}年第${getWeekOfYear(d)}周版本投产`, type: 'weekly', raw: '周版' })
      }
      if (d.getDate() === 1) {
        records.push({ content: `${d.getFullYear()}年${d.getMonth() + 1}月版本投产`, type: 'monthly', raw: '月版' })
      }
      if (records.length) map.set(key, records)
    }
  }
  return map
}

const getWeekOfYear = (d: Date) => {
  const onejan = new Date(d.getFullYear(), 0, 1)
  return Math.ceil(((d.getTime() - onejan.getTime()) / 86400000 + onejan.getDay() + 1) / 7)
}

// ===== 从 dataset 结果解析 =====
// 在 row 的 key 里按列名（中文名）找匹配的 key
const findKeyByName = (row: any, name: string): string | null => {
  if (!row || typeof row !== 'object') return null
  const keys = Object.keys(row)
  // 精确匹配
  if (name in row) return name
  // 模糊包含
  const fuzzy = keys.find(k => typeof k === 'string' && k.includes(name))
  return fuzzy || null
}

const parseData = (res: any) => {
  const map = new Map<string, any[]>()
  const data = res?.data
  if (!data) return map
  const fields = Array.isArray(data.fields) ? data.fields : []
  const rows = data.tableRow || []
  // 找到 view 里 xAxis/yAxis/extStack 绑定的字段 id
  const xFieldId = view.value?.xAxis?.[0]?.id
  const yFieldId = view.value?.yAxis?.[0]?.id
  const extFieldId = view.value?.extStack?.[0]?.id
  // engineFieldName → 用户字段名
  const engToName = fields.reduce((pre: any, f: any) => {
    pre[f.engineFieldName] = f.name
    return pre
  }, {})
  // 字段 id → engineFieldName
  const idToEng = fields.reduce((pre: any, f: any) => {
    if (f.id) pre[f.id] = f.engineFieldName || f.name
    return pre
  }, {})
  const dateEng = xFieldId ? idToEng[xFieldId] : null
  const contentEng = yFieldId ? idToEng[yFieldId] : null
  const typeEng = extFieldId ? idToEng[extFieldId] : null
  state.fieldMap = { dateField: dateEng, contentField: contentEng, typeField: typeEng }
  const probeRow = rows[0] || {}
  console.log('[DeployCalendar] 1. 绑定字段id:', 'x=', xFieldId, 'y=', yFieldId, 'ext=', extFieldId)
  console.log('[DeployCalendar] 2. 解析出engineName:', 'date=', dateEng, 'content=', contentEng, 'type=', typeEng)
  console.log('[DeployCalendar] 3. row的keys:', JSON.stringify(Object.keys(probeRow)))
  console.log('[DeployCalendar] 4. 第一行数据:', JSON.stringify(probeRow))
  console.log('[DeployCalendar] 5. 返回的fields:', JSON.stringify(fields.map((f: any) => ({ id: f.id, eng: f.engineFieldName, name: f.name }))))
  if (!dateEng) return map
  // 兜底：若 tableRow 的 key 与 engineFieldName 不一致，尝试按 originName/name 匹配
  const useOriginKey = !(dateEng in probeRow) && Object.keys(probeRow).length > 0
  const dateKey = useOriginKey ? (findKeyByName(probeRow, '投产日期') || dateEng) : dateEng
  const contentKey = useOriginKey ? (findKeyByName(probeRow, '投产内容') || contentEng) : contentEng
  const typeKey = useOriginKey ? (findKeyByName(probeRow, '版本类型') || typeEng) : typeEng
  if (useOriginKey) {
    console.info('[DeployCalendar] key 不匹配 engineFieldName，改用按列名匹配', { dateKey, contentKey, typeKey, dateEngInRow: dateEng in probeRow })
  }
  let withDate = 0
  rows.forEach((row: any) => {
    const dateRaw = row[dateKey]
    if (!dateRaw) return
    withDate++
    const d = parseDate(dateRaw)
    if (!d) return
    const key = formatDate(d)
    const content = contentKey ? row[contentKey] : ''
    const rawType = typeKey ? row[typeKey] : ''
    const tp = classifyType(rawType)
    const list = map.get(key) || []
    list.push({ content: content || '', type: tp, raw: rawType, rawDate: dateRaw })
    map.set(key, list)
  })
  console.log('[DeployCalendar] 9. 行数统计:', '总行=', rows.length, '有日期=', withDate, '解析成功=', map.size)
  return map
}

const parseDate = (raw: any): Date | null => {
  if (raw instanceof Date) return raw
  if (typeof raw !== 'string' && typeof raw !== 'number') return null
  const s = String(raw).trim()
  // 支持 YYYY-MM-DD / YYYY/MM/DD / YYYYMMDD / YYYYMM
  let m = s.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/)
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  m = s.match(/^(\d{4})(\d{2})(\d{2})$/)
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  m = s.match(/^(\d{4})[-/]?(0?[1-9]|1[0-2])$/)
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, 1)
  const dt = new Date(s)
  return isNaN(dt.getTime()) ? null : dt
}

const calcData = (viewCalc: Chart, callback?: () => void) => {
  isError.value = false
  if (!viewCalc.tableId || viewCalc['dataFrom'] === 'template' || !viewCalc?.xAxis?.length) {
    // 未绑数据集 / 未绑日期列 → mock 兜底
    console.warn('[DeployCalendar] 走 mock：未绑数据集或日期列', { tableId: viewCalc.tableId, xAxis: viewCalc?.xAxis })
    state.useMock = true
    state.deployMap = buildMock()
    callback?.()
    return
  }
  const v = JSON.parse(JSON.stringify(viewCalc))
  // 复用 table-info 查询通路拿 tableRow + fields
  // 把 yAxis/extStack 的字段也并入 xAxis，确保日期+内容+类型三列都返回。
  const xArr = Array.isArray(v.xAxis) ? v.xAxis : []
  const yArr = Array.isArray(v.yAxis) ? v.yAxis : []
  const extArr = Array.isArray(v.extStack) ? v.extStack : []
  v.xAxis = [...xArr, ...yArr, ...extArr]
  v.type = 'table-info'
  v.render = 'antv'
  getData(v)
  v.type = 'table-info'
  v.render = 'antv'
  getData(v)
    .then(res => {
      if (res?.code && res.code !== 0) {
        console.warn('[DeployCalendar] getData 报错，走 mock', res?.code, res?.msg)
        isError.value = true
        errMsg.value = res.msg
        state.useMock = true
        state.deployMap = buildMock()
      } else {
        const map = parseData(res)
        console.info('[DeployCalendar] parseData 结果', { useMock: map.size === 0, days: map.size, sample: [...map.entries()].slice(0, 3) })
        if (map.size === 0) {
          state.useMock = true
          state.deployMap = buildMock()
        } else {
          state.useMock = false
          state.deployMap = map
        }
        const curViewInfo = canvasViewInfo.value[props.element.id]
        if (res?.data && curViewInfo) {
          curViewInfo['curFields'] = res.data.fields || []
        }
        dvMainStore.setViewDataDetails(props.element.id, res)
      }
      callback?.()
      nextTick(() => {})
    })
    .catch(e => {
      console.warn('[DeployCalendar] getData 异常，走 mock', e)
      state.useMock = true
      state.deployMap = buildMock()
      callback?.()
    })
}

watch([() => props.searchCount], () => {
  calcData(view.value, () => {})
})

const renderChart = () => {}

onBeforeMount(() => {
  calcData(view.value)
})

defineExpose({ calcData, renderChart })
</script>

<style lang="less" scoped>
.deploy-calendar-main {
  overflow: hidden;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
  color: #e6e8eb;
  font-size: 12px;
  user-select: none;
}
.cal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  .cal-title {
    font-size: 14px;
    font-weight: 600;
  }
  .cal-nav {
    display: flex;
    align-items: center;
    gap: 6px;
    .cal-nav-btn {
      cursor: pointer;
      color: #a6aeb8;
      font-size: 16px;
      &:hover {
        color: #fff;
      }
    }
  }
}
.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  color: #8a9099;
  margin-bottom: 4px;
  .cal-weekday {
    padding: 2px 0;
  }
}
.cal-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-auto-rows: 1fr;
  gap: 2px;
  min-height: 0;
}
.cal-cell {
  min-height: 0;
  border-radius: 4px;
  position: relative;
  background: #1d2026;
  &:hover {
    background: #262a31;
  }
  &.is-other-month {
    opacity: 0.35;
  }
  &.is-today {
    outline: 1px dashed #8a9099;
  }
  &.has-deploy {
    background: #2a2024;
  }
}
.cell-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
  padding: 3px 0;
  cursor: default;
  border-radius: 4px;
  border: 1px solid transparent;
  transition: border-color 0.15s, background 0.15s;
  .cell-date {
    line-height: 18px;
    font-size: 12px;
  }
  .cell-dots {
    display: flex;
    gap: 3px;
    height: 6px;
  }
  &.is-selected {
    border-color: #409eff;
    background: rgba(64, 158, 255, 0.18);
  }
}
.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.dot-weekly {
  background: #409eff;
}
.dot-monthly {
  background: #f56c6c;
}
.cal-legend {
  display: flex;
  gap: 14px;
  justify-content: center;
  margin-top: 6px;
  color: #8a9099;
  .legend-item {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}
/* light theme */
.deploy-calendar-main.is-light {
  color: #303133;
  .cal-nav .cal-nav-btn {
    color: #606266;
    &:hover {
      color: #303133;
    }
  }
  .cal-cell {
    background: #f5f7fa;
    &:hover {
      background: #ecf0f3;
    }
    &.has-deploy {
      background: #fdf0f0;
    }
    &.is-today {
      outline: 1px dashed #909399;
    }
    .cell-inner.is-selected {
      border-color: #409eff;
      background: rgba(64, 158, 255, 0.15);
    }
  }
  .cal-weekdays,
  .cal-legend {
    color: #909399;
  }
}
</style>

<style lang="less">
.deploy-calendar-popover .pop-list {
  max-height: 260px;
  overflow-y: auto;
  .pop-title {
    font-weight: 600;
    margin-bottom: 6px;
    font-size: 13px;
  }
  .pop-item {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    padding: 4px 0;
    border-bottom: 1px dashed #eee;
    &:last-child {
      border-bottom: none;
    }
    .pop-type {
      flex-shrink: 0;
      padding: 1px 5px;
      border-radius: 3px;
      font-size: 11px;
      color: #fff;
    }
    .pop-type-weekly {
      background: #409eff;
    }
    .pop-type-monthly {
      background: #f56c6c;
    }
    .pop-type-unknown {
      background: #909399;
    }
    .pop-content {
      flex: 1;
      word-break: break-all;
    }
  }
}
</style>
