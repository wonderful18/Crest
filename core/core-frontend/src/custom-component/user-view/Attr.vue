<script setup lang="ts">
import CommonAttr from '@/custom-component/common/CommonAttr.vue'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { storeToRefs } from 'pinia'
import { computed, toRefs, watch } from 'vue'
import { snapshotStoreWithOut } from '@/store/modules/data-visualization/snapshot'
import {
  EXCEL_EXPORT_SCOPE,
  defaultViewExcelExportConfig,
  isExportableTableView
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
const { curComponent, canvasViewInfo } = storeToRefs(dvMainStore)

const currentViewInfo = computed(() => canvasViewInfo.value[curComponent.value?.id])
const excelExportShow = computed(() => isExportableTableView(currentViewInfo.value))
const initExcelExportConfig = () => {
  if (!curComponent.value) {
    return
  }
  curComponent.value.excelExport = defaultViewExcelExportConfig(
    currentViewInfo.value?.excelExport || curComponent.value.excelExport
  )
  if (currentViewInfo.value) {
    currentViewInfo.value.excelExport = curComponent.value.excelExport
  }
}

const excelExport = computed(
  () => curComponent.value?.excelExport || defaultViewExcelExportConfig()
)

const onExcelExportChange = () => {
  if (currentViewInfo.value) {
    currentViewInfo.value.excelExport = curComponent.value.excelExport
  }
  snapshotStore.recordSnapshotCache('excelExport')
}

watch(
  () => curComponent.value?.id,
  () => initExcelExportConfig(),
  { immediate: true }
)

watch(
  () => currentViewInfo.value?.excelExport,
  value => {
    if (curComponent.value && value) {
      curComponent.value.excelExport = defaultViewExcelExportConfig(value)
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="attr-list">
    <CommonAttr :themes="themes" :element="curComponent">
      <el-collapse-item
        v-if="excelExportShow"
        :effect="themes"
        title="Excel导出"
        name="excelExport"
        class="common-style-area"
      >
        <el-form-item label="启用导出" :class="'form-item-' + themes">
          <el-switch v-model="excelExport.enabled" @change="onExcelExportChange" />
        </el-form-item>
        <el-form-item label="导出范围" :class="'form-item-' + themes">
          <el-radio-group v-model="excelExport.scope" @change="onExcelExportChange">
            <el-radio :effect="themes" :label="EXCEL_EXPORT_SCOPE.CURRENT_FILTERED">
              当前筛选结果
            </el-radio>
            <el-radio :effect="themes" :label="EXCEL_EXPORT_SCOPE.ALL">全部数据</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-collapse-item>
    </CommonAttr>
  </div>
</template>
