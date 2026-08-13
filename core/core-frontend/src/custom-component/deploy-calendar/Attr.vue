<script setup lang="ts">
import CommonAttr from '@/custom-component/common/CommonAttr.vue'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { storeToRefs } from 'pinia'
import { PropType } from 'vue'

defineProps({
  themes: {
    type: String as PropType<EditorTheme>,
    default: 'dark'
  }
})

const dvMainStore = dvMainStoreWithOut()
const { curComponent, canvasViewInfo, mobileInPc, batchOptStatus } = storeToRefs(dvMainStore)
</script>

<template>
  <div class="attr-list de-collapse-style">
    <CommonAttr
      :themes="themes"
      :element="curComponent"
      :background-color-picker-width="197"
      :background-border-select-width="197"
    />
  </div>
</template>

<style lang="less" scoped>
.attr-list {
  height: 100%;
  overflow-y: auto;
}
</style>
