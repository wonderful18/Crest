<script lang="ts" setup>
import { computed, onMounted, reactive, watch, nextTick, PropType } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import {
  COLOR_PANEL,
  DEFAULT_DEPLOY_CALENDAR_STYLE
} from '@/views/chart/components/editor/util/chart'
import { cloneDeep, defaultsDeep } from 'lodash-es'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'

const { t } = useI18n()
const dvMainStore = dvMainStoreWithOut()

const props = defineProps({
  chart: {
    type: Object,
    required: true
  },
  themes: {
    type: String as PropType<EditorTheme>,
    default: 'dark'
  },
  propertyInner: {
    type: Array<string>
  }
})

const emit = defineEmits(['onDeployCalendarStyleChange'])

const predefineColors = COLOR_PANEL

const fontSizeList = computed(() => {
  const arr = []
  for (let i = 10; i <= 60; i = i + 2) {
    arr.push({ name: i + '', value: i })
  }
  return arr
})
const titleFontSizeList = computed(() => {
  const arr = []
  for (let i = 12; i <= 48; i = i + 2) {
    arr.push({ name: i + '', value: i })
  }
  return arr
})

const state = reactive({
  form: JSON.parse(JSON.stringify(DEFAULT_DEPLOY_CALENDAR_STYLE))
})

const change = (prop: string) => {
  emit('onDeployCalendarStyleChange', state.form, prop)
}

// 版本类型→颜色覆盖映射兜底（老配置可能无此字段）
const ensureTypeColorMap = () => {
  if (!state.form.typeColorMap || typeof state.form.typeColorMap !== 'object') {
    state.form.typeColorMap = {}
  }
}

// 从当前图表数据去重出版本类型列表，供动态渲染颜色选择器
// deploy-calendar 是自定义组件，查询结果由 Component.vue 通过 setViewDataDetails 写入
// canvasViewDataInfo（而非 view.data），故此处用 getViewDataDetails 读取（同 TableHeaderGroupConfig）
// 访问 dvMainStore.canvasViewDataInfo 以建立响应式依赖，数据加载后列表自动更新
const typeColorList = computed<{ type: string; label: string }[]>(() => {
  const chart = props.chart as any
  // 触发响应式依赖
  void dvMainStore.canvasViewDataInfo
  const viewData = dvMainStore.getViewDataDetails(chart?.id)
  const rows = viewData?.tableRow
  if (!Array.isArray(rows) || rows.length === 0) return []
  const extField = chart?.extStack?.[0]
  if (!extField?.id) return []
  const fields = Array.isArray(viewData?.fields) ? viewData.fields : []
  // 字段 id → engineFieldName
  const idToEng = fields.reduce((pre: any, f: any) => {
    if (f?.id) pre[f.id] = f.engineFieldName || f.name
    return pre
  }, {})
  let typeKey = idToEng[extField.id]
  // 兜底：engineFieldName 不在 row key 里时，按用户字段名模糊匹配
  const probe = rows[0] || {}
  if (!typeKey || !(typeKey in probe)) {
    const engToName = fields.reduce((pre: any, f: any) => {
      if (f?.engineFieldName) pre[f.engineFieldName] = f.name
      return pre
    }, {})
    const name = engToName[typeKey]
    typeKey = (name && name in probe) ? name : (Object.keys(probe).find(k => typeof k === 'string' && name && k.includes(name)) || typeKey)
  }
  if (!typeKey) return []
  const seen = new Set<string>()
  const list: { type: string; label: string }[] = []
  rows.forEach((row: any) => {
    if (!row) return
    const v = row[typeKey]
    const tp = v === null || v === undefined ? '' : String(v).trim()
    if (seen.has(tp)) return
    seen.add(tp)
    list.push({ type: tp, label: tp === '' ? t('deploy_calendar.unspecified') : tp })
  })
  return list
})

// 单个类型颜色变更
const changeTypeColor = (tp: string) => {
  ensureTypeColorMap()
  emit('onDeployCalendarStyleChange', state.form, 'typeColorMap')
}

const init = () => {
  const custom = defaultsDeep(
    cloneDeep(props.chart?.customAttr?.deployCalendar),
    cloneDeep(DEFAULT_DEPLOY_CALENDAR_STYLE)
  )
  state.form = cloneDeep(custom)
  ensureTypeColorMap()
  // 颜色初次赋值兜底
  nextTick(() => {
    state.form.titleColor = custom.titleColor
    state.form.dateColor = custom.dateColor
  })
}

onMounted(() => {
  init()
})

watch(
  () => props.chart?.customAttr?.deployCalendar,
  () => {
    init()
  },
  { deep: true }
)
</script>

<template>
  <div>
    <el-form
      :model="state.form"
      label-position="top"
      size="small"
      :effect="themes"
    >
      <!-- 月份标题 -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.title_color')"
      >
        <div style="display: flex">
          <el-color-picker
            :effect="themes"
            v-model="state.form.titleColor"
            class="color-picker-style"
            :predefine="predefineColors"
            @change="change('titleColor')"
            show-alpha
            is-custom
          />
          <el-tooltip :content="t('chart.font_size')" placement="top">
            <el-select
              style="width: 56px; margin-left: 8px"
              :effect="themes"
              v-model="state.form.titleFontSize"
              :placeholder="t('chart.text_fontsize')"
              size="small"
              @change="change('titleFontSize')"
            >
              <el-option
                v-for="option in titleFontSizeList"
                :key="option.value"
                :label="option.name"
                :value="option.value"
              />
            </el-select>
          </el-tooltip>
        </div>
      </el-form-item>
      <el-form-item class="form-item" :class="'form-item-' + themes" :effect="themes">
        <el-checkbox
          size="small"
          :effect="themes"
          v-model="state.form.titleIsBolder"
          @change="change('titleIsBolder')"
        >
          {{ t('deploy_calendar.title_bolder') }}
        </el-checkbox>
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 日期文字 -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.date_color')"
      >
        <div style="display: flex">
          <el-color-picker
            :effect="themes"
            v-model="state.form.dateColor"
            class="color-picker-style"
            :predefine="predefineColors"
            @change="change('dateColor')"
            show-alpha
            is-custom
          />
          <el-tooltip :content="t('chart.font_size')" placement="top">
            <el-select
              style="width: 56px; margin-left: 8px"
              :effect="themes"
              v-model="state.form.dateFontSize"
              :placeholder="t('chart.text_fontsize')"
              size="small"
              @change="change('dateFontSize')"
            >
              <el-option
                v-for="option in fontSizeList"
                :key="option.value"
                :label="option.name"
                :value="option.value"
              />
            </el-select>
          </el-tooltip>
        </div>
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 单元格底色 -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.cell_bg_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.cellBgColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('cellBgColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.cell_hover_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.cellHoverColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('cellHoverColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.deploy_cell_bg_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.deployCellBgColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('deployCellBgColor')"
          show-alpha
          is-custom
        />
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 状态高亮 -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.today_border_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.todayBorderColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('todayBorderColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.selected_border_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.selectedBorderColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('selectedBorderColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.selected_bg_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.selectedBgColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('selectedBgColor')"
          show-alpha
          is-custom
        />
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 类型颜色（动态：按数据集里实际出现的版本类型逐个配色） -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.type_colors')"
      >
        <div v-if="typeColorList.length === 0" class="type-color-empty">
          {{ t('deploy_calendar.type_colors_empty') }}
        </div>
        <div v-else class="type-color-list">
          <div
            v-for="item in typeColorList"
            :key="item.type"
            class="type-color-row"
          >
            <span class="type-color-label" :title="item.label">{{ item.label }}</span>
            <el-color-picker
              :effect="themes"
              v-model="state.form.typeColorMap[item.type]"
              class="color-picker-style"
              :predefine="predefineColors"
              @change="changeTypeColor(item.type)"
              show-alpha
              is-custom
            />
          </div>
        </div>
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.weekday_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.weekdayColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('weekdayColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.legend_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.legendColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('legendColor')"
          show-alpha
          is-custom
        />
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 弹出标题（popover 日期标题） -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.title_pop_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.titlePopColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('titlePopColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item class="form-item" :class="'form-item-' + themes" :effect="themes">
        <el-checkbox
          size="small"
          :effect="themes"
          v-model="state.form.popShowSummary"
          @change="change('popShowSummary')"
        >
          {{ t('deploy_calendar.pop_summary_show') }}
        </el-checkbox>
      </el-form-item>
      <el-form-item class="form-item" :class="'form-item-' + themes" :effect="themes">
        <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap">
          <el-tooltip :content="t('chart.font_size')" placement="top">
            <el-select
              style="width: 56px"
              :effect="themes"
              v-model="state.form.titlePopFontSize"
              :placeholder="t('chart.text_fontsize')"
              size="small"
              @change="change('titlePopFontSize')"
            >
              <el-option
                v-for="option in fontSizeList"
                :key="option.value"
                :label="option.name"
                :value="option.value"
              />
            </el-select>
          </el-tooltip>
          <el-select
            style="width: 80px"
            :effect="themes"
            v-model="state.form.titlePopAlign"
            size="small"
            @change="change('titlePopAlign')"
          >
            <el-option :label="t('deploy_calendar.align_left')" value="left" />
            <el-option :label="t('deploy_calendar.align_center')" value="center" />
            <el-option :label="t('deploy_calendar.align_right')" value="right" />
          </el-select>
          <el-checkbox
            size="small"
            :effect="themes"
            v-model="state.form.titlePopIsBolder"
            @change="change('titlePopIsBolder')"
            >{{ t('deploy_calendar.title_pop_bolder') }}</el-checkbox
          >
          <el-checkbox
            size="small"
            :effect="themes"
            v-model="state.form.titlePopWrap"
            @change="change('titlePopWrap')"
            >{{ t('deploy_calendar.title_pop_wrap') }}</el-checkbox
          >
        </div>
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 弹出类型标签（popover 版本类型标签） -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.type_pop_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.typePopColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('typePopColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item class="form-item" :class="'form-item-' + themes" :effect="themes">
        <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap">
          <el-tooltip :content="t('chart.font_size')" placement="top">
            <el-select
              style="width: 56px"
              :effect="themes"
              v-model="state.form.typePopFontSize"
              :placeholder="t('chart.text_fontsize')"
              size="small"
              @change="change('typePopFontSize')"
            >
              <el-option
                v-for="option in fontSizeList"
                :key="option.value"
                :label="option.name"
                :value="option.value"
              />
            </el-select>
          </el-tooltip>
          <el-select
            style="width: 80px"
            :effect="themes"
            v-model="state.form.typePopAlign"
            size="small"
            @change="change('typePopAlign')"
          >
            <el-option :label="t('deploy_calendar.align_left')" value="left" />
            <el-option :label="t('deploy_calendar.align_center')" value="center" />
            <el-option :label="t('deploy_calendar.align_right')" value="right" />
          </el-select>
          <el-checkbox
            size="small"
            :effect="themes"
            v-model="state.form.typePopIsBolder"
            @change="change('typePopIsBolder')"
            >{{ t('deploy_calendar.type_pop_bolder') }}</el-checkbox
          >
          <el-checkbox
            size="small"
            :effect="themes"
            v-model="state.form.typePopWrap"
            @change="change('typePopWrap')"
            >{{ t('deploy_calendar.type_pop_wrap') }}</el-checkbox
          >
        </div>
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 弹出内容（popover 投产内容文字） -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.content_pop_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.contentPopColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('contentPopColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item class="form-item" :class="'form-item-' + themes" :effect="themes">
        <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap">
          <el-tooltip :content="t('chart.font_size')" placement="top">
            <el-select
              style="width: 56px"
              :effect="themes"
              v-model="state.form.contentPopFontSize"
              :placeholder="t('chart.text_fontsize')"
              size="small"
              @change="change('contentPopFontSize')"
            >
              <el-option
                v-for="option in fontSizeList"
                :key="option.value"
                :label="option.name"
                :value="option.value"
              />
            </el-select>
          </el-tooltip>
          <el-select
            style="width: 80px"
            :effect="themes"
            v-model="state.form.contentPopAlign"
            size="small"
            @change="change('contentPopAlign')"
          >
            <el-option :label="t('deploy_calendar.align_left')" value="left" />
            <el-option :label="t('deploy_calendar.align_center')" value="center" />
            <el-option :label="t('deploy_calendar.align_right')" value="right" />
          </el-select>
          <el-checkbox
            size="small"
            :effect="themes"
            v-model="state.form.contentPopIsBolder"
            @change="change('contentPopIsBolder')"
            >{{ t('deploy_calendar.content_pop_bolder') }}</el-checkbox
          >
          <el-checkbox
            size="small"
            :effect="themes"
            v-model="state.form.contentPopWrap"
            @change="change('contentPopWrap')"
            >{{ t('deploy_calendar.content_pop_wrap') }}</el-checkbox
          >
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<style lang="less" scoped>
.form-item {
  margin-bottom: 12px;
}
:deep(.ed-color-picker.is-custom .ed-color-picker__mask) {
  height: 26px;
  width: 48px;
}
.m-divider {
  margin: 0 0 12px;
  border-color: rgba(31, 35, 41, 0.15);

  &.divider-dark {
    border-color: rgba(255, 255, 255, 0.15);
  }
}
.type-color-empty {
  font-size: 12px;
  opacity: 0.6;
  line-height: 1.6;
}
.type-color-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.type-color-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.type-color-label {
  flex: 1;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
