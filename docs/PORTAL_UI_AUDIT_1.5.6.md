# Portal 与大屏查看体验审查记录

> 审查日期：2026-06-07  
> 审查分支：security/1.5.6-hardening  
> 审查范围：门户主页、门户大屏查看页、站点标题与图标加载  
> 审查状态：已记录，本轮不做视觉样式重构

## 基线确认

- 当前分支已包含 `v1.5.5`，且相较 `v1.5.5` 继续包含 1.5.6 开发期变更。
- 本地 `http://localhost:18100/#/portal` 已用当前分支重新构建前端 `dist`、后端 jar，并重建 `crest-web:split`、`crest-service:split` 镜像。
- 入口页资源已不再引用 `1.5.4-crest` 文件名，浏览器标题为 `研发效能指标管理系统`。
- `favicon.svg` 服务端返回内容与 `core/core-frontend/public/favicon.svg` 哈希一致。

## 本轮已处理

### 1. 站点标题兜底仍可能回退到 Crest

- 文件：`core/core-frontend/src/store/modules/appearance.ts`
- 文件：`core/core-frontend/src/utils/utils.ts`
- 问题：站点配置接口为空、配置加载失败或直接调用 `setTitle()` 无参数时，前端仍可能把标题回退为 `Crest`。
- 处理：统一兜底为 `研发效能指标管理系统`，避免新环境或空配置环境出现旧品牌名。

## 已验证但暂不列为问题

### 1. Portal 表格操作列居中

- 文件：`core/core-frontend/src/views/portal/DataPortal.vue:548`
- 文件：`core/core-frontend/src/views/portal/DataPortal.vue:1283`
- 验证结果：桌面视口下操作列宽度 104px，按钮容器宽度 104px，中心偏移为 0。
- 结论：当前开发包中“收藏/查看”按钮已居中，不需要重复修复。

### 2. 大屏筛选下拉可选择

- 页面：`/#/portal/view/1261000000000000000?dvType=dataV`
- 验证结果：点击筛选外层 select wrapper 后，项目选项可见；选择 `灰度发布平台` 后筛选值能写入控件。
- 结论：当前开发包未复现“下拉无法选择”。后续如果用户仍反馈，需要重点确认是否是旧包缓存、浏览器缩放、嵌入模式或具体大屏配置差异。

### 3. 大屏查看页顶部位置已与门户页对齐

- 文件：`core/core-frontend/src/views/portal/DataPortal.vue:633`
- 文件：`core/core-frontend/src/views/portal/DataPortalView.vue:156`
- 验证结果：1440px 桌面视口下，两页左侧品牌区域坐标一致，均为 x=72、y=12；浏览器控制台无错误。
- 结论：当前开发包中切换门户与大屏查看页不会再出现明显左侧品牌跳动。

## 后续建议改造

### P1. 抽取统一 PortalHeader，避免主页和大屏页继续漂移

- 文件：`core/core-frontend/src/views/portal/DataPortal.vue:633`
- 文件：`core/core-frontend/src/views/portal/DataPortalView.vue:156`
- 现状：门户主页使用 flex header，大屏查看页使用三列 grid header。两处视觉参数接近，但实现不同。
- 风险：后续改 logo、标题字号、右侧按钮或移动端断点时，很容易只改一处，造成再次位移。
- 建议：抽取 `PortalHeader.vue`，用 props 控制中间资源标题、右侧动作按钮、是否显示返回/刷新/全屏；把品牌区尺寸、padding、字体、账号入口统一到一个组件内。

### P1. 图标按钮缺少可访问名称

- 文件：`core/core-frontend/src/views/portal/DataPortalView.vue:80`
- 文件：`core/core-frontend/src/views/portal/DataPortal.vue:564`
- 现状：返回、刷新、全屏、收藏、查看均主要依赖 tooltip 和图形表达，按钮本身缺少 `aria-label`。
- 风险：键盘和读屏场景下按钮语义不足，自动化测试也更难稳定定位。
- 建议：给所有纯图标按钮补充 `aria-label`，并统一图标按钮组件的 hover、focus-visible、disabled 状态。

### P2. 常用入口图标资源管理方式需要收敛

- 文件：`core/core-frontend/src/views/portal/DataPortal.vue:77`
- 现状：常用入口图标使用 `/svg/...` 字符串，并带有 `?v=20260606` 这类临时缓存参数。
- 风险：后续换包、离线部署或构建路径变化时，图标容易再次缺失；临时版本参数也不利于长期维护。
- 建议：改为 import 项目内 SVG 资源，或建立 portal entry icon 映射表，避免直接拼公开路径。

### P2. 长标题和右侧动作区需要明确压缩规则

- 文件：`core/core-frontend/src/views/portal/DataPortalView.vue:220`
- 现状：大屏标题最大宽度为 `min(24vw, 460px)`，在宽屏下会较早截断；右侧动作区固定堆叠 3 个图标按钮和账号入口。
- 风险：长大屏名称、长用户名或新增动作按钮时，标题可读性下降。
- 建议：统一 header 三列的最小/最大宽度约束，右侧动作超过阈值时进入更多菜单，标题使用中间剩余空间而不是固定 `24vw`。

### P2. Portal 首屏空收藏缺少行动入口

- 文件：`core/core-frontend/src/views/portal/DataPortal.vue:34`
- 文件：`core/core-frontend/src/views/portal/DataPortal.vue:187`
- 现状：普通用户默认进入“我的收藏”，无收藏时仅显示空态文案。
- 风险：新用户会以为没有资源，必须自己发现“全部资源”页签。
- 建议：空收藏状态增加一个明确的“去全部资源”动作，或在空态下展示可收藏资源推荐，但不要改变默认页签。

### P3. Portal 响应式规则仍偏页面内聚，缺少设计级断点规范

- 文件：`core/core-frontend/src/views/portal/DataPortal.vue:1363`
- 文件：`core/core-frontend/src/views/portal/DataPortalView.vue:336`
- 现状：门户主页和大屏查看页各自维护断点，断点值和压缩策略不完全一致。
- 风险：后续新增入口、按钮、资源类型后，PC 缩放和窄屏体验容易出现局部挤压。
- 建议：沉淀 portal 专用布局 token，例如 header 高度、页面左右 padding、品牌区最大宽度、右侧动作最小宽度、内容区断点。

## 建议拆分顺序

1. 先做 `PortalHeader` 抽取和图标按钮可访问性补齐，风险低、收益高。
2. 再处理常用入口图标资源管理，避免离线包和缓存场景再次丢图。
3. 最后集中做长标题、空收藏引导和响应式断点，作为一次独立 UI 收口，不混入安全或发布分支。
