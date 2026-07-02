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

const layoutOptions: Array<{ label: string; value: ChartGroupLayoutMode }> = [
  { label: t('visualization.chart_group_layout_keep'), value: 'keep' },
  { label: t('visualization.chart_group_layout_horizontal'), value: 'horizontal' },
  { label: t('visualization.chart_group_layout_vertical'), value: 'vertical' },
  { label: t('visualization.chart_group_layout_grid'), value: 'grid' }
]

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
    ElMessage.warning(t('visualization.chart_group_create_failed'))
    return
  }
  recordSnapshot('createChartGroup', true)
}

const createSelectedGroup = () => {
  if (selectedComponents.value.length < 2) {
    ElMessage.warning(t('visualization.chart_group_select_more'))
    return
  }
  const ok = composeStore.createChartGroupFromComponents(selectedComponents.value)
  if (!ok) {
    ElMessage.warning(t('visualization.chart_group_create_failed'))
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
    ElMessage.warning(t('visualization.chart_group_parent_missing'))
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
    ElMessage.warning(t('visualization.chart_group_grid_unavailable'))
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
    <el-collapse-item :effect="themes" :title="t('visualization.chart_group_container')" name="chartGroup">
      <template v-if="batch">
        <div class="chart-group-status">
          {{ t('visualization.chart_group_selected_count', { count: selectedComponents.length }) }}
        </div>
        <el-button
          size="small"
          type="primary"
          plain
          :disabled="selectedComponents.length < 2"
          @click="createSelectedGroup"
        >
          {{ t('visualization.chart_group_create_selected') }}
        </el-button>
      </template>

      <template v-else-if="isGroup && groupConfig">
        <div class="chart-group-field">
          <span>{{ t('visualization.chart_group_name') }}</span>
          <el-input size="small" v-model="groupName" @change="onGroupNameChange" />
        </div>
        <div class="chart-group-field">
          <span>{{ t('visualization.chart_group_layout_mode') }}</span>
          <el-select size="small" v-model="layoutMode">
            <el-option
              v-for="option in layoutOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>
        <div class="chart-group-grid">
          <div class="chart-group-field">
            <span>{{ t('visualization.chart_group_horizontal_gap') }}</span>
            <el-input-number
              size="small"
              v-model="horizontalGap"
              :min="0"
              controls-position="right"
            />
          </div>
          <div class="chart-group-field">
            <span>{{ t('visualization.chart_group_vertical_gap') }}</span>
            <el-input-number
              size="small"
              v-model="verticalGap"
              :min="0"
              controls-position="right"
            />
          </div>
        </div>
        <el-checkbox
          v-model="clipOverflow"
          @change="() => recordSnapshot('chartGroupClipOverflow', true)"
        >
          {{ t('visualization.chart_group_clip_overflow') }}
        </el-checkbox>
        <div class="chart-group-actions">
          <el-button size="small" type="primary" plain @click="applyLayout">
            {{ t('visualization.chart_group_apply_layout') }}
          </el-button>
          <el-button size="small" plain @click="cancelGroup">
            {{ t('visualization.chart_group_cancel') }}
          </el-button>
        </div>
      </template>

      <template v-else-if="parentInfo">
        <div class="chart-group-status">
          {{ t('visualization.chart_group_joined', { name: parentInfo.group.name }) }}
        </div>
        <div class="chart-group-actions">
          <el-button size="small" type="primary" plain @click="editParentGroup">
            {{ t('visualization.chart_group_edit') }}
          </el-button>
          <el-button size="small" plain @click="removeFromGroup">
            {{ t('visualization.chart_group_remove') }}
          </el-button>
        </div>
      </template>

      <template v-else-if="canCreateCurrent">
        <div class="chart-group-status">{{ t('visualization.chart_group_not_joined') }}</div>
        <el-button size="small" type="primary" plain @click="createCurrentGroup">
          {{ t('visualization.chart_group_create') }}
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
    height: 34px;
    line-height: 34px;
    padding-left: 6px;
    font-size: 12px;
    font-weight: 400;
  }

  :deep(.ed-collapse-item__content) {
    padding: 12px 16px;
  }
}

.chart-group-status {
  margin-bottom: 10px;
  color: #9ea6b2;
  font-size: 12px;
  line-height: 18px;
}

.chart-group-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
  color: #9ea6b2;
  font-size: 12px;
}

.chart-group-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.chart-group-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}
</style>
