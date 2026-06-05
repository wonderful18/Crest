<script setup lang="ts">
import { computed, toRefs, watch } from 'vue'
import CommonAttr from '@/custom-component/common/CommonAttr.vue'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { storeToRefs } from 'pinia'
import { snapshotStoreWithOut } from '@/store/modules/data-visualization/snapshot'
import {
  EXCEL_EXPORT_CONTENT,
  EXCEL_EXPORT_SCOPE,
  collectExportableTableViews,
  defaultExportButtonConfig,
  isPivotTableView
} from '@/utils/visualization/filteredExcelExport.mjs'

const props = defineProps({
  themes: {
    type: String,
    default: 'dark'
  }
})

const { themes } = toRefs(props)
const dvMainStore = dvMainStoreWithOut()
const snapshotStore = snapshotStoreWithOut()
const { curComponent, componentData, canvasViewInfo } = storeToRefs(dvMainStore)

const initExportButtonConfig = () => {
  if (!curComponent.value) {
    return
  }
  curComponent.value.exportButton = defaultExportButtonConfig(curComponent.value.exportButton)
}

const exportButton = computed(() => curComponent.value?.exportButton || defaultExportButtonConfig())

const tableOptions = computed(() => collectExportableTableViews(componentData.value, canvasViewInfo.value))
const selectedView = computed(() =>
  tableOptions.value.find(item => item.id === String(exportButton.value.targetViewId))?.viewInfo
)
const formattedExportDisabled = computed(() => !isPivotTableView(selectedView.value))

const onExportButtonChange = () => {
  if (formattedExportDisabled.value && exportButton.value.content === EXCEL_EXPORT_CONTENT.FORMATTED) {
    exportButton.value.content = EXCEL_EXPORT_CONTENT.VIEW
  }
  snapshotStore.recordSnapshotCache('exportButton')
}

watch(
  () => curComponent.value?.id,
  () => initExportButtonConfig(),
  { immediate: true }
)

watch(
  tableOptions,
  options => {
    if (!exportButton.value.targetViewId && options.length === 1) {
      exportButton.value.targetViewId = options[0].id
      snapshotStore.recordSnapshotCache('exportButton-target-default')
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="attr-list">
    <CommonAttr :themes="themes" :element="curComponent">
      <el-collapse-item
        :effect="themes"
        title="Excel导出"
        name="excelExport"
        class="common-style-area"
      >
        <el-form-item label="按钮文案" :class="'form-item-' + themes">
          <el-input v-model="exportButton.text" @change="onExportButtonChange" />
        </el-form-item>
        <el-form-item label="目标表格" :class="'form-item-' + themes">
          <el-select
            v-model="exportButton.targetViewId"
            placeholder="请选择要导出的表格"
            filterable
            @change="onExportButtonChange"
          >
            <el-option
              v-for="item in tableOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="导出范围" :class="'form-item-' + themes">
          <el-radio-group v-model="exportButton.scope" @change="onExportButtonChange">
            <el-radio :effect="themes" :label="EXCEL_EXPORT_SCOPE.CURRENT_FILTERED">
              当前筛选结果
            </el-radio>
            <el-radio :effect="themes" :label="EXCEL_EXPORT_SCOPE.ALL">全部数据</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="导出内容" :class="'form-item-' + themes">
          <el-radio-group v-model="exportButton.content" @change="onExportButtonChange">
            <el-radio :effect="themes" :label="EXCEL_EXPORT_CONTENT.VIEW">展示列表数据</el-radio>
            <el-radio
              :effect="themes"
              :disabled="formattedExportDisabled"
              :label="EXCEL_EXPORT_CONTENT.FORMATTED"
            >
              透视表带格式
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-collapse-item>
    </CommonAttr>
  </div>
</template>
