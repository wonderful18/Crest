<script setup lang="ts">
import { computed, onMounted, PropType, reactive, watch, ref, nextTick, inject } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { DEFAULT_BASIC_STYLE } from '@/views/chart/components/editor/util/chart'
import { cloneDeep, defaultsDeep, filter, find } from 'lodash-es'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { storeToRefs } from 'pinia'
import CustomAggrEdit from './CustomAggrEdit.vue'
import { v4 as uuidV4 } from 'uuid'
import { ElMessage } from 'element-plus-secondary'
import {
  buildSummaryColumnFieldOptions,
  getSummaryColumnFieldKey,
  normalizeSummaryColumn as normalizeSummaryColumnConfig,
  SUMMARY_COLUMN_VALIDATION,
  validateSummaryColumn
} from './summaryColumnConfig.mjs'

const dvMainStore = dvMainStoreWithOut()
const { batchOptStatus } = storeToRefs(dvMainStore)
const { t } = useI18n()
const props = defineProps({
  chart: {
    type: Object as PropType<ChartObj>,
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
const showProperty = prop => props.propertyInner?.includes(prop)

const state = reactive({
  basicStyleForm: JSON.parse(JSON.stringify(DEFAULT_BASIC_STYLE)) as ChartBasicStyle,
  currentAxis: undefined as string,
  currentAxisSummary: undefined as {
    show: boolean
    field: string
    summary: string
    originName?: string
  },
  currentSummaryColumn: undefined as SummaryColumn,
  editTarget: 'series' as 'series' | 'summaryColumn'
})
type SummaryColumn = {
  id: string
  name: string
  show: boolean
  summary: string
  field?: string
  originName?: string
  position?: string
  format?: {
    decimal?: number
    unit?: string
    percent?: boolean
    thousandSeparator?: boolean
    emptyText?: string
  }
}

const emit = defineEmits(['onBasicStyleChange'])
const SUMMARY_OPTION_KEY = 'key'
const changeBasicStyle = (prop?: string, requestData = false) => {
  emit('onBasicStyleChange', { data: state.basicStyleForm, requestData }, prop)
}
const changeSummaryType = () => {
  if (state.currentAxisSummary.summary === 'custom' && !state.currentAxisSummary.originName) {
    return
  }
  changeBasicStyle('seriesSummary')
}
watch(
  [
    () => props.chart.customAttr.basicStyle.showSummary,
    () => props.chart.xAxis,
    () => props.chart.yAxis
  ],
  () => {
    init()
  },
  {
    deep: true
  }
)

function getAxisList() {
  return props.chart.type === 'table-info'
    ? filter(props.chart.xAxis, axis => [2, 3, 4].includes(axis.fieldType))
    : props.chart.yAxis
}

const computedAxis = computed(() => {
  return getAxisList()
})
const summaryTypes = [
  { [SUMMARY_OPTION_KEY]: 'sum', name: t('chart.sum') },
  { [SUMMARY_OPTION_KEY]: 'avg', name: t('chart.avg') },
  { [SUMMARY_OPTION_KEY]: 'max', name: t('chart.max') },
  { [SUMMARY_OPTION_KEY]: 'min', name: t('chart.min') },
  { [SUMMARY_OPTION_KEY]: 'custom', name: t('commons.custom') }
]
const summaryColumnTypes = [
  ...summaryTypes,
  { [SUMMARY_OPTION_KEY]: 'count', name: t('chart.count') }
]
const summaryColumnValidationMessages = {
  [SUMMARY_COLUMN_VALIDATION.FIELD_REQUIRED]: 'chart.summary_column_field_required',
  [SUMMARY_COLUMN_VALIDATION.FORMULA_REQUIRED]: 'chart.summary_column_formula_required'
}

function onSelectAxis(value) {
  state.currentAxisSummary = find(state.basicStyleForm.seriesSummary, s => s.field === value)
}

const calcEdit = ref()
const editCalcField = ref(false)
const dimension = inject('dimension', () => [])
const quota = inject('quota', () => [])
const getSourceFields = () => [
  ...(props.chart.xAxis || []),
  ...(props.chart.yAxis || []),
  ...dimension(),
  ...quota()
]
const quotaOptions = computed(() => {
  return buildSummaryColumnFieldOptions(getSourceFields())
})
const getSummaryColumnFieldOptions = (column: SummaryColumn) =>
  buildSummaryColumnFieldOptions(getSourceFields(), { includeNonNumeric: column?.summary === 'count' })
const normalizeSummaryColumnField = (column: SummaryColumn) => {
  const options = getSummaryColumnFieldOptions(column)
  if (column.field && options.some(field => field.engineFieldName === column.field)) {
    return
  }
  column.field = options[0]?.engineFieldName
}
const editField = () => {
  state.editTarget = 'series'
  editCalcField.value = true
  nextTick(() => {
    calcEdit.value.initEdit(
      state.currentAxisSummary,
      getAxisList().filter(ele => ele.id !== '-1')
    )
  })
}
const editSummaryColumnField = column => {
  state.currentSummaryColumn = column
  state.editTarget = 'summaryColumn'
  editCalcField.value = true
  nextTick(() => {
    calcEdit.value.initEdit({ ...column, summary: 'custom' }, quotaOptions.value)
  })
}
const closeEditCalc = () => {
  editCalcField.value = false
}
const confirmEditCalc = () => {
  calcEdit.value.setFieldForm()
  const obj = cloneDeep(calcEdit.value.fieldForm)
  if (state.editTarget === 'summaryColumn') {
    state.currentSummaryColumn.originName = obj.originName?.trim?.() || ''
    state.currentSummaryColumn.summary = 'custom'
    if (!validateCurrentSummaryColumn(state.currentSummaryColumn)) {
      return
    }
    closeEditCalc()
    changeSummaryColumns()
    return
  }
  state.currentAxisSummary.originName = obj.originName
  setFieldDefaultValue(state.currentAxisSummary)
  closeEditCalc()
  changeSummaryType()
}
const normalizeSummaryColumn = (column: Partial<SummaryColumn>): SummaryColumn => {
  return normalizeSummaryColumnConfig(column, {
    defaultName: t('chart.summary_column'),
    idFactory: () => `summary_${uuidV4()}`
  }) as SummaryColumn
}
const validateCurrentSummaryColumn = (column: SummaryColumn): boolean => {
  const result = validateSummaryColumn(column)
  if (result.valid) {
    return true
  }
  ElMessage.warning(t(summaryColumnValidationMessages[result.reason]))
  return false
}
const changeSummaryColumns = () => {
  state.basicStyleForm.summaryColumns = state.basicStyleForm.summaryColumns.map(normalizeSummaryColumn)
  const invalidColumn = state.basicStyleForm.summaryColumns.find(
    column => !validateSummaryColumn(column).valid
  )
  if (invalidColumn && !validateCurrentSummaryColumn(invalidColumn)) {
    return
  }
  changeBasicStyle('summaryColumns', true)
}
const changeSummaryColumnType = (column: SummaryColumn) => {
  if (column.summary !== 'custom') {
    column.originName = ''
  }
  normalizeSummaryColumnField(column)
  changeSummaryColumns()
}
const addSummaryColumn = () => {
  if (!quotaOptions.value.length) {
    ElMessage.warning(t('chart.summary_column_no_quota'))
    return
  }
  const quotaField = quotaOptions.value[0]
  state.basicStyleForm.summaryColumns.push(
    normalizeSummaryColumn({
      name: `${t('chart.summary_column')}${state.basicStyleForm.summaryColumns.length + 1}`,
      field: getSummaryColumnFieldKey(quotaField)
    })
  )
  changeSummaryColumns()
}
const removeSummaryColumn = index => {
  state.basicStyleForm.summaryColumns.splice(index, 1)
  changeSummaryColumns()
}
const moveSummaryColumn = (index, offset) => {
  const targetIndex = index + offset
  if (targetIndex < 0 || targetIndex >= state.basicStyleForm.summaryColumns.length) return
  const [item] = state.basicStyleForm.summaryColumns.splice(index, 1)
  state.basicStyleForm.summaryColumns.splice(targetIndex, 0, item)
  changeSummaryColumns()
}
const init = () => {
  const basicStyle = cloneDeep(props.chart.customAttr.basicStyle)

  state.basicStyleForm = defaultsDeep(basicStyle, cloneDeep(DEFAULT_BASIC_STYLE)) as ChartBasicStyle

  const axisList = getAxisList()

  const tempList = []
  for (let i = 0; i < axisList.length; i++) {
    const axis = axisList[i]
    let savedAxis = find(state.basicStyleForm.seriesSummary, s => s.field === axis.engineFieldName)
    if (savedAxis) {
      if (savedAxis.summary == undefined) {
        savedAxis.summary = 'sum'
      }
      if (savedAxis.show == undefined) {
        savedAxis.show = true
      }
    } else {
      savedAxis = {
        field: axis.engineFieldName,
        summary: 'sum',
        show: true
      }
    }
    tempList.push(savedAxis)
  }

  state.basicStyleForm.seriesSummary = tempList
  state.basicStyleForm.summaryColumns = (state.basicStyleForm.summaryColumns || []).map(
    normalizeSummaryColumn
  )

  if (state.basicStyleForm.seriesSummary.length > 0 && state.basicStyleForm.showSummary) {
    state.currentAxis = state.basicStyleForm.seriesSummary[0].field
    onSelectAxis(state.currentAxis)
  } else {
    state.currentAxis = undefined
    state.currentAxisSummary = undefined
  }
}
const setFieldDefaultValue = field => {
  field.extField = 2
  field.chartId = props.chart.id
  field.datasetGroupId = props.chart.tableId
  field.lastSyncTime = null
  field.columnIndex = dimension().length + quota().length
  field.extractedFieldType = field.fieldType
}
onMounted(() => {
  init()
})
</script>

<template>
  <div class="summary-selector" :class="'summary-selector-' + themes">
    <el-form
      ref="summaryForm"
      :disabled="!state.basicStyleForm.showSummary"
      :model="state.basicStyleForm"
      size="small"
      label-position="top"
    >
      <el-form-item
        v-if="showProperty('summaryLabel')"
        :label="t('chart.table_summary_label')"
        :class="'form-item-' + themes"
        class="form-item"
      >
        <el-input
          v-model="state.basicStyleForm.summaryLabel"
          type="text"
          :effect="themes"
          :max-length="10"
          @blur="changeBasicStyle('summaryLabel')"
        />
      </el-form-item>

      <el-form-item v-if="!batchOptStatus" class="form-item" :class="'form-item-' + themes">
        <el-select
          v-model="state.currentAxis"
          :class="'form-item-' + themes"
          class="form-item"
          :effect="themes"
          @change="onSelectAxis"
        >
          <el-option
            v-for="c in computedAxis"
            :key="c.engineFieldName"
            :value="c.engineFieldName"
            :label="c.chartShowName ?? c.name"
          />
        </el-select>
      </el-form-item>

      <template v-if="state.currentAxis && state.currentAxisSummary">
        <el-form-item class="form-item" :class="'form-item-' + themes">
          <el-checkbox
            size="small"
            :effect="themes"
            v-model="state.currentAxisSummary.show"
            @change="changeBasicStyle('seriesSummary')"
          >
            {{ t('chart.table_show_summary') }}
          </el-checkbox>
        </el-form-item>

        <div class="indented-container">
          <el-form-item class="form-item" :class="'form-item-' + themes">
            <el-col :span="state.currentAxisSummary.show ? 19 : 22" :offset="2">
              <el-select
                v-model="state.currentAxisSummary.summary"
                :class="'form-item-' + themes"
                class="form-item"
                :effect="themes"
                :disabled="!state.currentAxisSummary.show"
                @change="changeSummaryType"
              >
                <el-option v-for="c in summaryTypes" :key="c.key" :value="c.key" :label="c.name" />
              </el-select>
            </el-col>
            <el-col v-if="state.currentAxisSummary.show" :span="2" :offset="1">
              <el-icon style="cursor: pointer">
                <Setting @click="editField()" />
              </el-icon>
            </el-col>
          </el-form-item>
        </div>
      </template>

      <div class="summary-column-header">
        <span>{{ t('chart.summary_column') }}</span>
        <el-button text size="small" @click="addSummaryColumn">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
      <div
        v-for="(column, index) in state.basicStyleForm.summaryColumns"
        :key="column.id"
        class="summary-column-item"
      >
        <div class="summary-column-row">
          <el-checkbox :effect="themes" v-model="column.show" @change="changeSummaryColumns" />
          <el-input
            v-model="column.name"
            :effect="themes"
            :placeholder="t('chart.summary_column_name')"
            @blur="changeSummaryColumns"
          />
        </div>
        <div class="summary-column-row">
          <el-select v-model="column.summary" :effect="themes" @change="changeSummaryColumnType(column)">
            <el-option
              v-for="c in summaryColumnTypes"
              :key="c.key"
              :value="c.key"
              :label="c.name"
            />
          </el-select>
          <el-select
            v-model="column.field"
            :effect="themes"
            :placeholder="t('chart.summary_column_field')"
            @change="changeSummaryColumns"
          >
            <el-option
              v-for="field in getSummaryColumnFieldOptions(column)"
              :key="field.engineFieldName"
              :value="field.engineFieldName"
              :label="field.chartShowName ?? field.name"
            />
          </el-select>
        </div>
        <div class="summary-column-row">
          <el-input-number
            v-model="column.format.decimal"
            :min="0"
            :max="8"
            controls-position="right"
            @change="changeSummaryColumns"
          />
          <el-input
            v-model="column.format.unit"
            :effect="themes"
            :placeholder="t('chart.summary_column_unit')"
            @blur="changeSummaryColumns"
          />
        </div>
        <div class="summary-column-row summary-column-actions">
          <el-checkbox :effect="themes" v-model="column.format.percent" @change="changeSummaryColumns">
            {{ t('chart.value_formatter_percent') }}
          </el-checkbox>
          <el-checkbox
            :effect="themes"
            v-model="column.format.thousandSeparator"
            @change="changeSummaryColumns"
          >
            {{ t('chart.value_formatter_thousand_separator') }}
          </el-checkbox>
        </div>
        <div class="summary-column-row summary-column-actions">
          <el-input
            v-model="column.format.emptyText"
            :effect="themes"
            :placeholder="t('chart.summary_column_empty_text')"
            @blur="changeSummaryColumns"
          />
          <el-icon :title="t('commons.custom')" @click="editSummaryColumnField(column)"><Setting /></el-icon>
          <el-icon @click="moveSummaryColumn(index, -1)"><ArrowUp /></el-icon>
          <el-icon @click="moveSummaryColumn(index, 1)"><ArrowDown /></el-icon>
          <el-icon @click="removeSummaryColumn(index)"><Delete /></el-icon>
        </div>
      </div>
    </el-form>
  </div>
  <!--图表计算字段-->
  <el-dialog
    v-model="editCalcField"
    width="1000px"
    title="自定义总计"
    :close-on-click-modal="false"
  >
    <custom-aggr-edit ref="calcEdit" />
    <template #footer>
      <el-button secondary @click="closeEditCalc()">{{ t('dataset.cancel') }} </el-button>
      <el-button type="primary" @click="confirmEditCalc()">{{ t('dataset.confirm') }} </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="less">
.summary-selector {
  width: 100%;
}

.indented-container {
  margin-top: 8px;
  width: 100%;
  padding-left: 22px;
}

.summary-column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0 8px;
  color: #1f2329;
  font-size: 13px;
}

.summary-column-item {
  padding: 8px 0;
  border-top: 1px solid var(--crestCardStrokeColor, #dee0e3);
}

.summary-column-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  min-width: 0;

  .ed-select,
  .ed-input,
  .ed-input-number {
    flex: 1;
    min-width: 0;
  }
}

.summary-column-actions {
  flex-wrap: wrap;
  row-gap: 4px;

  :deep(.ed-checkbox) {
    margin-right: 0;
  }

  .ed-icon {
    cursor: pointer;
    color: #646a73;
  }
}

.summary-selector-dark {
  .summary-column-header {
    color: #c9d1d9;
  }

  .summary-column-item {
    border-top-color: rgba(255, 255, 255, 0.16);
  }

  .summary-column-actions {
    :deep(.ed-checkbox__label) {
      color: #c9d1d9;
    }

    .ed-icon {
      color: #a8b1bd;
    }
  }
}
</style>
