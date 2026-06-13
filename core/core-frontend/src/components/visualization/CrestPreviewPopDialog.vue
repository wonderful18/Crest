<template>
  <el-dialog
    ref="previewPopDialog"
    modal-class="preview_pop_custom"
    :append-to-body="true"
    :fullscreen="state.fullscreen"
    v-model="state.dialogShow"
    :style="dialogStyle"
    :modal="false"
    :width="state.width"
  >
    <template #header>
      <div class="preview-pop-header">
        <span class="preview-pop-title">{{ state.name }}</span>
        <button class="preview-pop-download" type="button" @click="downloadData">
          <Icon name="dv-preview-download" className="preview-pop-download-icon">
            <dvPreviewDownload class="svg-icon" />
          </Icon>
          <span>下载数据</span>
        </button>
      </div>
    </template>
    <div v-if="state.url" class="preview-main-frame-outer">
      <iframe
        v-if="state.dialogShow"
        ref="previewFrameRef"
        class="preview-main-frame"
        id="iframe-de-preview-pop"
        :src="state.url.startsWith('#/') ? `?${new Date().getTime()}${state.url}` : state.url"
        scrolling="auto"
        frameborder="0"
      />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useEmbedded } from '@/store/modules/embedded'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import { storeToRefs } from 'pinia'
import ChartCarouselTooltip from '@/views/chart/components/js/g2plot_tooltip_carousel'
import dvPreviewDownload from '@/assets/svg/icon_download_outlined.svg'
const dvMainStore = dvMainStoreWithOut()
const { canvasStyleData } = storeToRefs(dvMainStore)
const state = reactive({
  dialogShow: false,
  name: '',
  fullscreen: false,
  url: '',
  width: '70vw',
  height: '70%'
})
const embeddedStore = useEmbedded()
const previewFrameRef = ref<HTMLIFrameElement | null>(null)
const hideTrackBarMenus = () => {
  const hide = () => {
    document.querySelectorAll<HTMLElement>('.track_bar_custom').forEach(menu => {
      menu.style.display = 'none'
      menu.setAttribute('aria-hidden', 'true')
    })
  }
  hide()
  window.setTimeout(hide, 0)
  window.setTimeout(hide, 100)
}
const dialogStyle = computed(() => {
  if (state.fullscreen) {
    return [
      { '--ed-dialog-bg-color': canvasStyleData.value.dialogBackgroundColor },
      { '--ed-dialog__de-text': canvasStyleData.value.dialogButton }
    ]
  } else {
    return [
      { '--ed-dialog-bg-color': canvasStyleData.value.dialogBackgroundColor },
      { '--ed-dialog__de-text': canvasStyleData.value.dialogButton },
      { height: state.height }
    ]
  }
})

const previewInit = params => {
  hideTrackBarMenus()
  state.name = params.name || ''
  if (params.url.includes('?')) {
    state.url = `${params.url}&popWindow=true`
  } else {
    state.url = `${params.url}?popWindow=true`
  }
  if (params.size === 'large') {
    state.fullscreen = true
  } else if (params.size === 'middle') {
    state.fullscreen = false
    state.width = '80vw'
    state.height = '80%'
  } else {
    state.fullscreen = false
    state.width = '65vw'
    state.height = '65%'
  }
  state.dialogShow = true
  void nextTick(hideTrackBarMenus)
  void embeddedStore
}

const downloadData = () => {
  previewFrameRef.value?.contentWindow?.postMessage(
    { type: 'crest:preview-pop-download-data' },
    window.location.origin
  )
}
// 监听弹窗显示隐藏，控制tooltip显示隐藏，避免遮挡弹窗
watch(
  () => state.dialogShow,
  show => {
    document.querySelectorAll('.g2-tooltip')?.forEach(tooltip => {
      tooltip.classList.toggle('hidden-tooltip', show)
    })
    if (show) {
      hideTrackBarMenus()
    }
    if (!show) ChartCarouselTooltip.closeEnlargeDialogDestroy()
  }
)

defineExpose({
  previewInit
})
</script>

<style lang="less">
.preview_pop_custom {
  overflow: hidden;
  :deep(.is_link) {
    &:hover {
      color: var(--ed-dialog__de-text) !important;
    }
  }
  .ed-dialog__close {
    color: var(--ed-dialog__de-text) !important;
  }
  .preview-main-frame-outer {
    width: 100%;
    height: 100%;
    .preview-main-frame {
      width: 100%;
      height: 100%;
    }
  }
  .ed-dialog__body {
    height: calc(100% - 42px);
    padding: 0;
  }
  .ed-dialog__header {
    height: 36px;
    padding-right: 88px;
    .ed-dialog__headerbtn {
      top: 4px !important;
      right: 8px !important;
    }
  }
  .preview-pop-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 36px;
    padding-left: 12px;
  }
  .preview-pop-title {
    min-width: 0;
    overflow: hidden;
    color: var(--ed-dialog__de-text);
    font-size: 13px;
    font-weight: 600;
    line-height: 36px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .preview-pop-download {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    height: 26px;
    padding: 0 10px;
    color: var(--ed-dialog__de-text);
    font-size: 12px;
    line-height: 26px;
    cursor: pointer;
    background: rgba(255, 255, 255, 0.78);
    border: 1px solid rgba(185, 28, 28, 0.18);
    border-radius: 4px;
  }
  .preview-pop-download:hover {
    color: #a92323;
    background: #fff7f7;
    border-color: rgba(185, 28, 28, 0.32);
  }
  .preview-pop-download-icon {
    width: 14px;
    height: 14px;
  }
}
</style>
