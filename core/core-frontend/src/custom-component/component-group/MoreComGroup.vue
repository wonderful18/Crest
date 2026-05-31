<script setup lang="ts">
import dbMoreWeb from '@/assets/svg/db-more-web.svg'
import { toRefs } from 'vue'
import eventBus from '@/utils/eventBus'
import DragComponent from '@/custom-component/component-group/DragComponent.vue'
import { commonHandleDragEnd, commonHandleDragStart } from '@/utils/canvasUtils'
import { useI18n } from '@/hooks/web/useI18n'
import dvTabScreen from '@/assets/svg/dv-tab-screen.svg'
const { t } = useI18n()
const props = defineProps({
  propValue: {
    type: Array,
    default: () => []
  },
  dvModel: {
    type: String,
    default: 'dv'
  },
  element: {
    type: Object,
    default() {
      return {
        propValue: null
      }
    }
  },
  themes: {
    type: String,
    default: 'dark'
  }
})

const { dvModel } = toRefs(props)
const newComponent = params => {
  eventBus.emit('handleNew', { componentName: params, innerType: params })
}

const handleDragStart = e => {
  commonHandleDragStart(e, dvModel.value)
}

const handleDragEnd = e => {
  commonHandleDragEnd(e, dvModel.value)
}
</script>

<template>
  <div class="group" @dragstart="handleDragStart" @dragend="handleDragEnd">
    <drag-component
      :themes="themes"
      name="YYYY-MM-DD 08:00:00"
      :label="t('visualization.date_time')"
      drag-info="TimeClock&TimeClock"
      v-on:click="newComponent('TimeClock')"
    ></drag-component>
    <drag-component
      :themes="themes"
      :icon="dbMoreWeb"
      :label="t('visualization.web')"
      drag-info="Frame&Frame"
      v-on:click="newComponent('Frame')"
    ></drag-component>
    <!--    <drag-component-->
    <!--      :themes="themes"-->
    <!--      :icon="dvTabScreen"-->
    <!--      :label="$t('visualization.screen_page')"-->
    <!--      drag-info="Screen&Screen"-->
    <!--      v-on:click="newComponent('Screen', 'Screen')"-->
    <!--    ></drag-component>-->
  </div>
</template>

<style lang="less" scoped>
.group {
  padding: 12px 8px;
  display: inline-flex;
}
</style>
