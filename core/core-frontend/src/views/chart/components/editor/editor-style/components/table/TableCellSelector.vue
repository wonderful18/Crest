<script lang="ts" setup>
import icon_bold_outlined from '@/assets/svg/icon_bold_outlined.svg'
import icon_italic_outlined from '@/assets/svg/icon_italic_outlined.svg'
import icon_leftAlignment_outlined from '@/assets/svg/icon_left-alignment_outlined.svg'
import icon_centerAlignment_outlined from '@/assets/svg/icon_center-alignment_outlined.svg'
import icon_rightAlignment_outlined from '@/assets/svg/icon_right-alignment_outlined.svg'
import icon_customAlignment_outlined from '@/assets/svg/icon_custom-alignment_outlined.svg'
import icon_info_outlined from '@/assets/svg/icon_info_outlined.svg'
import { computed, onMounted, PropType, reactive, watch } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { COLOR_PANEL, DEFAULT_TABLE_CELL } from '@/views/chart/components/editor/util/chart'
import { ElSpace } from 'element-plus-secondary'
import { cloneDeep, defaultsDeep } from 'lodash-es'
import { convertToAlphaColor, isAlphaColor } from '@/views/chart/components/js/util'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { storeToRefs } from 'pinia'
import { SERIES_NUMBER_FIELD } from '@antv/s2'
import { getMergeableFieldOptions } from '@/views/chart/components/js/panel/common/tableMergeCells.mjs'
import { createTableCellChangePayload } from './tableCellMergeConfig.mjs'
const dvMainStore = dvMainStoreWithOut()
const { mobileInPc } = storeToRefs(dvMainStore)

const { t } = useI18n()

const props = defineProps({
  chart: {
    type: Object as PropType<ChartObj>,
    required: true
  },
  themes: {
    type: String,
    default: 'dark'
  },
  propertyInner: {
    type: Array<string>
  }
})

watch(
  [
    () => props.chart.customAttr.tableCell,
    () => props.chart.xAxis,
    () => props.chart.yAxis,
    () => props.chart.customAttr.tableHeader
  ],
  () => {
    init()
  },
  { deep: true }
)

const predefineColors = COLOR_PANEL

const fontSizeList = computed(() => {
  const arr = []
  for (let i = 10; i <= 40; i = i + 2) {
    arr.push({
      name: i + '',
      value: i
    })
  }
  for (let i = 50; i <= 200; i = i + 10) {
    arr.push({
      name: i + '',
      value: i
    })
  }
  return arr
})

const state = reactive({
  tableCellForm: {} as ChartTableCellAttr
})

const alignConfig = reactive({
  id: '',
  align: 'left'
})

const showCustomAlign = computed(() => {
  return ['table-info', 'table-normal'].includes(props.chart.type)
})

const alignConfigOptions = reactive([])
const mergeFieldOptions = reactive([])
// 单元格分组可选项：定位列/起列/止列，取 xAxis 全部列（含指标，不截断在 quota 前）
const cellGroupFieldOptions = reactive([])

const emit = defineEmits(['onTableCellChange'])

const changeTableCell = prop => {
  if (prop === 'alignConfig') {
    state.tableCellForm.alignConfig = alignConfigOptions.map(item => ({
      id: item.id,
      align: item.id === alignConfig.id ? alignConfig.align : item.align
    }))
  }
  const payload = createTableCellChangePayload(state.tableCellForm, prop, mergeFieldOptions)
  if (prop === 'mergeCells' || prop === 'mergeFields') {
    state.tableCellForm.mergeCells = payload.mergeCells
    state.tableCellForm.mergeFields = payload.mergeFields
  }
  emit('onTableCellChange', payload, prop)
}

const changeAlignConfig = () => {
  const selected = state.tableCellForm.alignConfig.find(item => item.id === alignConfig.id)
  if (selected) {
    alignConfig.align = selected.align
  }
}

// 单元格分组：开关 = 数组非空
const cellGroupsEnabled = computed(() => (state.tableCellForm.cellGroups?.length ?? 0) > 0)
const toggleCellGroups = (val: boolean) => {
  state.tableCellForm.cellGroups = val ? [createEmptyCellGroup()] : []
  changeTableCell('cellGroups')
}
const createEmptyCellGroup = () => ({
  matchField: cellGroupFieldOptions[0]?.id ?? '',
  matchValue: '',
  startField: cellGroupFieldOptions[0]?.id ?? '',
  endField: cellGroupFieldOptions[0]?.id ?? '',
  text: ''
})
// 匹配值候选：从当前页数据按定位列去重，供模糊搜索下拉
const getMatchValueOptions = (matchField: string) => {
  const rows = props.chart?.data?.tableRow
  if (!Array.isArray(rows) || !matchField) {
    return []
  }
  const seen = new Set<string>()
  const options: { id: string; label: string }[] = []
  rows.forEach((row: any) => {
    const v = row?.[matchField]
    if (v == null) return
    const key = String(v)
    if (seen.has(key)) return
    seen.add(key)
    options.push({ id: key, label: key })
  })
  return options
}
const addCellGroup = () => {
  state.tableCellForm.cellGroups.push(createEmptyCellGroup())
  changeTableCell('cellGroups')
}
const removeCellGroup = (index: number) => {
  state.tableCellForm.cellGroups.splice(index, 1)
  changeTableCell('cellGroups')
}
// 是否处于合并态（自动或手动），用于互斥禁用斑马纹/冻结
const hasMerge = computed(
  () => state.tableCellForm.mergeCells || (state.tableCellForm.cellGroups?.length ?? 0) > 0
)

const init = () => {
  const tableCell = props.chart?.customAttr?.tableCell
  if (tableCell) {
    state.tableCellForm = defaultsDeep(cloneDeep(tableCell), cloneDeep(DEFAULT_TABLE_CELL))
    if (tableCell.mergeCells === undefined) {
      state.tableCellForm.mergeCells = false
    }
    // cellGroups 容错：老图表无此字段
    state.tableCellForm.cellGroups = Array.isArray(tableCell.cellGroups)
      ? tableCell.cellGroups
      : []
    const alpha = props.chart.customAttr.basicStyle.alpha

    if (!isAlphaColor(state.tableCellForm.tableItemBgColor)) {
      state.tableCellForm.tableItemBgColor = convertToAlphaColor(
        state.tableCellForm.tableItemBgColor,
        alpha
      )
    }

    if (!isAlphaColor(state.tableCellForm.tableItemSubBgColor)) {
      state.tableCellForm.tableItemSubBgColor = convertToAlphaColor(
        state.tableCellForm.tableItemSubBgColor,
        alpha
      )
    }

    if (['table-info', 'table-normal'].includes(props.chart.type)) {
      const axis = [...props.chart?.xAxis]
      if (props.chart?.type === 'table-normal') {
        axis.push(...props.chart?.yAxis)
      }
      const alignCfg = props.chart?.customAttr?.tableCell?.alignConfig || []
      const alignCfgMap = alignCfg?.reduce((p, n) => {
        p[n.id] = n.align
        return p
      }, {})
      alignConfigOptions.splice(0, alignConfigOptions.length)
      const tableHeader = props.chart?.customAttr?.tableHeader
      if (tableHeader?.showIndex) {
        alignConfigOptions.push({
          id: SERIES_NUMBER_FIELD,
          label: tableHeader.indexLabel,
          align: alignCfgMap[SERIES_NUMBER_FIELD] || 'left'
        })
      }
      axis.forEach(item => {
        const align = alignCfgMap[item.engineFieldName] || 'left'
        alignConfigOptions.push({
          id: item.engineFieldName,
          label: item.chartShowName ?? item.name,
          align
        })
      })
      if (alignConfigOptions.length) {
        const exist = alignConfigOptions.findIndex(item => item.id === alignConfig.id) !== -1
        if (!exist) {
          alignConfig.id = alignConfigOptions[0].id
          alignConfig.align = alignConfigOptions[0].align
        }
      } else {
        alignConfig.id = ''
        alignConfig.align = 'left'
      }
    }
    mergeFieldOptions.splice(0, mergeFieldOptions.length)
    // 单元格分组：table-info 的全部列均可参与
    cellGroupFieldOptions.splice(0, cellGroupFieldOptions.length)
    if (props.chart.type === 'table-info') {
      const groupOptions = (props.chart?.xAxis || []).map(item => ({
        id: item.engineFieldName,
        label: item.chartShowName ?? item.name
      }))
      cellGroupFieldOptions.push(...groupOptions)
      const options = getMergeableFieldOptions(props.chart?.xAxis || [])
      mergeFieldOptions.push(...options)
      const optionIds = new Set(options.map(item => item.id))
      if (state.tableCellForm.mergeCells && !options.length) {
        state.tableCellForm.mergeCells = false
        state.tableCellForm.mergeFields = []
      } else if (Array.isArray(tableCell.mergeFields)) {
        state.tableCellForm.mergeFields = tableCell.mergeFields.filter(id => optionIds.has(id))
      } else if (state.tableCellForm.mergeCells) {
        state.tableCellForm.mergeFields = options.map(item => item.id)
      }
    }
  }
}
const showProperty = prop => props.propertyInner?.includes(prop)

onMounted(() => {
  init()
})
</script>

<template>
  <el-form size="small" ref="tableCellForm" :model="state.tableCellForm" label-position="top">
    <el-form-item
      :label="t('chart.backgroundColor')"
      class="form-item"
      :class="'form-item-' + themes"
      v-if="showProperty('tableItemBgColor') && state.tableCellForm.tableItemBgColor"
    >
      <el-color-picker
        :effect="themes"
        is-custom
        :trigger-width="108"
        v-model="state.tableCellForm.tableItemBgColor"
        :predefine="predefineColors"
        show-alpha
        @change="changeTableCell('tableItemBgColor')"
      />
    </el-form-item>
    <el-form-item
      :class="'form-item-' + themes"
      class="form-item"
      v-if="showProperty('enableTableCrossBG')"
      label=""
    >
      <el-checkbox
        v-model="state.tableCellForm.enableTableCrossBG"
        :effect="themes"
        :disabled="hasMerge"
        @change="changeTableCell('enableTableCrossBG')"
      >
        <span class="data-area-label">
          <span style="margin-right: 4px">{{ t('chart.stripe') }}</span>
          <el-tooltip
            class="item"
            effect="dark"
            placement="bottom"
            v-if="state.tableCellForm.mergeCells"
          >
            <template #content>
              <div>{{ t('chart.table_cross_bg_tip') }}</div>
            </template>
            <el-icon class="hint-icon" :class="{ 'hint-icon--dark': themes === 'dark' }">
              <Icon name="icon_info_outlined"><icon_info_outlined class="svg-icon" /></Icon>
            </el-icon>
          </el-tooltip>
        </span>
      </el-checkbox>
    </el-form-item>
    <el-form-item
      :class="'form-item-' + themes"
      class="form-item"
      label=""
      v-if="showProperty('tableItemSubBgColor') && state.tableCellForm.tableItemSubBgColor"
    >
      <el-color-picker
        v-model="state.tableCellForm.tableItemSubBgColor"
        :effect="themes"
        :predefine="predefineColors"
        :disabled="!state.tableCellForm.enableTableCrossBG"
        is-custom
        show-alpha
        @change="changeTableCell('tableItemSubBgColor')"
      />
    </el-form-item>
    <el-space>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        v-if="showProperty('tableFontColor')"
        :label="t('chart.text')"
      >
        <el-color-picker
          :effect="themes"
          is-custom
          v-model="state.tableCellForm.tableFontColor"
          :predefine="predefineColors"
          @change="changeTableCell('tableFontColor')"
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        v-if="showProperty('tableItemFontSize')"
      >
        <template #label>&nbsp;</template>
        <el-select
          style="width: 58px"
          :effect="themes"
          v-model="state.tableCellForm.tableItemFontSize"
          @change="changeTableCell('tableItemFontSize')"
        >
          <el-option
            v-for="option in fontSizeList"
            :key="option.value"
            :label="option.name"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
    </el-space>
    <el-space :class="{ 'mobile-style': mobileInPc }">
      <el-form-item class="form-item" :class="'form-item-' + themes">
        <el-checkbox
          :effect="themes"
          class="icon-checkbox"
          v-model="state.tableCellForm.isBolder"
          @change="changeTableCell('isBolder')"
        >
          <el-tooltip effect="dark" placement="top">
            <template #content>
              {{ t('chart.bolder') }}
            </template>
            <div
              class="icon-btn"
              :class="{ dark: themes === 'dark', active: state.tableCellForm.isBolder }"
            >
              <el-icon>
                <Icon name="icon_bold_outlined"><icon_bold_outlined class="svg-icon" /></Icon>
              </el-icon>
            </div>
          </el-tooltip>
        </el-checkbox>
      </el-form-item>

      <el-form-item class="form-item" :class="'form-item-' + themes">
        <el-checkbox
          :effect="themes"
          class="icon-checkbox"
          v-model="state.tableCellForm.isItalic"
          @change="changeTableCell('isItalic')"
        >
          <el-tooltip effect="dark" placement="top">
            <template #content>
              {{ t('chart.italic') }}
            </template>
            <div
              class="icon-btn"
              :class="{ dark: themes === 'dark', active: state.tableCellForm.isItalic }"
            >
              <el-icon>
                <Icon name="icon_italic_outlined"><icon_italic_outlined class="svg-icon" /></Icon>
              </el-icon>
            </div>
          </el-tooltip>
        </el-checkbox>
      </el-form-item>

      <div class="position-divider" :class="'position-divider--' + themes"></div>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        v-if="showProperty('tableItemAlign')"
      >
        <el-radio-group
          class="icon-radio-group"
          v-model="state.tableCellForm.tableItemAlign"
          @change="changeTableCell('tableItemAlign')"
        >
          <el-radio value="left">
            <el-tooltip effect="dark" placement="top">
              <template #content>
                {{ t('chart.text_pos_left') }}
              </template>
              <div
                class="icon-btn"
                :class="{
                  dark: themes === 'dark',
                  active: state.tableCellForm.tableItemAlign === 'left'
                }"
              >
                <el-icon>
                  <Icon name="icon_left-alignment_outlined"
                    ><icon_leftAlignment_outlined class="svg-icon"
                  /></Icon>
                </el-icon>
              </div>
            </el-tooltip>
          </el-radio>
          <el-radio value="center">
            <el-tooltip effect="dark" placement="top">
              <template #content>
                {{ t('chart.text_pos_center') }}
              </template>
              <div
                class="icon-btn"
                :class="{
                  dark: themes === 'dark',
                  active: state.tableCellForm.tableItemAlign === 'center'
                }"
              >
                <el-icon>
                  <Icon name="icon_center-alignment_outlined"
                    ><icon_centerAlignment_outlined class="svg-icon"
                  /></Icon>
                </el-icon>
              </div>
            </el-tooltip>
          </el-radio>
          <el-radio value="right">
            <el-tooltip effect="dark" placement="top">
              <template #content>
                {{ t('chart.text_pos_right') }}
              </template>
              <div
                class="icon-btn"
                :class="{
                  dark: themes === 'dark',
                  active: state.tableCellForm.tableItemAlign === 'right'
                }"
              >
                <el-icon>
                  <Icon name="icon_right-alignment_outlined"
                    ><icon_rightAlignment_outlined class="svg-icon"
                  /></Icon>
                </el-icon>
              </div>
            </el-tooltip>
          </el-radio>
          <el-radio value="custom" v-if="showCustomAlign">
            <el-tooltip effect="dark" placement="top">
              <template #content>
                {{ t('commons.custom') }}
              </template>
              <div
                class="icon-btn"
                :class="{
                  dark: themes === 'dark',
                  active: state.tableCellForm.tableItemAlign === 'custom'
                }"
              >
                <el-icon>
                  <Icon name="icon_custom-alignment_outlined"
                    ><icon_customAlignment_outlined class="svg-icon"
                  /></Icon>
                </el-icon>
              </div>
            </el-tooltip>
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-space>

    <el-row
      v-if="showProperty('tableItemAlign') && state.tableCellForm.tableItemAlign === 'custom'"
    >
      <el-col :span="12">
        <el-select :effect="themes" v-model="alignConfig.id" @change="changeAlignConfig">
          <el-option
            v-for="item in alignConfigOptions"
            :key="item.id"
            :label="item.label"
            :value="item.id"
          />
        </el-select>
      </el-col>
      <el-col :span="12" style="display: flex; align-items: center">
        <el-radio-group
          class="icon-radio-group"
          v-model="alignConfig.align"
          @change="changeTableCell('alignConfig')"
        >
          <el-radio value="left">
            <el-tooltip effect="dark" placement="top">
              <template #content>
                {{ t('chart.text_pos_left') }}
              </template>
              <div
                class="icon-btn"
                :class="{
                  dark: themes === 'dark',
                  active: alignConfig.align === 'left'
                }"
              >
                <el-icon>
                  <Icon name="icon_left-alignment_outlined"
                    ><icon_leftAlignment_outlined class="svg-icon"
                  /></Icon>
                </el-icon>
              </div>
            </el-tooltip>
          </el-radio>
          <el-radio value="center">
            <el-tooltip effect="dark" placement="top">
              <template #content>
                {{ t('chart.text_pos_center') }}
              </template>
              <div
                class="icon-btn"
                :class="{
                  dark: themes === 'dark',
                  active: alignConfig.align === 'center'
                }"
              >
                <el-icon>
                  <Icon name="icon_center-alignment_outlined"
                    ><icon_centerAlignment_outlined class="svg-icon"
                  /></Icon>
                </el-icon>
              </div>
            </el-tooltip>
          </el-radio>
          <el-radio value="right">
            <el-tooltip effect="dark" placement="top">
              <template #content>
                {{ t('chart.text_pos_right') }}
              </template>
              <div
                class="icon-btn"
                :class="{
                  dark: themes === 'dark',
                  active: alignConfig.align === 'right'
                }"
              >
                <el-icon>
                  <Icon name="icon_right-alignment_outlined"
                    ><icon_rightAlignment_outlined class="svg-icon"
                  /></Icon>
                </el-icon>
              </div>
            </el-tooltip>
          </el-radio>
        </el-radio-group>
      </el-col>
    </el-row>
    <el-row :gutter="8">
      <el-col :span="12">
        <el-form-item
          :label="t('visualization.lineHeight')"
          class="form-item"
          :class="'form-item-' + themes"
          v-if="showProperty('tableItemHeight')"
        >
          <el-input-number
            :effect="themes"
            controls-position="right"
            v-model="state.tableCellForm.tableItemHeight"
            :min="20"
            :max="1000"
            @change="changeTableCell('tableItemHeight')"
          />
        </el-form-item>
      </el-col>
    </el-row>
    <el-form-item
      class="form-item"
      :class="'form-item-' + themes"
      v-if="showProperty('tableFreeze')"
    >
      <el-checkbox
        size="small"
        :effect="themes"
        :disabled="hasMerge"
        v-model="state.tableCellForm.tableFreeze"
        @change="changeTableCell('tableFreeze')"
      >
        <span class="data-area-label">
          <span style="margin-right: 4px">{{ t('chart.table_freeze') }}</span>
          <el-tooltip
            class="item"
            effect="dark"
            placement="bottom"
            v-if="hasMerge"
          >
            <template #content>
              <div>{{ t('chart.table_freeze_tip') }}</div>
            </template>
            <el-icon class="hint-icon" :class="{ 'hint-icon--dark': themes === 'dark' }">
              <Icon name="icon_info_outlined"><icon_info_outlined class="svg-icon" /></Icon>
            </el-icon>
          </el-tooltip>
        </span>
      </el-checkbox>
    </el-form-item>
    <el-row :gutter="8" v-if="showProperty('tableFreeze')">
      <el-col :span="12">
        <el-form-item
          :label="t('chart.table_col_freeze_tip')"
          class="form-item"
          :class="'form-item-' + themes"
          v-if="showProperty('tableColumnFreezeHead')"
        >
          <el-input-number
            :effect="themes"
            controls-position="right"
            v-model="state.tableCellForm.tableColumnFreezeHead"
            :disabled="hasMerge || !state.tableCellForm.tableFreeze"
            :min="0"
            :max="100"
            @change="changeTableCell('tableColumnFreezeHead')"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item
          :label="t('chart.table_row_freeze_tip')"
          class="form-item"
          :class="'form-item-' + themes"
          v-if="showProperty('tableRowFreezeHead')"
        >
          <el-input-number
            :effect="themes"
            controls-position="right"
            v-model="state.tableCellForm.tableRowFreezeHead"
            :disabled="hasMerge || !state.tableCellForm.tableFreeze"
            :min="0"
            :max="100"
            @change="changeTableCell('tableRowFreezeHead')"
          />
        </el-form-item>
      </el-col>
    </el-row>
    <el-form-item
      class="form-item"
      :class="'form-item-' + themes"
      v-if="showProperty('mergeCells')"
    >
      <el-checkbox
        size="small"
        :effect="themes"
        :disabled="showProperty('mergeFields') && !mergeFieldOptions.length"
        v-model="state.tableCellForm.mergeCells"
        @change="changeTableCell('mergeCells')"
      >
        <span class="data-area-label">
          <span style="margin-right: 4px">{{ t('chart.merge_cells') }}</span>
          <el-tooltip class="item" effect="dark" placement="bottom">
            <template #content>
              <div>{{ t('chart.merge_cells_tips') }}</div>
            </template>
            <el-icon class="hint-icon" :class="{ 'hint-icon--dark': themes === 'dark' }">
              <Icon name="icon_info_outlined"><icon_info_outlined class="svg-icon" /></Icon>
            </el-icon>
          </el-tooltip>
        </span>
      </el-checkbox>
    </el-form-item>
    <el-form-item
      :label="t('chart.merge_cells_fields')"
      class="form-item"
      :class="'form-item-' + themes"
      v-if="showProperty('mergeFields') && state.tableCellForm.mergeCells"
    >
      <el-select
        :effect="themes"
        v-model="state.tableCellForm.mergeFields"
        multiple
        clearable
        collapse-tags
        :placeholder="t('chart.merge_cells_fields_placeholder')"
        @change="changeTableCell('mergeFields')"
      >
        <el-option
          v-for="item in mergeFieldOptions"
          :key="item.id"
          :label="item.label"
          :value="item.id"
        />
      </el-select>
      <div class="merge-fields-tip" :class="{ 'merge-fields-tip--dark': themes === 'dark' }">
        {{ t('chart.merge_cells_fields_tip') }}
      </div>
    </el-form-item>
    <el-form-item
      class="form-item"
      :class="'form-item-' + themes"
      v-if="showProperty('cellGroups')"
    >
      <el-checkbox
        size="small"
        :effect="themes"
        :model-value="cellGroupsEnabled"
        @change="toggleCellGroups"
      >
        <span class="data-area-label">
          <span style="margin-right: 4px">{{ t('chart.cell_groups') }}</span>
          <el-tooltip class="item" effect="dark" placement="bottom">
            <template #content>
              <div>{{ t('chart.cell_groups_tip') }}</div>
            </template>
            <el-icon class="hint-icon" :class="{ 'hint-icon--dark': themes === 'dark' }">
              <Icon name="icon_info_outlined"><icon_info_outlined class="svg-icon" /></Icon>
            </el-icon>
          </el-tooltip>
        </span>
      </el-checkbox>
    </el-form-item>
    <div
      class="cell-groups-list"
      :class="{ 'cell-groups-list--dark': themes === 'dark' }"
      v-if="showProperty('cellGroups') && cellGroupsEnabled"
    >
      <div
        v-for="(group, index) in state.tableCellForm.cellGroups"
        :key="index"
        class="cell-group-item"
      >
        <el-select
          :effect="themes"
          v-model="group.matchField"
          :placeholder="t('chart.cell_groups_match_field')"
          size="small"
          @change="changeTableCell('cellGroups')"
        >
          <el-option
            v-for="item in cellGroupFieldOptions"
            :key="item.id"
            :label="item.label"
            :value="item.id"
          />
        </el-select>
        <el-select
          :effect="themes"
          v-model="group.matchValue"
          filterable
          allow-create
          default-first-option
          clearable
          :placeholder="t('chart.cell_groups_match_value')"
          size="small"
          @change="changeTableCell('cellGroups')"
        >
          <el-option
            v-for="item in getMatchValueOptions(group.matchField)"
            :key="item.id"
            :label="item.label"
            :value="item.id"
          />
        </el-select>
        <el-select
          :effect="themes"
          v-model="group.startField"
          :placeholder="t('chart.cell_groups_start_field')"
          size="small"
          @change="changeTableCell('cellGroups')"
        >
          <el-option
            v-for="item in cellGroupFieldOptions"
            :key="item.id"
            :label="item.label"
            :value="item.id"
          />
        </el-select>
        <el-select
          :effect="themes"
          v-model="group.endField"
          :placeholder="t('chart.cell_groups_end_field')"
          size="small"
          @change="changeTableCell('cellGroups')"
        >
          <el-option
            v-for="item in cellGroupFieldOptions"
            :key="item.id"
            :label="item.label"
            :value="item.id"
          />
        </el-select>
        <el-input
          :effect="themes"
          v-model="group.text"
          :placeholder="t('chart.cell_groups_text')"
          size="small"
          @change="changeTableCell('cellGroups')"
        />
        <el-button
          :effect="themes"
          size="small"
          text
          class="cell-group-del"
          @click="removeCellGroup(index)"
        >
          {{ t('chart.cell_groups_delete') }}
        </el-button>
      </div>
      <el-button
        :effect="themes"
        size="small"
        text
        class="cell-group-add"
        @click="addCellGroup"
      >
        + {{ t('chart.cell_groups_add') }}
      </el-button>
    </div>
    <el-form-item
      class="form-item"
      :class="'form-item-' + themes"
      v-if="showProperty('showHorizonBorder')"
    >
      <el-checkbox
        size="small"
        :effect="themes"
        v-model="state.tableCellForm.showHorizonBorder"
        @change="changeTableCell('showHorizonBorder')"
      >
        {{ t('chart.table_cell_show_horizon_border') }}
      </el-checkbox>
    </el-form-item>
    <el-form-item
      class="form-item"
      :class="'form-item-' + themes"
      v-if="showProperty('showVerticalBorder')"
    >
      <el-checkbox
        size="small"
        :effect="themes"
        v-model="state.tableCellForm.showVerticalBorder"
        @change="changeTableCell('showVerticalBorder')"
      >
        {{ t('chart.table_cell_show_vertical_border') }}
      </el-checkbox>
    </el-form-item>
  </el-form>
</template>

<style lang="less" scoped>
.icon-btn {
  font-size: 16px;
  line-height: 16px;
  width: 24px;
  height: 24px;
  text-align: center;
  vertical-align: middle;
  border-radius: 6px;
  padding-top: 4px;

  color: #1f2329;

  cursor: pointer;

  &.dark {
    color: #a6a6a6;
    &.active {
      color: var(--ed-color-primary);
      background-color: var(--ed-color-primary-1a, rgba(59, 130, 246, 0.1));
    }
    &:hover {
      background-color: rgba(255, 255, 255, 0.1);
    }
  }

  &.active {
    color: var(--ed-color-primary);
    background-color: var(--ed-color-primary-1a, rgba(59, 130, 246, 0.1));
  }

  &:hover {
    background-color: rgba(31, 35, 41, 0.1);
  }
}
.icon-radio-group {
  :deep(.ed-radio) {
    margin-right: 8px;

    &:last-child {
      margin-right: 0;
    }
  }
  :deep(.ed-radio__input) {
    display: none;
  }
  :deep(.ed-radio__label) {
    padding: 0;
  }
}
.position-divider {
  width: 1px;
  height: 18px;
  margin-bottom: 8px;
  background: rgba(31, 35, 41, 0.15);

  &.position-divider--dark {
    background: rgba(235, 235, 235, 0.15);
  }
}
.icon-checkbox {
  :deep(.ed-checkbox__input) {
    display: none;
  }
  :deep(.ed-checkbox__label) {
    padding: 0;
  }
}

.mobile-style {
  margin-top: 25px;
}
.data-area-label {
  text-align: left;
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: center;
}
.merge-fields-tip {
  margin-top: 6px;
  color: rgba(31, 35, 41, 0.6);
  font-size: 12px;
  line-height: 18px;

  &.merge-fields-tip--dark {
    color: rgba(235, 235, 235, 0.65);
  }
}
.cell-groups-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 4px;
  background: rgba(31, 35, 41, 0.04);

  &.cell-groups-list--dark {
    background: rgba(235, 235, 235, 0.06);
  }

  .cell-group-item {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 6px;
    align-items: center;

    .cell-group-del {
      grid-column: 1 / -1;
      justify-self: end;
    }
  }

  .cell-group-add {
    align-self: flex-start;
  }
}
</style>
