<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus-secondary'
import { useI18n } from '@/hooks/web/useI18n'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { composeStoreWithOut } from '@/store/modules/data-visualization/compose'
import { snapshotStoreWithOut } from '@/store/modules/data-visualization/snapshot'
import {
  applyChartGroupLayout,
  findChartGroupParent,
  normalizeChartGroupContainer,
  type ChartGroupLayoutMode
} from '@/utils/chartGroupContainer'

const props = withDefaults(
  defineProps<{
    element?: any
    themes?: EditorTheme
    batch?: boolean
  }>(),
  {
    themes: 'dark',
    batch: false
  }
)

const { t } = useI18n()
const activeName = ref('chartGroup')
const dvMainStore = dvMainStoreWithOut()
const composeStore = composeStoreWithOut()
const snapshotStore = snapshotStoreWithOut()
const { componentData } = storeToRefs(dvMainStore)
const { areaData } = storeToRefs(composeStore)

const tr = (key: string, fallback: string, named?: Record<string, string | number>) => {
  const value = named ? t(key, named) : t(key)
  if (value && value !== key) {
    return value
  }
  return Object.entries(named || {}).reduce(
    (text, [name, val]) => text.replace(`{${name}}`, String(val)),
    fallback
  )
}

const layoutOptions = computed<Array<{ label: string; value: ChartGroupLayoutMode }>>(() => [
  { label: tr('visualization.chart_group_layout_keep', '保持位置'), value: 'keep' },
  { label: tr('visualization.chart_group_layout_horizontal', '横向贴合'), value: 'horizontal' },
  { label: tr('visualization.chart_group_layout_vertical', '纵向贴合'), value: 'vertical' },
  { label: tr('visualization.chart_group_layout_grid', '网格贴合'), value: 'grid' }
])

const selectedComponents = computed(() =>
  (areaData.value.components || []).filter(component => component.component !== 'GroupArea')
)
const currentElement = computed(() => props.element)
const isGroup = computed(() => currentElement.value?.component === 'Group')
const parentInfo = computed(() => {
  if (!currentElement.value || isGroup.value) {
    return null
  }
  return findChartGroupParent(componentData.value, currentElement.value.id)
})
const canCreateCurrent = computed(
  () => currentElement.value && !props.batch && !isGroup.value && !parentInfo.value
)
const groupConfig = computed(() =>
  isGroup.value ? normalizeChartGroupContainer(currentElement.value) : null
)

const groupName = computed({
  get: () => groupConfig.value?.name || '',
  set: value => {
    const group = groupConfig.value
    if (!group) {
      return
    }
    group.name = value
    group.label = value
  }
})

const layoutMode = computed({
  get: () => groupConfig.value?.chartGroupContainer.layoutMode || 'keep',
  set: (value: ChartGroupLayoutMode) => {
    const group = groupConfig.value
    if (!group) {
      return
    }
    group.chartGroupContainer.layoutMode = value
  }
})

const horizontalGap = computed({
  get: () => groupConfig.value?.chartGroupContainer.horizontalGap || 0,
  set: value => {
    const group = groupConfig.value
    if (!group) {
      return
    }
    group.chartGroupContainer.horizontalGap = Number(value) || 0
  }
})

const verticalGap = computed({
  get: () => groupConfig.value?.chartGroupContainer.verticalGap || 0,
  set: value => {
    const group = groupConfig.value
    if (!group) {
      return
    }
    group.chartGroupContainer.verticalGap = Number(value) || 0
  }
})

const clipOverflow = computed({
  get: () => groupConfig.value?.chartGroupContainer.clipOverflow !== false,
  set: value => {
    const group = groupConfig.value
    if (!group) {
      return
    }
    group.chartGroupContainer.clipOverflow = value
  }
})

const recordSnapshot = (name: string, immediate = false) => {
  snapshotStore.recordSnapshotCacheToMobile(name)
  if (immediate) {
    snapshotStore.recordSnapshot()
  }
}

const createCurrentGroup = () => {
  const element = currentElement.value
  if (!element) {
    return
  }
  const ok = composeStore.createChartGroupFromComponents(
    [element],
    element.canvasId || 'canvas-main'
  )
  if (!ok) {
    ElMessage.warning(tr('visualization.chart_group_create_failed', '当前组件无法创建图表组'))
    return
  }
  recordSnapshot('createChartGroup', true)
}

const createSelectedGroup = () => {
  if (selectedComponents.value.length < 2) {
    ElMessage.warning(tr('visualization.chart_group_select_more', '请选择至少两个组件'))
    return
  }
  const ok = composeStore.createChartGroupFromComponents(selectedComponents.value)
  if (!ok) {
    ElMessage.warning(tr('visualization.chart_group_create_failed', '当前组件无法创建图表组'))
    return
  }
  dvMainStore.setBatchOptStatus(false)
  recordSnapshot('createSelectedChartGroup', true)
}

const editParentGroup = () => {
  if (!parentInfo.value) {
    return
  }
  dvMainStore.setCurComponent({
    component: parentInfo.value.group,
    index: parentInfo.value.groupIndex
  })
}

const removeFromGroup = () => {
  const element = currentElement.value
  if (!element) {
    return
  }
  const ok = composeStore.removeComponentFromChartGroup(element.id)
  if (!ok) {
    ElMessage.warning(tr('visualization.chart_group_parent_missing', '未找到所属图表组'))
    return
  }
  recordSnapshot('removeFromChartGroup', true)
}

const cancelGroup = () => {
  if (!isGroup.value) {
    return
  }
  composeStore.decompose()
  recordSnapshot('cancelChartGroup', true)
}

const applyLayout = () => {
  const group = groupConfig.value
  if (!group) {
    return
  }
  const ok = applyChartGroupLayout(group, layoutMode.value, {
    horizontalGap: horizontalGap.value,
    verticalGap: verticalGap.value
  })
  if (!ok) {
    ElMessage.warning(tr('visualization.chart_group_grid_unavailable', '当前图表组无法识别为稳定网格'))
    return
  }
  recordSnapshot('applyChartGroupLayout', true)
}

const onGroupNameChange = () => {
  recordSnapshot('chartGroupName', true)
}

watch(
  () => props.element,
  component => {
    if (component?.component === 'Group') {
      normalizeChartGroupContainer(component)
    }
  },
  { immediate: true }
)
</script>

<template>
  <el-collapse v-model="activeName" class="chart-group-setting" :class="`theme-${themes}`">
    <el-collapse-item
      :effect="themes"
      :title="tr('visualization.chart_group_container', '图表组')"
      name="chartGroup"
    >
      <template v-if="batch">
        <div class="chart-group-status">
          {{
            tr('visualization.chart_group_selected_count', '已选择 {count} 个组件', {
              count: selectedComponents.length
            })
          }}
        </div>
        <el-button
          size="small"
          type="primary"
          :disabled="selectedComponents.length < 2"
          @click="createSelectedGroup"
        >
          {{ tr('visualization.chart_group_create_selected', '用选中图表创建图表组') }}
        </el-button>
      </template>

      <template v-else-if="isGroup && groupConfig">
        <el-form class="chart-group-form" label-position="top">
          <el-form-item :label="tr('visualization.chart_group_name', '图表组名称')">
            <el-input size="small" v-model="groupName" @change="onGroupNameChange" />
          </el-form-item>
          <el-form-item :label="tr('visualization.chart_group_layout_mode', '布局模式')">
            <el-select size="small" v-model="layoutMode">
              <el-option
                v-for="option in layoutOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <div class="chart-group-grid">
            <el-form-item :label="tr('visualization.chart_group_horizontal_gap', '横向间距')">
              <el-input-number
                size="small"
                v-model="horizontalGap"
                :min="0"
                controls-position="right"
              />
            </el-form-item>
            <el-form-item :label="tr('visualization.chart_group_vertical_gap', '纵向间距')">
              <el-input-number
                size="small"
                v-model="verticalGap"
                :min="0"
                controls-position="right"
              />
            </el-form-item>
          </div>
        </el-form>
        <el-checkbox
          v-model="clipOverflow"
          @change="() => recordSnapshot('chartGroupClipOverflow', true)"
        >
          {{ tr('visualization.chart_group_clip_overflow', '裁剪溢出') }}
        </el-checkbox>
        <div class="chart-group-actions">
          <el-button size="small" type="primary" @click="applyLayout">
            {{ tr('visualization.chart_group_apply_layout', '应用布局') }}
          </el-button>
          <el-button size="small" @click="cancelGroup">
            {{ tr('visualization.chart_group_cancel', '取消图表组') }}
          </el-button>
        </div>
      </template>

      <template v-else-if="parentInfo">
        <div class="chart-group-status">
          {{
            tr('visualization.chart_group_joined', '已加入图表组：{name}', {
              name: parentInfo.group.name
            })
          }}
        </div>
        <div class="chart-group-actions">
          <el-button size="small" type="primary" @click="editParentGroup">
            {{ tr('visualization.chart_group_edit', '编辑图表组') }}
          </el-button>
          <el-button size="small" @click="removeFromGroup">
            {{ tr('visualization.chart_group_remove', '从图表组移出') }}
          </el-button>
        </div>
      </template>

      <template v-else-if="canCreateCurrent">
        <div class="chart-group-status">
          {{ tr('visualization.chart_group_not_joined', '未加入图表组') }}
        </div>
        <el-button size="small" type="primary" @click="createCurrentGroup">
          {{ tr('visualization.chart_group_create', '创建图表组') }}
        </el-button>
      </template>
    </el-collapse-item>
  </el-collapse>
</template>

<style scoped lang="less">
.chart-group-setting {
  border-top: 0;
  border-bottom: 0;

  :deep(.ed-collapse-item__header) {
    height: 36px !important;
    line-height: 36px !important;
    padding: 0 !important;
    font-size: 12px !important;
    font-weight: 500 !important;
  }

  :deep(.ed-collapse-item__content) {
    padding: 16px 10px 0 !important;
    border: none;
  }

  :deep(.ed-form-item) {
    display: block;
    margin-bottom: 8px;
  }

  :deep(.ed-form-item__label) {
    display: block;
    justify-content: flex-start;
    height: auto;
    padding: 0 0 4px;
    color: #646a73;
    font-size: 12px;
    line-height: 20px;
  }

  :deep(.ed-input),
  :deep(.ed-select),
  :deep(.ed-input-number) {
    width: 100%;
    --ed-input-height: 28px;
  }

  :deep(.ed-input__wrapper),
  :deep(.ed-select__wrapper) {
    min-height: 28px;
  }

  :deep(.ed-input__inner) {
    font-size: 12px;
    font-weight: 400;
  }

  :deep(.ed-input-number .ed-input__inner) {
    text-align: left;
  }

  :deep(.ed-input-number__decrease),
  :deep(.ed-input-number__increase) {
    --ed-input-number-controls-height: 13px;
  }

  :deep(.ed-checkbox) {
    height: 20px;
    margin-top: 2px;

    .ed-checkbox__label {
      font-size: 12px;
      line-height: 20px;
    }
  }

  :deep(.ed-button) {
    height: 28px;
    margin-left: 0;
    padding: 0 8px;
    font-size: 12px;
    border-radius: 2px;
  }

  :deep(.ed-button + .ed-button) {
    margin-left: 0;
  }

  &.theme-dark {
    :deep(.ed-form-item__label),
    :deep(.ed-checkbox__label) {
      color: #a6a6a6;
    }

    :deep(.ed-input__wrapper),
    :deep(.ed-select__wrapper) {
      background-color: #373737;
      box-shadow: 0 0 0 1px #505050 inset;
    }

    :deep(.ed-input__inner),
    :deep(.ed-select__placeholder),
    :deep(.ed-select__selected-item) {
      color: #ebebeb;
    }

    :deep(.ed-input-number__decrease),
    :deep(.ed-input-number__increase) {
      background-color: #373737;
      border-color: #505050;
      color: #a6a6a6;
    }

    :deep(.ed-button:not(.ed-button--primary)) {
      background-color: #373737;
      border-color: #505050;
      color: #a6a6a6;
    }
  }

  &.theme-light {
    :deep(.ed-form-item__label) {
      color: #646a73;
    }
  }
}

.chart-group-status {
  margin-bottom: 10px;
  color: #a6a6a6;
  font-size: 12px;
  line-height: 18px;
}

.chart-group-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
}

.chart-group-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}
</style>
