<template>
  <div
    class="deploy-calendar-main"
    :class="{ 'is-light': themes === 'light' }"
    :style="{ '--dc-cell-hover': state.style.cellHoverColor }"
  >
    <div class="cal-header">
      <span
        class="cal-title"
        :style="{
          color: state.style.titleColor,
          'font-size': state.style.titleFontSize + 'px',
          'font-weight': state.style.titleIsBolder ? 600 : 400
        }"
        >{{ currentLabel }}</span
      >
      <div class="cal-nav">
        <!-- 导航按钮颜色跟随标题色：避免深色画布下按钮继承深色文字而看不见 -->
        <el-icon
          class="cal-nav-btn"
          :style="{ color: state.style.titleColor }"
          @click="changeMonth(-1)"
          ><ArrowLeft
        /></el-icon>
        <el-icon
          class="cal-nav-btn"
          :style="{ color: state.style.titleColor }"
          @click="goToday"
          :title="t('deploy_calendar.go_today')"
        >
          <Calendar />
        </el-icon>
        <el-icon
          class="cal-nav-btn"
          :style="{ color: state.style.titleColor }"
          @click="changeMonth(1)"
          ><ArrowRight
        /></el-icon>
      </div>
    </div>
    <div class="cal-weekdays">
      <div
        v-for="w in weekdays"
        :key="w"
        class="cal-weekday"
        :style="{ color: state.style.weekdayColor }"
      >
        {{ w }}
      </div>
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
        :style="{
          background: cell.deployments.length > 0
            ? state.style.deployCellBgColor
            : state.style.cellBgColor,
          outline: cell.isToday ? `1px dashed ${state.style.todayBorderColor}` : 'none'
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
              :style="{
                'border-color':
                  selectedKey === formatDate(cell.date) ? state.style.selectedBorderColor : 'transparent',
                background:
                  selectedKey === formatDate(cell.date) ? state.style.selectedBgColor : 'transparent'
              }"
              @click.stop="onDateClick(cell)"
            >
              <span
                class="cell-date"
                :style="{
                  color: state.style.dateColor,
                  'font-size': state.style.dateFontSize + 'px'
                }"
                >{{ cell.day }}</span
              >
              <div class="cell-dots">
                <span
                  v-for="tp in cell.types"
                  :key="tp"
                  class="dot"
                  :style="{ background: typeColorOf(tp) }"
                  :title="typeLabelOf(tp)"
                ></span>
              </div>
            </div>
          </template>
          <div class="pop-list">
            <div
              class="pop-title"
              :class="{ 'pop-nowrap': !state.style.titlePopWrap }"
              :style="{
                color: state.style.titlePopColor,
                'font-size': state.style.titlePopFontSize + 'px',
                'font-weight': state.style.titlePopIsBolder ? 600 : 400,
                'text-align': state.style.titlePopAlign
              }"
            >
              {{ formatDate(cell.date) }}{{ state.style.popShowSummary !== false ? '（共 ' + cell.deployments.length + ' 个）' : '' }}
            </div>
            <div v-for="(d, i) in cell.deployments" :key="i" class="pop-item">
              <span
                class="pop-type"
                :class="{ 'pop-nowrap': !state.style.typePopWrap }"
                :style="{
                  color: state.style.typePopColor,
                  background: d.typeColor,
                  'font-size': state.style.typePopFontSize + 'px',
                  'font-weight': state.style.typePopIsBolder ? 600 : 400,
                  'text-align': state.style.typePopAlign
                }"
                >{{ d.typeLabel }}</span
              >
              <span
                class="pop-content"
                :class="{ 'pop-nowrap': !state.style.contentPopWrap }"
                :style="{
                  color: state.style.contentPopColor,
                  'font-size': state.style.contentPopFontSize + 'px',
                  'font-weight': state.style.contentPopIsBolder ? 600 : 400,
                  'text-align': state.style.contentPopAlign
                }"
                >{{ d.content }}</span
              >
            </div>
          </div>
        </el-popover>
      </div>
    </div>
    <div class="cal-legend" :style="{ color: state.style.legendColor }">
      <span v-for="tp in state.typeOrder" :key="tp" class="legend-item">
        <i class="dot" :style="{ background: typeColorOf(tp) }"></i>{{ typeLabelOf(tp) }}
      </span>
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
import { cloneDeep, defaultsDeep } from 'lodash-es'
import { DEFAULT_DEPLOY_CALENDAR_STYLE, COLOR_PANEL } from '@/views/chart/components/editor/util/chart'
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
  // 每条: { content, type: 版本类型原始值, raw }
  deployMap: new Map<string, any[]>(),
  // 字段名映射: { dateField, contentField, typeField }
  fieldMap: { dateField: null, contentField: null, typeField: null },
  useMock: true,
  // 日历样式（由 renderChart 从 customAttr.deployCalendar 合并默认值得来）
  style: cloneDeep(DEFAULT_DEPLOY_CALENDAR_STYLE),
  // 版本类型按出现顺序排列（图例顺序与配色循环依据）
  typeOrder: [] as string[],
  // 版本类型→颜色覆盖映射（来自 customAttr.deployCalendar.typeColorMap，用户可逐个覆盖）
  typeColorMap: {} as Record<string, string>
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

// 判断版本类型：
// 中文含"周/月/待定/紧急"，英文 weekly/monthly/pending/urgent 等识别
// 多个匹配 → 'both'（同时显示命中类型圆点）
// 都不匹配时，若该日期确有投产记录，回退为 'both'（保证圆点可见）
// 版本类型规范化：原始值 → 字符串 key（空值归为 ''）
const normalizeType = (raw: any): string => {
  if (raw === null || raw === undefined) return ''
  return String(raw).trim()
}

// 登记一个版本类型：若未见过则加入 typeOrder，并按出现顺序分配调色板颜色
// 用户在样式面板覆盖过 typeColorMap 的类型优先用覆盖色
const registerType = (tp: string) => {
  if (tp === undefined) return
  if (state.typeOrder.includes(tp)) return
  state.typeOrder.push(tp)
  if (!(tp in state.typeColorMap)) {
    const idx = state.typeOrder.length - 1
    state.typeColorMap[tp] = COLOR_PANEL[idx % COLOR_PANEL.length]
  }
}

// 每次取数前重置：清空出现顺序，保留用户在 customAttr 里配置的覆盖色
// 直接读 customAttr 源（而非 state.style），避免 calcData 在 renderChart 之前
// 调用时 state.style 尚未 hydrate，导致用户已配颜色被清空回退为调色板默认色
const resetTypes = () => {
  state.typeOrder = []
  const saved = view.value?.customAttr?.deployCalendar?.typeColorMap
  state.typeColorMap = saved && typeof saved === 'object' ? { ...saved } : {}
}

// 取数完成后再次同步用户覆盖色：覆盖"用户在 async getData 窗口期改色"的竞态
// （resetTypes 早于 getData 捕获旧值，期间用户改色只写入了 customAttr，未进入运行时 typeColorMap）
const syncTypeColorOverrides = () => {
  const saved = view.value?.customAttr?.deployCalendar?.typeColorMap
  if (!saved || typeof saved !== 'object') return
  state.typeOrder.forEach(tp => {
    if (saved[tp]) state.typeColorMap[tp] = saved[tp]
  })
}

// 类型→颜色：用户覆盖优先，否则按 typeOrder 顺序从调色板循环取色
const typeColorOf = (tp: string): string => {
  if (state.typeColorMap[tp]) return state.typeColorMap[tp]
  const idx = state.typeOrder.indexOf(tp)
  return COLOR_PANEL[(idx < 0 ? 0 : idx) % COLOR_PANEL.length]
}

// 类型显示标签：空值显示"未指定"，否则直接用原始值
const typeLabelOf = (tp: string) => {
  if (tp === '') return t('deploy_calendar.unspecified')
  return tp
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
  // 当日涉及的版本类型（去重，保留出现顺序）
  const types: string[] = []
  records.forEach(r => {
    if (!types.includes(r.type)) types.push(r.type)
  })
  const todayStr = formatDate(today)
  return {
    date: d,
    day: d.getDate(),
    inMonth,
    isToday: key === todayStr,
    types,
    deployments: records.map(r => ({
      content: r.content,
      typeLabel: typeLabelOf(r.type),
      typeColor: typeColorOf(r.type),
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
        registerType('周版')
        records.push({ content: `${d.getFullYear()}年第${getWeekOfYear(d)}周版本投产`, type: '周版', raw: '周版' })
      }
      if (d.getDate() === 1) {
        registerType('月版')
        records.push({ content: `${d.getFullYear()}年${d.getMonth() + 1}月版本投产`, type: '月版', raw: '月版' })
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
  // getData 返回的是 view 对象，fields/tableRow 嵌套在 view.data 中（与普通图表 ChartComponentS2 取 res?.data 一致）
  const payload = res?.data || res
  if (!payload) {
    console.warn('[DeployCalendar] parseData 失败：getData 返回空（res 为空），将回退 mock')
    return map
  }
  // 结构体检：后端返回体顶层字段一览，便于排查"fields/tableRow 缺失"类问题
  console.info('[DeployCalendar] 0. 后端返回结构体检:', {
    resKeys: Object.keys(res || {}),
    dataKeys: Object.keys(res?.data || {}),
    hasFields: Array.isArray(payload.fields),
    fieldsLen: Array.isArray(payload.fields) ? payload.fields.length : 0,
    hasTableRow: Array.isArray(payload.tableRow),
    tableRowLen: Array.isArray(payload.tableRow) ? payload.tableRow.length : 0,
    code: res?.code,
    msg: res?.msg
  })
  const fields = Array.isArray(payload.fields) ? payload.fields : []
  const rows = payload.tableRow || []
  if (fields.length === 0) {
    console.warn('[DeployCalendar] parseData 失败：后端返回 fields 为空。'
      + '最可能原因：后端 ChartDataManage.calcData 第151行按列权限/engineFieldName 过滤时，'
      + 'xAxis/yAxis/extStack 绑定的字段全被 removeIf 删除。请检查：① 真实数据集是否对日期/内容/类型列配了列权限导致被过滤；'
      + '② 数据集重建过字段，view 里绑定的字段 engineFieldName 与数据集当前字段对不上。')
  }
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
  if (!dateEng) {
    console.warn('[DeployCalendar] parseData 失败：未能从返回 fields 解析出日期列 engineFieldName。'
      + '可能：① fields 为空（见诊断0）；② xAxis 绑定的字段id', xFieldId, '不在后端返回的 fields(id列表) 里 → 字段被后端过滤或数据集字段被重建。'
      + '返回 fields 的 id 列表:', fields.map((f: any) => f.id))
    return map
  }
  // 兜底：若 tableRow 的 key 与 engineFieldName 不一致，
  // 用字段 id 对应的用户字段名(engToName)精确匹配 row key；匹配不到再模糊。
  const useOriginKey = !(dateEng in probeRow) && Object.keys(probeRow).length > 0
  const dateKey = useOriginKey
    ? (engToName[dateEng] && engToName[dateEng] in probeRow
        ? engToName[dateEng]
        : findKeyByName(probeRow, engToName[dateEng] || '') || dateEng)
    : dateEng
  const contentKey = useOriginKey
    ? (contentEng && engToName[contentEng] && engToName[contentEng] in probeRow
        ? engToName[contentEng]
        : contentEng && findKeyByName(probeRow, engToName[contentEng] || '') || contentEng)
    : contentEng
  const typeKey = useOriginKey
    ? (typeEng && engToName[typeEng] && engToName[typeEng] in probeRow
        ? engToName[typeEng]
        : typeEng && findKeyByName(probeRow, engToName[typeEng] || '') || typeEng)
    : typeEng
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
    const system = contentKey ? row[contentKey] : ''
    const systemStr = system === null || system === undefined ? '' : String(system).trim()
    const rawType = typeKey ? row[typeKey] : ''
    // 版本类型直接用原始值作 key（空值归为 ''），不再正则映射
    const tp = normalizeType(rawType)
    registerType(tp)
    const list = map.get(key) || []
    // 同一天内相同关联系统名称去重（含空值），避免同一系统多条投产记录在 popover 中重复显示
    if (list.some(r => r.content === systemStr)) return
    list.push({ content: systemStr, type: tp, raw: rawType, rawDate: dateRaw })
    map.set(key, list)
  })
  console.log('[DeployCalendar] 9. 行数统计:', '总行=', rows.length, '有日期=', withDate, '解析成功=', map.size)
  if (withDate > 0 && map.size === 0) {
    // 有日期值但全部解析失败：打印前几个原始值，便于定位日期格式问题
    const sample = rows.slice(0, 5).map((r: any) => r[dateKey]).filter(Boolean)
    console.warn('[DeployCalendar] parseData 失败：有', withDate, '行日期值但 parseDate 全部解析失败，将回退 mock。'
      + '前5个原始日期值:', JSON.stringify(sample), 'dateKey=', dateKey)
  }
  return map
}

const parseDate = (raw: any): Date | null => {
  if (raw instanceof Date) {
    // Date 对象：取本地年月日构造纯日期，避免时区偏移导致圆点偏移到相邻日期
    return new Date(raw.getFullYear(), raw.getMonth(), raw.getDate())
  }
  if (typeof raw !== 'string' && typeof raw !== 'number') return null
  const s = String(raw).trim()
  // 支持 YYYY-MM-DD / YYYY/MM/DD（后跟可选时分秒或 T...ISO 格式）
  // 只取年月日，用 new Date(y, m-1, d) 构造本地纯日期，避免时区偏移
  let m = s.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/)
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  m = s.match(/^(\d{4})(\d{2})(\d{2})$/)
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  m = s.match(/^(\d{4})[-/]?(0?[1-9]|1[0-2])$/)
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, 1)
  // ISO 格式 2026-07-03T00:00:00.000Z：提取年月日构造本地纯日期
  m = s.match(/^(\d{4})-(\d{2})-(\d{2})T/)
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  const dt = new Date(s)
  if (isNaN(dt.getTime())) return null
  // 兜底：取本地年月日构造纯日期，避免时区偏移
  return new Date(dt.getFullYear(), dt.getMonth(), dt.getDate())
}

const calcData = (viewCalc: Chart, callback?: () => void) => {
  isError.value = false
  // 重置类型列表：清空出现顺序，保留用户在 customAttr 里的覆盖色
  resetTypes()
  // 注意：不在此处调 renderChart()，避免写 reactive state.style 触发同步重绘
  // 波及父组件 index.vue 在 view 切换间隙 view.value 为 undefined 时崩溃。
  // 样式更新由 watch(customAttr.deployCalendar) + 父组件 renderChart 回调驱动。
  if (!viewCalc || !viewCalc.tableId || viewCalc['dataFrom'] === 'template' || !viewCalc?.xAxis?.length) {
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
  // 清空 yAxis/extStack/drillFields，避免后端 formatAxis 把残留字段放进 yAxis/drill 轴
  // （table-info 只用 xAxis，残留轴字段会被列权限过滤且干扰诊断）
  v.yAxis = []
  v.extStack = []
  v.drillFields = []
  v.type = 'table-info'
  v.render = 'antv'
  // 强制关闭明细表分页：投产日历需要全部投产数据，不能只取默认 pageSize（20行）
  // 后端 TableInfoHandler.customFilter 会根据 basicStyle.tablePageMode=='page' 设置 LIMIT，
  // 改成非 page 值后走 goPage=null/pageSize=null 分支，SQL 不加 LIMIT 返回全量。
  if (v.customAttr?.basicStyle) {
    v.customAttr.basicStyle.tablePageMode = 'all'
  }
  console.info('[DeployCalendar] 7. 传给后端的 view:', {
    viewId: v.id, type: v.type,
    xAxisIds: v.xAxis.map((f: any) => f?.id),
    yAxisLen: v.yAxis.length, extStackLen: v.extStack.length, drillFieldsLen: v.drillFields.length,
    fieldDetails: v.xAxis.map((f: any) => ({ id: f?.id, eng: f?.engineFieldName, name: f?.name, groupType: f?.groupType, summary: f?.summary }))
  })
  getData(v)
    .then(res => {
      // 诊断：传给后端的字段 vs 后端实际返回的字段，定位"字段被列权限/engineFieldName 过滤"问题
      const sentFieldIds = [...xArr, ...yArr, ...extArr].map((f: any) => f?.id).filter(Boolean)
      const retFields = Array.isArray(res?.data?.fields) ? res.data.fields : []
      const retFieldIds = retFields.map((f: any) => f?.id)
      const retFieldEngs = retFields.map((f: any) => f?.engineFieldName)
      console.info('[DeployCalendar] 6. 传给后端的字段id:', sentFieldIds, '后端返回字段id:', retFieldIds, 'engineFieldName:', retFieldEngs)
      if (sentFieldIds.length > 0 && retFieldIds.length === 0) {
        console.warn('[DeployCalendar] 诊断：传出了', sentFieldIds.length, '个字段，但后端返回 fields 为空 → 后端把这些字段全过滤掉了（见 calcData 列权限 removeIf）。'
          + '请确认数据集字段权限 / 字段是否被重建导致 engineFieldName 不匹配。')
      }
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
        if (res?.data?.fields && curViewInfo) {
          curViewInfo['curFields'] = res.data.fields || []
        }
        dvMainStore.setViewDataDetails(props.element.id, res)
        // 数据加载后同步用户覆盖色（覆盖 resetTypes 与 getData 之间的竞态窗口）
        syncTypeColorOverrides()
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
  if (view.value) {
    calcData(view.value, () => {})
  }
})

const renderChart = () => {
  try {
    const raw = view.value?.customAttr?.deployCalendar
    const custom = raw
      ? defaultsDeep(cloneDeep(raw), cloneDeep(DEFAULT_DEPLOY_CALENDAR_STYLE))
      : cloneDeep(DEFAULT_DEPLOY_CALENDAR_STYLE)
    state.style = custom
    // 同步用户覆盖色到运行时 typeColorMap（样式面板改色后圆点/图例立即生效）
    // 已登记类型若用户未覆盖则保留自动分配色，已覆盖则用 customAttr 里的值
    const saved = custom?.typeColorMap
    if (saved && typeof saved === 'object') {
      state.typeOrder.forEach(tp => {
        if (saved[tp]) state.typeColorMap[tp] = saved[tp]
      })
    }
  } catch (e) {
    // 防御：配置异常时回退默认值，避免日历报错波及整个大屏渲染
    console.warn('[DeployCalendar] renderChart 读取样式失败，回退默认值', e)
    state.style = cloneDeep(DEFAULT_DEPLOY_CALENDAR_STYLE)
  }
}

onBeforeMount(() => {
  if (view.value) {
    calcData(view.value)
    // 初始挂载时应用一次样式（calcData 内不再调，避免重绘波及父组件）
    renderChart()
  }
})

// 样式配置变化时实时更新（右侧面板改颜色/字号等）
watch(
  () => view.value?.customAttr?.deployCalendar,
  () => {
    renderChart()
  },
  { deep: true }
)

defineExpose({ calcData, renderChart })
</script>

<style lang="less" scoped>
// 颜色全部由模板内联 :style（读取 state.style）驱动，这里只保留布局与结构样式。
// 悬浮底色通过根节点 CSS 变量 --dc-cell-hover 传入（见模板 :style）。
.deploy-calendar-main {
  overflow: hidden;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
  font-size: 12px;
  user-select: none;
}
.cal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  .cal-nav {
    display: flex;
    align-items: center;
    gap: 6px;
    .cal-nav-btn {
      cursor: pointer;
      font-size: 16px;
    }
  }
}
.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
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
  &:hover {
    background: var(--dc-cell-hover) !important;
  }
  &.is-other-month {
    opacity: 0.35;
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
  .cell-dots {
    display: flex;
    gap: 3px;
    height: 6px;
  }
}
.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.cal-legend {
  display: flex;
  gap: 14px;
  justify-content: center;
  margin-top: 6px;
  .legend-item {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}
</style>

<style lang="less">
.deploy-calendar-popover .pop-list {
  max-height: 260px;
  overflow-y: auto;
  .pop-title {
    // 颜色/字号/加粗/对齐由模板内联 :style 驱动，这里只保留布局
    margin-bottom: 6px;
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
      // 文字色/字号/加粗/底色均由模板内联 :style 驱动（底色=类型动态色）
    }
    .pop-content {
      flex: 1;
      // 默认换行；不换行时由 .pop-nowrap class 接管（单行截断省略号）
      word-break: break-all;
    }
  }
  // 换行关闭时的单行截断（配合 wrap=false）
  .pop-nowrap {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
