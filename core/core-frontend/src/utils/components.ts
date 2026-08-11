import { defineAsyncComponent } from 'vue'

// 组件懒加载：避免首屏 bundle 打入所有自定义组件(tinymce/exceljs/echarts/video.js 等重组件
// 按组件名动态 import，仅在大屏实际使用到该组件时才加载对应 chunk，分享链接/纯表格大屏首屏体积大幅下降。
const VText = defineAsyncComponent(() => import('@/custom-component/v-text/Component.vue'))
const VQuery = defineAsyncComponent(() => import('@/custom-component/v-query/Component.vue'))
const VTextAttr = defineAsyncComponent(() => import('@/custom-component/v-text/Attr.vue'))
const Group = defineAsyncComponent(() => import('@/custom-component/group/Component.vue'))
const GroupAttr = defineAsyncComponent(() => import('@/custom-component/group/Attr.vue'))
const UserView = defineAsyncComponent(() => import('@/custom-component/user-view/Component.vue'))
const UserViewAttr = defineAsyncComponent(() => import('@/custom-component/user-view/Attr.vue'))
const Picture = defineAsyncComponent(() => import('@/custom-component/picture/Component.vue'))
const PictureAttr = defineAsyncComponent(() => import('@/custom-component/picture/Attr.vue'))
const DynamicBackground = defineAsyncComponent(
  () => import('@/custom-component/dynamic_background/Component.vue')
)
const DynamicBackgroundAttr = defineAsyncComponent(
  () => import('@/custom-component/dynamic_background/Attr.vue')
)
const Decoration = defineAsyncComponent(() => import('@/custom-component/decoration/Component.vue'))
const DecorationAttr = defineAsyncComponent(() => import('@/custom-component/decoration/Attr.vue'))
const CanvasBoard = defineAsyncComponent(() => import('@/custom-component/canvas-board/Component.vue'))
const CanvasBoardAttr = defineAsyncComponent(() => import('@/custom-component/canvas-board/Attr.vue'))
const CanvasIcon = defineAsyncComponent(() => import('@/custom-component/canvas-icon/Component.vue'))
const CanvasIconAttr = defineAsyncComponent(() => import('@/custom-component/canvas-icon/Attr.vue'))
const Tabs = defineAsyncComponent(() => import('@/custom-component/tabs/Component.vue'))
const TabsAttr = defineAsyncComponent(() => import('@/custom-component/tabs/Attr.vue'))
const Graphical = defineAsyncComponent(() => import('@/custom-component/graphical/Component.vue'))
const GraphicalAttr = defineAsyncComponent(() => import('@/custom-component/graphical/Attr.vue'))
const CircleShape = defineAsyncComponent(() => import('@/custom-component/circle-shape/Component.vue'))
const CircleShapeAttr = defineAsyncComponent(
  () => import('@/custom-component/circle-shape/Attr.vue')
)
const RectShape = defineAsyncComponent(() => import('@/custom-component/rect-shape/Component.vue'))
const RectShapeAttr = defineAsyncComponent(() => import('@/custom-component/rect-shape/Attr.vue'))
const SvgTriangle = defineAsyncComponent(
  () => import('@/custom-component/svgs/svg-triangle/Component.vue')
)
const SvgTriangleAttr = defineAsyncComponent(
  () => import('@/custom-component/svgs/svg-triangle/Attr.vue')
)
const TimeClock = defineAsyncComponent(() => import('@/custom-component/time-clock/Component.vue'))
const TimeClockAttr = defineAsyncComponent(() => import('@/custom-component/time-clock/Attr.vue'))
const GroupArea = defineAsyncComponent(() => import('@/custom-component/group-area/Component.vue'))
const GroupAreaAttr = defineAsyncComponent(() => import('@/custom-component/group-area/Attr.vue'))
const Frame = defineAsyncComponent(() => import('@/custom-component/frame/ComponentFrame.vue'))
const FrameAttr = defineAsyncComponent(() => import('@/custom-component/frame/Attr.vue'))
const Screen = defineAsyncComponent(() => import('@/custom-component/screen/Component.vue'))
const ScreenAttr = defineAsyncComponent(() => import('@/custom-component/screen/Attr.vue'))
const Video = defineAsyncComponent(() => import('@/custom-component/video/Component.vue'))
const VideoAttr = defineAsyncComponent(() => import('@/custom-component/video/Attr.vue'))
const StreamMedia = defineAsyncComponent(() => import('@/custom-component/stream-media/Component.vue'))
const StreamMediaAttr = defineAsyncComponent(
  () => import('@/custom-component/stream-media/Attr.vue')
)
const ScrollText = defineAsyncComponent(() => import('@/custom-component/scroll-text/Component.vue'))
const ScrollTextAttr = defineAsyncComponent(() => import('@/custom-component/scroll-text/Attr.vue'))
const PopArea = defineAsyncComponent(() => import('@/custom-component/pop-area/Component.vue'))
const PopAreaAttr = defineAsyncComponent(() => import('@/custom-component/pop-area/Attr.vue'))
const PictureGroup = defineAsyncComponent(() => import('@/custom-component/picture-group/Component.vue'))
const PictureGroupAttr = defineAsyncComponent(
  () => import('@/custom-component/picture-group/Attr.vue')
)
const ExportButton = defineAsyncComponent(
  () => import('@/custom-component/export-button/Component.vue')
)
const ExportButtonAttr = defineAsyncComponent(() => import('@/custom-component/export-button/Attr.vue'))

export const componentsMap = {
  VText,
  VQuery,
  VTextAttr,
  Group,
  GroupAttr,
  UserView,
  UserViewAttr,
  Picture,
  PictureAttr,
  DynamicBackground,
  DynamicBackgroundAttr,
  CanvasBoard,
  CanvasBoardAttr,
  CanvasIcon,
  CanvasIconAttr,
  Tabs,
  TabsAttr,
  Graphical,
  GraphicalAttr,
  CircleShape,
  CircleShapeAttr,
  RectShape,
  RectShapeAttr,
  SvgTriangle,
  SvgTriangleAttr,
  TimeClock,
  TimeClockAttr,
  GroupArea,
  GroupAreaAttr,
  Frame,
  FrameAttr,
  Video,
  VideoAttr,
  StreamMedia,
  StreamMediaAttr,
  ScrollText,
  ScrollTextAttr,
  PopArea,
  PopAreaAttr,
  PictureGroup,
  PictureGroupAttr,
  ExportButton,
  ExportButtonAttr,
  Decoration,
  DecorationAttr,
  Screen,
  ScreenAttr,
  DeDecoration: Decoration,
  DeDecorationAttr: DecorationAttr,
  DeFrame: Frame,
  DeFrameAttr: FrameAttr,
  DeGraphical: Graphical,
  DeGraphicalAttr: GraphicalAttr,
  DeScreen: Screen,
  DeScreenAttr: ScreenAttr,
  DeStreamMedia: StreamMedia,
  DeStreamMediaAttr: StreamMediaAttr,
  DeTabs: Tabs,
  DeTabsAttr: TabsAttr,
  DeTimeClock: TimeClock,
  DeTimeClockAttr: TimeClockAttr,
  DeVideo: Video,
  DeVideoAttr: VideoAttr
}

export default function findComponent(key) {
  return componentsMap[key]
}

export function findComponentAttr(component) {
  const key =
    component.component === 'UserView' && component.innerType === 'picture-group'
      ? 'PictureGroupAttr'
      : component.component + 'Attr'
  return componentsMap[key]
}
