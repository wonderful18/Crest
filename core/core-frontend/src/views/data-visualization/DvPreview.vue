<script setup lang="ts">
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { computed, onBeforeUnmount, onMounted, ref, type CSSProperties } from 'vue'
import CrestPreview from '@/components/data-visualization/canvas/CrestPreview.vue'
import { storeToRefs } from 'pinia'

const dvMainStore = dvMainStoreWithOut()
const { fullscreenFlag } = storeToRefs(dvMainStore)
const crestPreviewRef = ref<InstanceType<typeof CrestPreview> | null>(null)
const crestPreviewOuterRef = ref<HTMLElement | null>(null)
const dataInitState = ref(true)
const keepProportion = ref('heightFirst')
let resizeObserver: ResizeObserver | null = null
const props = defineProps({
  canvasStylePreview: {
    required: true,
    type: Object
  },
  canvasDataPreview: {
    required: true,
    type: Object
  },
  canvasViewInfoPreview: {
    required: true,
    type: Object
  },
  dvInfo: {
    required: true,
    type: Object
  },
  curPreviewGap: {
    required: false,
    type: Number,
    default: 0
  },
  // 联动按钮位置
  showLinkageButton: {
    type: Boolean,
    default: true
  },
  showPosition: {
    required: false,
    type: String,
    default: 'preview'
  },
  // 显示悬浮按钮
  showPopBar: {
    type: Boolean,
    default: false
  },
  downloadStatus: {
    required: false,
    type: Boolean,
    default: false
  }
})

const restore = () => {
  crestPreviewRef.value?.restore()
}
const contentInnerClass = computed(() => {
  //屏幕适配方式 widthFirst=宽度优先(默认) heightFirst=高度优先 full=铺满全屏 keepSize=不缩放
  if (screenAdaptor.value === 'heightFirst') {
    return 'preview-content-inner-height-first'
  } else if (screenAdaptor.value === 'full') {
    return 'preview-content-inner-full'
  } else if (screenAdaptor.value === 'keep') {
    return 'preview-content-inner-size-keep'
  } else {
    return 'preview-content-inner-width-first'
  }
})

const outerStyle = computed<CSSProperties>(() => {
  return {
    flexDirection: props.canvasStylePreview.screenAdaptor === 'heightFirst' ? 'row' : 'column'
  }
})

const screenAdaptor = computed(() => {
  if (props.canvasStylePreview.screenAdaptor === 'keepProportion') {
    return keepProportion.value
  } else {
    return props.canvasStylePreview.screenAdaptor
  }
})

const keepProportionCheck = outerContentRect => {
  const { width, height } = outerContentRect
  const { innerWidth, innerHeight } = crestPreviewRef.value.getPreviewCanvasSize()
  if (width > innerWidth || height < innerHeight) {
    keepProportion.value = 'heightFirst'
  } else {
    keepProportion.value = 'widthFirst'
  }
}

onMounted(() => {
  resizeObserver = new ResizeObserver(entries => {
    for (let entry of entries) {
      // entry.contentRect 包含 width, height, top, left 等属性
      keepProportionCheck(entry.contentRect)
    }
  })

  if (crestPreviewOuterRef.value) {
    resizeObserver.observe(crestPreviewOuterRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
})

defineExpose({
  restore
})
</script>

<template>
  <div
    id="de-preview-content"
    ref="crestPreviewOuterRef"
    :class="{ 'screen-full': fullscreenFlag }"
    :style="outerStyle"
    class="content-outer"
  >
    <div class="content-inner" :class="contentInnerClass">
      <CrestPreview
        ref="crestPreviewRef"
        v-if="canvasStylePreview && dataInitState"
        :component-data="canvasDataPreview"
        :canvas-style-data="canvasStylePreview"
        :canvas-view-info="canvasViewInfoPreview"
        :dv-info="dvInfo"
        :cur-gap="curPreviewGap"
        :show-position="showPosition"
        :download-status="downloadStatus"
        :outer-screen-adaptor="screenAdaptor"
        :show-pop-bar="showPopBar"
        :show-linkage-button="showLinkageButton"
      ></CrestPreview>
    </div>
  </div>
</template>

<style lang="less" scoped>
.content-outer {
  width: 100%;
  height: calc(100vh - 100px);
  background: #f5f6f7;
  display: flex;
  overflow-y: auto;
  align-items: center;
  flex-direction: column;
  justify-content: center; /* 上下居中 */
  ::-webkit-scrollbar {
    display: none;
  }
}
</style>
