<script lang="ts" setup>
import { computed, toRefs } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { exportFilteredExcel } from '@/utils/visualization/filteredExcelExportService'
import { defaultExportButtonConfig } from '@/utils/visualization/filteredExcelExport.mjs'

defineOptions({
  inheritAttrs: false
})

const props = defineProps({
  element: {
    type: Object,
    default() {
      return {
        exportButton: null,
        style: {}
      }
    }
  },
  showPosition: {
    type: String,
    required: false,
    default: 'canvas'
  }
})

const { element, showPosition } = toRefs(props)
const exportButton = computed(() => defaultExportButtonConfig(element.value?.exportButton))
const isPreview = computed(() => showPosition.value.includes('preview'))
const buttonStyle = computed(() => {
  const style = element.value?.style || {}
  return {
    color: style.color || '#ffffff',
    backgroundColor: style.backgroundColor || '#3B82F6',
    fontSize: (style.fontSize || 14) + 'px',
    fontWeight: style.fontWeight || 400,
    borderColor: style.borderActive ? style.borderColor : 'transparent',
    borderWidth: style.borderActive ? style.borderWidth + 'px' : '0',
    borderStyle: style.borderActive ? style.borderStyle : 'solid',
    borderRadius: (style.borderRadius || 4) + 'px',
    cursor: isPreview.value ? 'pointer' : 'default'
  }
})

const handleClick = () => {
  if (!isPreview.value) {
    return
  }
  exportFilteredExcel({
    targetViewId: exportButton.value.targetViewId,
    scope: exportButton.value.scope,
    content: exportButton.value.content
  })
}
</script>

<template>
  <button class="excel-export-button component" :style="buttonStyle" @click.stop="handleClick">
    <el-icon v-if="element?.loading" class="button-icon"><Loading /></el-icon>
    <span>{{ exportButton.text }}</span>
  </button>
</template>

<style lang="less" scoped>
.excel-export-button {
  width: 100%;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
  box-sizing: border-box;
  outline: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    filter: brightness(1.05);
  }

  &:active {
    filter: brightness(0.95);
  }
}

.button-icon {
  font-size: 14px;
}
</style>
