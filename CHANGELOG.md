# Changelog

## v1.5.37 - 2026-08-25

v1.5.37 是 v1.5.36 之后的修复与增强版本。投产日历图例从写死固定 4 类改为按数据集实际类型动态显示；修复 v1.5.33 懒加载引入的预览/分享态图表不发 data 请求导致数据为空的 P0 bug。**本版本没有新增数据库结构，无需执行升级 SQL。**

### 新功能

- 投产日历图例动态化：版本类型图例从写死的固定 4 类（周版/月版/待定版/紧急版）改为按数据集里实际出现的版本类型动态显示，每个类型可独立配色（`typeColorMap`），样式面板动态渲染颜色选择器。

### Bug 修复

- **预览/分享态图表不发 data 请求（P0）**：v1.5.33 将 `ChartComponentG2Plot`/`ChartComponentS2` 改为 `defineAsyncComponent` 异步加载，导致预览态/分享态首次加载时 `chartComponent` ref 为空、`calcData` 跳过取数。修复为加 `watch(chartComponent)` 在异步组件就绪时补触发 `queryData`，保留懒加载性能优化。

## v1.5.36 - 2026-08-17

v1.5.36 是 v1.5.35 之后的修复与增强版本。修复明细表「单元格分组」合并文本的换行和居中不生效问题,并补齐该功能的全套样式配置面板;同时为「投产窗口日历」的日期弹出框(popup)新增可视化样式配置,并支持待定版/紧急版圆点标记。**本版本没有新增数据库结构,无需执行升级 SQL。**

### 修复

- 单元格分组合并文本「换行」和「居中」不生效:面板设居中、勾换行后文字仍靠左、长文本不截断。根因是 S2 v2 的 `MergedCell` meta 只是左上角单格 meta,换行宽度/对齐基准只落在第一格内,且 `wordBreak`/`whiteSpace` 对 G-lite Text 无效。修复为 `CustomMergedCell` 新增 `getMergedContentBox` 基于合并后整体矩形,重写 `getMaxTextWidth`/`getTextPosition`,换行改设 `wordWrap`/`maxLines`。

### 增强

- 单元格分组合并文本样式可配置:为「单元格分组」新增颜色/字号/加粗/对齐/换行的全局样式面板,配置存于 `customAttr.tableCell` 的 `cellGroupText*` 字段,`configCellGroups` 注入 `cellGroupTextStyle` 由 `CustomMergedCell` 覆盖 S2 默认。
- 投产窗口日历弹出框(popup)样式可配置:日期标题、版本类型标签、投产内容三段文字的颜色/字号/加粗/对齐/换行可在面板调整;新增「待定版」「紧急版」圆点标记(`pendingDotColor`/`urgentDotColor`)。

## v1.5.35 - 2026-08-17

v1.5.35 是 v1.5.34 之后的修复与增强版本。修复「投产窗口日历」在深色画布下月份切换按钮不可见的问题,并为该日历组件补充可视化样式配置面板。**本版本没有新增数据库结构,无需执行升级 SQL。**

### 修复

- 投产窗口日历月份切换按钮不可见:深色画布下「上一月 / 回到今天 / 下一月」三个导航按钮继承外层深色文字导致隐形,修复为跟随标题色(`titleColor`)内联渲染,深浅色画布均可见。

### 增强

- 投产窗口日历样式可配置:新增「投产窗口日历样式」配置面板,月份标题、日期、单元格底色、状态高亮、圆点、星期表头、图例等颜色字号可在右侧面板可视化调整;配置存于 `customAttr.deployCalendar`(`ChartDeployCalendarStyle`)。
- 兼容老数据:`DeployCalendarView.setupDefaultOptions` 新建/切换图表时用 `defaultsDeep` 自动补全样式默认值,兼容 v1.5.34 无 `deployCalendar` 字段的老图表。

## v1.5.34 - 2026-08-13

v1.5.34 是 v1.5.33 之后的新功能版本。新增明细表「单元格分组」手动合并能力,并新增「投产窗口日历」自定义图表组件。**本版本没有新增数据库结构,无需执行升级 SQL。**

### 新增功能

- 明细表「单元格分组」:按定位列值匹配首行,横向合并起列到止列为一个单元格并支持自定义显示文本,匹配值可从当前页数据模糊搜索;与按值自动合并(mergeCells)可共存,手动优先。配置存于 `customAttr.tableCell.cellGroups`,`common_table.ts` 新增 `configCellGroups` 转成 S2 `mergedCellsInfo`。
- 「投产窗口日历」自定义图表组件(`deploy-calendar`):日历视图展示投产记录,点击日期联动查询明细表,再次点击清除联动恢复默认。

## v1.5.33 - 2026-08-11

v1.5.33 是 v1.5.32 之后的性能优化与修复版本。优化前端组件懒加载降低首屏体积,并改进导出应用模板的错误提示。**本版本没有新增数据库结构,无需执行升级 SQL。**

### 优化

- 前端组件懒加载(P0 性能优化):`utils/components.ts` 所有自定义组件改 `defineAsyncComponent` 按需加载,`views/chart/components/views/index.vue` 的 `ChartComponentG2Plot`(echarts)与 `ChartComponentS2`(antv-s2)改异步。首屏 preload 从 ~7MB 降到 3.6MB,tinymce/exceljs/echarts/video.js 移出首屏。

### 修复

- 导出应用模板错误提示不明确:`DataVisualizationServer.validateAppExportScope` 三处"包含无效数据集"笼统提示改为带具体数据集名称,信息精简到 200 字符以内避免被 `safeClientMessage` 降级。

## v1.5.32 - 2026-08-11

v1.5.32 是 v1.5.31 之后的修复版本。修复分享链接升级弹框、透视表合计为空、汇总表自定义公式列为空、明细表自定义排序跨页乱序四个问题。**本版本没有新增数据库结构,无需执行升级 SQL。**

### 修复

- 分享链接(`/link/`)打开时不再提示"系统有升级"弹框:`service.ts` 的 `executeVersionHandler` 判断当前路由为 `/link/` 分享路径时直接跳过升级提示。
- 透视表行/列合计均为空:`table-pivot.ts` 对齐 S2 v2.7.1 的 `TotalStatus` 字段名(`isRowGrandTotal`/`isColGrandTotal`),新增 `normalizeTotalStatus` 兼容旧名,合计节点正确匹配聚合配置。
- 汇总表自定义公式列为空:`tableSummaryColumns.mjs` 新增 `evalSummaryColumnFormula`(strip 前导 `=`、纯算术白名单求值、单引号转义)+ `buildIdToEngineFieldMap`,`appendSummaryColumnsToS2Config` 对 custom 公式列逐行算值;`table-normal.ts`/`table-info.ts` 构建 id→engineFieldName 映射并传入。
- 明细表自定义排序跨页乱序:`Dimension2SQLObj.java` 对 `custom_sort` 维度生成 `ORDER BY CASE WHEN ... END ASC` 下推 SQL,分页 `LIMIT` 在全局自定义顺序上切分;ES 数据源跳过;新增 `Dimension2SQLObjTest` 单测。

## v1.5.31 - 2026-08-03

v1.5.31 是 v1.5.30 之后的修复版本。修复明细表带查询条件后结果剩 1 页时不显示分页数量的问题。**本版本没有新增数据库结构,无需执行升级 SQL。**

### 修复

- 明细表(component type S2)带查询条件后结果剩 1 页(totalItems ≤ pageSize)时分页栏整块不渲染,"共 X 条"总数消失:`ChartComponentS2.vue` 的 `setupPage` 分页栏显示条件由 `totalItems > pageSize` 改为 `totalItems > 0`,单页也显示分页栏。

## v1.5.30 - 2026-08-03

v1.5.30 是 v1.5.29 之后的功能改进版本。新增数据大屏弹窗区查询按钮位置可配置(右上/右下)并带「高级查询」文字标签,并补全数据大屏分享链接仍显示图表悬浮导出按钮的问题。**本版本没有新增数据库结构,无需执行升级 SQL。**

### 新增

- 弹窗区查询按钮位置可配置(右上/右下)+「高级查询」文字标签:`CanvasBaseSetting.vue` 新增「查询按钮位置」单选项;`canvasUtils.ts` 的 `historyAdaptor` 兼容 `popupButtonPosition`(旧画布默认 `bottomRight`);`canvas-filter-btn/Component.vue` 接收 `position` prop 定位并加文字标签;`CrestPreview.vue` 透传 `popupButtonPosition`;`locales/en.ts`、`zh-CN.ts` 新增 `pop_button_position`/`_top`/`_bottom` 文案。

### 修复

- 数据大屏分享链接仍显示图表悬浮导出按钮(v1.5.29 遗漏):`ComponentEditBar.vue` 独立导出箭头 `el-dropdown` 的 `v-if` 增加 `dvInfo.type !== 'dataV'` 条件;`UserViewEnlarge.vue` 放大视图的图片导出按钮/像素选择/分割线同步补齐 `dvInfo.type !== 'dataV'` 条件。数据大屏场景不再渲染导出按钮。

## v1.5.29 - 2026-07-29

v1.5.29 是 v1.5.28 之后的修复版本。修复数据大屏分享链接仍显示导出 Excel 按钮、图表字段显示名称无法留空两个问题。**本版本没有新增数据库结构,无需执行升级 SQL。**

### 修复

- 数据大屏(dataV)分享链接「查看数据」放大后仍显示导出 Excel 按钮:`UserViewEnlarge.vue` 三个导出按钮 `v-if` 增加 `dvInfo.type !== 'dataV'` 守卫,数据大屏场景不再渲染导出按钮。
- 图表字段显示名称无法留空:移除 `chartShowName` 的 `required` 校验与 `showRename` 中 `name` 自动回填逻辑,显示名称现可留空。`index.vue`。

## v1.5.28 - 2026-07-29

v1.5.28 是 v1.5.27 之后的修复与功能改进版本。重构数据集「顺序调整」页签为 4 列布局并补齐拖拽,改进饼图/玫瑰图/柱状图标签防重叠与偏移,调整明细表合并单元格比较规则,并在仪表板隐藏明细导出按钮。**本版本没有新增数据库结构,无需执行升级 SQL。**

### 修复

- 修复数据集「顺序调整」页签被批量管理大表格占用:`batch-area` 显示条件由 `tabActive !== 'preview'` 收窄为 `tabActive === 'manage'`,顺序页签现只显示顺序列表。`dataset/form/index.vue`。
- 修复拖拽手柄图标不渲染:`<drag>` 改为 `<dragSvg>`(与文件内其它图标导入用法一致)。`dataset/form/index.vue`。
- 补齐 `en-US.ts`、`tw.ts` 缺失的 `field_order`/`field_order_tip`/`moveUp`/`moveDown`,新增 `dim_quota_type`、`drag_sort`。四个 locale 文件均已对齐。

### 新增

- 顺序调整页签 4 列布局:字段名称/物理字段名/维度·指标/操作(上移、下移、拖拽),拖拽手柄并入操作列,第 3 列改为维度/指标标签。`dataset/form/index.vue`。
- 饼图/玫瑰图标签防重叠(`labelAntiOverlap`,默认开启)与标签偏移(`labelOffset`)。`pie.ts`、`rose.ts`、`LabelSelector.vue`。
- 抽取按汇总方式计算总数的 `calcTotalValue`,柱状图与饼图共用。`bar/common.ts`。

### 调整

- 明细表合并单元格改回严格相等比较,`POS系统` 与 `pos系统` 不再合并。`tableMergeCells.mjs`。
- 仪表板放大查看隐藏明细导出按钮。`UserViewEnlarge.vue`。
- 后端 `TableInfoHandler`、`ChartDataServer`、`CalciteProvider`、`Provider` 跨源与数据查询相关修复。

## v1.5.27 - 2026-07-22

v1.5.27 是 v1.5.26 之后的修复与功能改进版本。修复 v1.5.26 跨源连表查询失败、导出 Excel 小数显示异常，并改进数据集缓存按天自定义时点（改为纯前端实现，无需数据库迁移）。**本版本没有新增数据库结构，无需执行升级 SQL。**

### 修复

- 修复跨源连表查询失败（`Unknown database 's_a_xxx'`）：`CalciteProvider` 跨源分支只替换了第一个数据源的虚拟 schema，改为遍历 `dsList` 所有 entry 替换。`fetchResultField` 和 `fetchTableField` 两处。`CalciteProvider.java`。
- 修复跨源 SQL 语法错误（`OFFSET N ROWS LIMIT N`）：Calcite `toSqlString` 生成 `OFFSET N ROWS FETCH NEXT M ROWS ONLY`，MySQL 不认识。`SqlUtils.addSchema` 增加 `OFFSET N ROWS FETCH NEXT M ROWS ONLY → LIMIT M OFFSET N` 替换，残留 `OFFSET N ROWS` 去除。`SqlUtils.java`。
- 修复导出 Excel FLOAT 整数值显示 `0.`（`0.######` 格式对 double 0.0 渲染）和 FLOAT 小数被误取整（`3.14` 显示 `3`）：FLOAT 整数值用纯整数格式 `"0"` 写入，非整数值显式用 `"0.######"` 小数格式写入。`ChartDataServer.java`（2 处）、`ExportCenterDownLoadManage.java`（1 处）。
- 修复保存同步任务后「按天时点」丢失：前端 `buildDatasetSyncTaskPayload` 漏传 `simpleCronHour`，已补传。

### 新增

- 数据集缓存按天自定义时点（纯前端方案，无数据库迁移）：利用后端 `syncRate=CRON` 模式，前端在「天 + 时点」时生成完整 cron 表达式存入 `cron` 字段。加载时反解回 UI。不新增数据库字段、不执行 Flyway 迁移。
- 明细表合并单元格大小写不敏感：`Pos系统/POS系统/pos系统` 相邻合并为一行，合并段统一显示大写。`tableMergeCells.mjs` 改 `sameValue` 大小写不敏感比较，`common_table.ts` 合并单元格 `fieldValue` 转大写。
- 数据集字段上下移：字段列表支持上移/下移调整顺序，`column_index` 持久化，导出列按调整后顺序排列。

### 验证

- 数据集缓存按天选 8 点保存后重开仍为 8 点，DB `cron='0 0 8 1/1 * ? *'`、`sync_rate='CRON'`。
- 跨源连表关联查询（两个数据源 JOIN）成功执行，不再报 `Unknown database`。
- 导出 FLOAT 整数 `12.0`→`12`、`0`→`0`；小数 `3.14`→`3.14`。
- 明细表含 Pos/POS/pos 相邻值合并为一行。

### 安装说明

- **从 v1.5.25 或 v1.5.26 升级到 v1.5.27 只需要替换前后端镜像，不需要新增升级 SQL。**
- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.27` 和 `ghcr.io/sevoniva/crest-web:v1.5.27`。

## v1.5.26 - 2026-07-20

v1.5.26 是 v1.5.25 之后的功能版本，新增数据集缓存按天自定义时点、明细表合并大小写、数据集字段上下移，并修复导出 Excel 小数显示缺陷。**本版本新增数据库结构，升级需执行升级 SQL（V1.5.9.1）。**

### 新增

- 数据集缓存按天自定义时点：按天更新支持 0-23 点选时点（如每天 8 点），新增 `simple_cron_hour` 字段与 Flyway 迁移 `V1.5.9.1__dataset_sync_task_simple_cron_hour.sql`。旧行 NULL 默认 0 点，向后兼容。
- 明细表合并单元格大小写不敏感：`Pos系统/POS系统/pos系统` 相邻合并为一行，合并段统一显示大写。`tableMergeCells.mjs` 改 `sameValue` 大小写不敏感比较，`common_table.ts` 合并单元格 `fieldValue` 转大写。
- 数据集字段上下移：字段列表支持上移/下移调整顺序，`column_index` 持久化，导出列按调整后顺序排列，null 排到最后兼容旧数据。

### 修复

- 修复导出 Excel FLOAT 整数显示 `0.`：FLOAT 整数值用纯整数格式 `"0"` 写入，显示 `0`/`12`（不再 `0.`/`12.0`）。
- 修复导出 Excel FLOAT 小数被误取整（`3.14` 显示成 `3`）：FLOAT 非整数值显式用 `"0.######"` 小数格式写入，不被 formatter 整数格式覆盖。`ChartDataServer.setExcelData`（table-info/table-normal + 其他视图分支）与 `ExportCenterDownLoadManage.setExportCellValue` 两条路径均已修复。
- 修复保存同步任务后「按天时点」丢失：前端 `buildDatasetSyncTaskPayload` 漏传 `simpleCronHour`，已补传。

### 验证

- 数据集缓存按天选 8 点保存后重开仍为 8 点，DB `simple_cron_hour=8`、`cron='0 0 8 1/1 * ? *'`。
- 明细表含 Pos/POS/pos 相邻值合并为一行，中央显示 POS系统。
- 字段上下移保存后导出 Excel 列顺序与调整后一致。
- 导出 FLOAT 整数 `12.0`→`12`、`0`→`0`；小数 `3.14`→`3.14`。

### 安装说明

- **本次新增数据库结构，从 v1.5.25 升级需执行升级 SQL**：后端启动 Flyway 自动应用 `V1.5.9.1`（`dataset_sync_task` 加 `simple_cron_hour` 列）。
- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.26` 和 `ghcr.io/sevoniva/crest-web:v1.5.26`。

## v1.5.25 - 2026-07-18

v1.5.25 是 v1.5.24 之后的缺陷修复版本，修复导出 Excel 整数列带多余小数点、以及日期列带多余 `00:00:00` 的问题。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复导出 Excel 整数列带多余小数点：整数类型字段被 `Double.parseDouble` 转 double 写入，POI 内部存为 `7.0`，格式 `0.######` 在部分 Excel/WPS 版本仍显示 `7.0`。改为整数字段强制设 `"0"` 格式（纯整数）并用 `longValueExact` 写入，确保显示 `7` 不带 `.0`；FLOAT 保留 double。`ChartDataServer.setExcelData`（table-info/table-normal 分支 + 其他视图分支）与 `ExportCenterDownLoadManage.setExportCellValue` 两条路径均已修复。
- 修复导出 Excel 日期列带多余 `00:00:00`：数据源返回 `"2026-05-28 00:00:00"`（纯日期 + 零时分秒）原样写入。新增 `normalizeDateString`，对 TIME 类型若值形如 `yyyy-MM-dd 00:00:00` 截成 `yyyy-MM-dd`；带实际时分秒的完整 datetime 不变。两条路径均已接入。

### 验证

- 导出明细 Excel，整数列显示 `7`、`21`（不再 `7.0`、`21.0`）；日期列显示 `2026-05-28`（不再 `2026-05-28 00:00:00`）；FLOAT 列保持原样。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.25` 和 `ghcr.io/sevoniva/crest-web:v1.5.25`。
- 从 v1.5.24 升级到 v1.5.25 只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.24 - 2026-07-17

v1.5.24 是 v1.5.23 之后的缺陷修复版本，修复跨数据源自定义 SQL 查询（含中文 CASE WHEN 别名/字面量）执行失败、连续查询偶发 40001、以及明细表 s2 v2 冻结配置失效的问题。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复跨数据源查询含中文等非 ASCII 字符串（如 `CASE WHEN ... THEN '中文' ELSE '其他' END AS 中文列`）失败：Calcite planning 会给非 ASCII 字面量加 `_UTF-8'xxx'` 前缀，MySQL 不认识。`CalciteProvider.fetchResultField` / `fetchTableField` 跨源分支改为用 `getConnectionFromPool` 返回的底层 MySQL JDBC 连接直接执行 + 虚拟 schema 替换为真实库名，绕过 Calcite 的 `_UTF-8` 注入；新增 `NO_CHARSET_FACTORY`（`supportsCharSet()=false`）套用到所有 `JdbcSchema.create`；`SqlUtils.addSchema` 渲染后清理 `_UTF-8'` 前缀并还原 `LIMIT N`；新增 `convertSingleQuotedAliases` 把 `AS '别名'`/`AS "别名"` 转反引号，`Provider`/`transSqlDialect` 同步调用；Calcite 连接 Lex `JAVA`→`MYSQL` 并设 `CONFORMANCE=MYSQL_5`。
- 修复连续跨源查询偶发 40001 / 连接池超时：上述两个跨源分支用 `take()` 借出连接后未 `close`，每次查询泄漏一个连接，池耗尽后 `getConnectionFromPool` borrow 超时（5s）抛 40001。改为 `try-with-resources` 确保归还。
- 修复明细表冻结行/列及汇总行冻结失效：`@antv/s2` v2 把冻结配置从顶层属性（`frozenColCount`/`frozenRowCount`/`frozenTrailingRowCount`）改为 `frozen` 嵌套对象。`table-info`/`table-normal`/`common_table` 统一改写为 `options.frozen = { colCount, rowCount, trailingRowCount }`，汇总行冻结合并已有配置避免覆盖。

### 验证

- 跨数据源自定义 SQL 含中文 CASE WHEN 别名/字面量的数据集，预览、保存、运行均成功，后端日志不再出现 `_UTF-8'` 前缀；连续多次查询不再偶发 40001。
- 明细表冻结行/列及汇总行冻结恢复正常生效。
- 全量回归：前端表格 60 项单测全过；后端 core-backend 724 项无新增失败，新增 `SqlUtilsTest` 7 项全过；`Quota2SQLObjTest` 2 项「失败」为容器默认 UTC 时区所致（对照实验证实），生产镜像已设 `Asia/Shanghai` 不受影响。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.24` 和 `ghcr.io/sevoniva/crest-web:v1.5.24`。
- 从 v1.5.23 升级到 v1.5.24 只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.23 - 2026-07-15

v1.5.23 是 v1.5.22 之后的缺陷修复版本，修复明细表开启自动换行后整表空白、仪表盘分享链接默认加密码后仍要求输入密码、以及导出明细科学计数法显示异常的问题。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复明细表勾选「自动换行」后整张表空白：`@antv/s2` 升级到 v2 后自定义换行渲染 `drawTextShape` 仍沿用 v1 已移除/变更的 API（`getTextAndIconPosition`、`renderText` 位置参数、`actualText`/`actualTextWidth` 只读 getter、`getCellArea`、`LayoutResult.spreadsheet`、g5.0 `Text` 缺 `wordWrapWidth`），逐一替换为 v2 对应实现。已知遗留：最大行数 `maxLines` 截断在部分场景未完全生效，后续单独处理。
- 修复仪表盘分享链接默认加密码后仍要求输入密码：`autoPwd=true`（密钥仅用于链接签名）时后端 `pwdValid`/`valid` 仍走密码校验、`detail` 接口返回内部密钥导致前端误判。`autoPwd=true` 时直接放行并将 `pwd` 置空返回。
- 修复导出明细科学计数法（`0E-8`/`1E-8`）显示异常：`ChartDataServer` 与 `ExportCenterDownLoadManage` 新增 `normalizeScientificZero`，零值统一输出 `0`、非零科学计数法值转 `toPlainString`，避免 `isSafeExcelNumericText` 正则不匹配导致以文本写入。

### 验证

- 明细表勾选「自动换行」后表格框架与单元格内容正常显示，控制台不再有 `getTextAndIconPosition`/`Cannot set property actualText`/`getCellArea`/`measureTextWidthRoughly of undefined` 等报错。
- 仪表盘分享链接（未勾选密码保护）打开后不再要求输入密码。
- 导出明细中 `0E-8` 等零值显示为 `0`，`1E-8` 等极小值显示为普通十进制。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.23` 和 `ghcr.io/sevoniva/crest-web:v1.5.23`。
- 从 v1.5.22 升级到 v1.5.23 只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.22 - 2026-07-13

v1.5.22 是 v1.5.21 之后的缺陷修复版本，修复组件/画布上传背景图后图片加载 404 导致不显示的问题。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复组件/画布上传背景图后缩略图不显示、画布背景图不生效：nginx 静态资源正则 location 优先级高于 `/api/v1/` 前缀，`GET /api/v1/static-resource/*.jpg` 被 `try_files` 拦截返回 404。正则加 `^(?!/api/)` 负向断言，`/api/` 下图片请求不再被拦截，正常代理到后端。上传与落盘一直正常，仅图片读取 404。

### 验证

- 重建前端镜像重启容器后 `GET /api/v1/static-resource/<id>.jpg` 返回 200；前端 `/assets/*.js` 仍 200 且带 30 天缓存头。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.22` 和 `ghcr.io/sevoniva/crest-web:v1.5.22`。
- 从 v1.5.21 升级到 v1.5.22 只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.21 - 2026-07-13

v1.5.21 是 v1.5.20 之后的缺陷修复版本，修复导出 Excel 时极小数值显示为科学计数法的问题。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复导出 Excel 时极小数值（如 1E-8）显示为科学计数法（0E-8/1E-08）：`createCellStyle` 在 formatter 为 null 或 auto 类型无小数点时由 `General` 改为 `0.######`，极小值四舍五入显示为 0。
- 修复 `auto` 类型下 `split(".")` 正则 bug（`.` 未转义），此前小数位格式串计算异常。

### 验证

- 后端 `core-backend` 编译通过；`ChartDataServerExportModeTest`（11）与 `ChartDataServerAuxiliaryHeaderTest`（1）共 12 个用例全部通过。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.21` 和 `ghcr.io/sevoniva/crest-web:v1.5.21`。
- 从 v1.5.20 升级到 v1.5.21 时只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.20 - 2026-07-13

v1.5.20 是 v1.5.19 之后的缺陷修复版本，修复背景图上传、标题备注标识、iframe 嵌入导出按钮、导出原始明细多列等问题。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复组件/画布上传背景图后不显示：`applyUploadedBackground` 上传后自动启用 `backgroundImageEnable` 并设置 `backgroundType='outerImage'`。
- 图表标题备注 i 标识默认显示：`DEFAULT_TITLE_STYLE` 与 `DEFAULT_TITLE_STYLE_BASE` 的 `remarkShow` 改为 `true`。
- 修复分享链接嵌入 iframe 后放大/详情弹窗的"导出数据"与"导出原始文件"按钮消失：header 改为有任意导出权限即渲染，标题在 iframe 下单独隐藏。
- 修复开启汇总的表格导出原始明细时末尾多出空白汇总列：`setExcelData` 的 `isSummaryEnabled` 判断排除 dataset 模式，与外层判定一致。

### 验证

- 前端 `vue-tsc` 类型检查通过；前端单元测试 42 个用例全部通过。
- 后端 `core-backend` 编译通过；`ChartDataServerExportModeTest`（11）与 `ChartDataServerAuxiliaryHeaderTest`（1）共 12 个用例全部通过。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.20` 和 `ghcr.io/sevoniva/crest-web:v1.5.20`。
- 从 v1.5.19 升级到 v1.5.20 时只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.19 - 2026-07-12

v1.5.19 是 v1.5.18 之后的安全补丁版本，修复代码审计中确认的 1 个 HIGH 与 3 个 MEDIUM 问题。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复 `$crest[]` 系统变量以字符串拼接写入 SQL 的注入风险：改为 `?` 占位符参数化绑定（HIGH-1-B）。
- 加固只读上下文校验：在 SELECT/WITH/SHOW/DESC/EXPLAIN 与单语句校验之上，新增写/DDL 原语与文件写出拦截；保留 UNION/UNION ALL（数据集生成依赖）（HIGH-1-C）。
- 无密码分享分配独立随机密钥，避免回落全局 fallback 造成跨资源伪造（MEDIUM-1）。
- token 签名密钥查询新增 60 秒 TTL 缓存，并在所有改密码路径显式失效，兼顾性能与“改密即失效旧 token”语义（MEDIUM-2）。

### 验证

- 新增/补充 4 个测试类，共 28 个用例全部通过（SqlSecurityPolicyTest、ShareSecretManageTest、CrestUserManageTest、SqlParserUtilsSecurityTest）。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.19` 和 `ghcr.io/sevoniva/crest-web:v1.5.19`。
- 默认离线包为 `crest-offline-v1.5.19-linux-amd64.tar.gz` 和 `crest-offline-v1.5.19-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.19-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.19-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.19.tar.gz`。
- 从 v1.5.18 升级到 v1.5.19 时只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.18 - 2026-07-11

v1.5.18 是 v1.5.17 之后的安全补丁版本，修复系统管理模块安全审计报告中遗留的 6 个高危问题。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复 SSO 回调缺少 nonce 校验的问题：callback 阶段现在会从 id_token 中提取 nonce 声明，与登录发起时缓存的 nonce 比对；id_token 未包含 nonce 时记录告警日志。
- 修复 SSO id_token claims 未校验的问题：现在会校验 id_token 的 issuer（如已配置）、audience（与 clientId 比对）和 expiration 声明，防止伪造 id_token payload 中的用户身份。
- 新增 SSO id_token 签名验证：配置 JWKS 公钥端点后，系统会从 JWKS 获取公钥验证 id_token 的 RSA 签名；未配置 JWKS 时保持 claims 校验模式（向后兼容）。
- 修复 SSO 生成的 token 永不过期的问题：SSO 登录生成的 token 现在设置 24 小时有效期，与本地登录保持一致；密码修改后旧 token 将在过期后失效。
- 修复权限范围 SQL 中 resourceType 参数未做白名单校验的问题：resourceScopeSql 现在对 resourceType 执行白名单校验（datasource、dataset、panel、screen），并对列名参数执行格式校验，防止 SQL 注入。
- 修复 communityScopeSql 中 uid 值字符串拼接的问题：现在会校验 uid 字符串为纯数字后才拼接到 SQL，非数字值会跳过范围过滤并记录告警日志。
- 修复用户批量导入静默失败的问题：导入异常现在会记录告警日志，并在返回结果中包含每条失败记录的账号和错误原因。

### 验证

- 新增 SSO nonce 校验、id_token claims 校验和 token 过期时间单元测试。
- 新增 SSO id_token JWKS 签名验证单元测试（有效签名通过、篡改签名拒绝、异密钥拒绝、过期拒绝）。
- 新增 resourceScopeSql 白名单和列名校验单元测试。
- 新增用户导入异常收集和日志记录单元测试。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.18` 和 `ghcr.io/sevoniva/crest-web:v1.5.18`。
- 默认离线包为 `crest-offline-v1.5.18-linux-amd64.tar.gz` 和 `crest-offline-v1.5.18-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.18-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.18-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.18.tar.gz`。
- 从 v1.5.17 升级到 v1.5.18 时只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.17 - 2026-07-09

v1.5.17 是 v1.5.16 之后的 1.5 LTS 补丁版本，修复明细表单元格合并的一个缺陷。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复明细表（table-info）单元格合并缺陷：当某列在所有行的值完全相同，但前置维度列的值变化把数据切成了多段时，该列无法合并。现在值完全相同的列会跨前置列隔断合并整列；值不完全相同的列仍保留原有层级上下文，不连续的相同值不会被跨段合并。

### 验证

- 新增并更新 `tableMergeCells` 单元测试，覆盖全相同列跨隔断合并、不全相同列保留上下文、末列被前置不连续列隔断等场景。
- 在本地部署环境验证多列明细表的合并行为。

## v1.5.16 - 2026-07-05

v1.5.16 是 v1.5.15 之后的 1.5 LTS 安全补丁版本，重点收敛数据导入、文件上传、跨窗口通信和依赖组件相关风险。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 加固 Excel/CSV 数据源导入链路，生成引擎表、字段和视图 SQL 时统一校验标识符并使用参数绑定，避免导入内容影响 SQL 结构。
- 收紧只读 SQL 校验返回值，统一使用规范化后的单语句 SQL，继续拦截多语句和非只读语句。
- 加固用户批量导入文件校验，管理员上传前会校验文件名、大小、后缀和 xlsx、xls、csv 内容特征。
- 加固静态资源、字体、驱动 JAR、数据源 Excel 和应用模板上传校验，覆盖文件名、大小、后缀、内容签名和 SVG 安全边界。
- 修复图表动态时间、柱状范围数据、Excel 解析结果和汇总导出中的空值路径，避免异常输入导致空指针或导出失败。
- 修复大屏跳转和嵌入通信中的 URL 来源校验问题，跨窗口消息和返回跳转统一限制在同源或显式允许来源内。
- 修复可视化详情、模板导入导出和应用导出相关接口的访问范围校验缺口，避免跨资源读取。
- 调整源码分析配置，排除构建产物、生成静态资源、第三方编辑器资源和目标目录，避免把非源码文件作为业务代码分析。

### 依赖

- 将 `logback-core` 升级至 `1.5.33`。
- 保持 `mysql-connector-j` 为 `8.2.0`、`netty-handler` 为 `4.2.15.Final`、`jackson-databind` 为 `2.22.0`，并补充 Jackson 反序列化安全默认测试。
- 更新前端富文本和颜色处理相关依赖锁定，移除不再需要的本地补丁。

### 验证

- 建立完整安全审计台账，确认问题已修复或加固，非可执行项均保留证据分类。
- 执行后端安全回归矩阵，覆盖 SQL 策略、Excel 字段、上传校验、静态资源、可视化权限、字体上传、图表汇总、用户导入和 JSON 反序列化安全默认值。
- 执行前端安全回归测试，覆盖同源 URL、静态资源上传、国际化 key、语言策略和汇总列配置。
- 执行前端 TypeScript 检查、生产依赖审计和源码安全规则检查。
- 核查 Maven 依赖树，确认关键运行时依赖版本与安全目标一致。

## v1.5.15 - 2026-07-04

v1.5.15 是 v1.5.14 之后的 1.5 LTS 稳定补丁版本，修复大屏发布、模板导入后静态资源、背景图片保存和自定义国际化资源初始化相关问题。本版本没有新增数据库结构，无需执行升级 SQL。

### 修复

- 修复大屏发布时提示"资源不存在"的问题。发布流程在同一事务内会先删除 `bi_visualization` 主表记录，再从 `bi_visualization_snapshot` 镜像表恢复主表；恢复权限校验调整为主表优先、镜像表兜底，保留创建者权限校验并兼容发布事务。
- 修复大屏保存发布状态时未带图表类型可能导致权限资源类型被错误写回的问题。
- 修复模板导出未完整收集画布、组件、组合、Tab 和嵌套配置中静态资源地址的问题，避免模板导入到其他环境后背景图、媒体图片或其他静态资源缺失。
- 修复上传背景图片后保存不稳定的问题，避免取消选择或空回调覆盖已上传图片。
- 修复 `/api/v1/static-resource/**` 静态资源访问目录与上传、模板导入保存目录不一致的问题。
- 修复未配置自定义前端国际化资源目录时，页面初始化请求 `/sys-parameter/i18n-options` 可能提示"国际化资源目录无效"的问题。

### 验证

- 增加大屏发布恢复、图表类型保持、自定义国际化目录缺失和自定义语言包识别后端回归测试。
- 增加静态资源地址收集、背景图片上传状态和模板导入默认值前端回归测试。
- 执行后端定向测试、前端定向测试、前端类型检查和浏览器发布流程验证。

## v1.5.14 - 2026-07-03

v1.5.14 是 v1.5.13 之后的 1.5 LTS 稳定补丁版本，覆盖安全加固、公共分享内部跳转、查询条件、表格汇总、模板导入导出、画布组合编排和交付包稳定性。本版本没有新增数据库结构；初始化数据仅清理模板中心访问密钥的旧默认值，已有环境升级时需要执行对应升级 SQL。

### 修复

- 修复多处文件路径、上传文件名、静态资源、字体文件和导出文件访问边界不完整的问题。
- 收紧数据源 SQL 执行校验，拦截多语句和非只读语句，同时保留字符串、注释和字段引用中的合法分号。
- 修复可视化、图表、联动、外部参数、模板、用户管理、分享票据、收藏、数据集字段和 SQL 日志相关接口的权限与参数校验缺口。
- 修复 WebSocket 握手信任请求参数 `userId` 的问题，改为使用当前登录用户身份。
- 收敛登录回跳、SSO 回跳、代理头回调地址、SSRF URL、异常返回、响应头、SVG 静态资源和前端跨窗口通信等安全边界。
- 修复富文本字段值、字段占位名称、图表提示文本、字体样式和 SVG 配置写入前的编码/转义边界。
- 修复兼容登录配置固定默认密码、客户端 IP 头未净化、Excel/CSV 解析和导出资源释放不完整的问题。
- 修复无密码分享链接签名密钥使用公开固定默认值的问题，未显式配置时改为服务端密钥派生或安全随机值。
- 移除管理端默认密码查询接口；新建本地用户后不再回显默认密码，重置密码仍返回本次生成密码，便于管理员交付。
- 修复后端打包前未清理旧前端静态资源的问题，避免历史 chunk 混入交付 JAR。
- 清理模板中心访问密钥的安装默认值，避免新环境携带公开固定值；升级 SQL 只清理仍保持旧默认值的记录。
- 更新后端和前端存在安全告警的依赖版本，其中 `jackson-databind` 升级至 `2.22.0`，`netty-handler` 升级至 `4.2.15.Final`。
- 固定 GitHub Actions 第三方 Action 到提交 SHA，增加 npm/pnpm 包发布时间等待策略，并收紧 Nginx Host 与 WebSocket 升级头转发。
- Kubernetes 清单移除 ConfigMap 中的敏感占位字段，改由 Secret 和标准环境变量注入，并补齐容器安全上下文、只读根文件系统和资源限制。
- 修复公共分享或嵌入报表中，数据大屏 A 跳转到已公开分享的数据大屏 B 时可能提示 `link token invalid` 的问题。
- 修复 hash 路由和嵌入场景下查询参数读取不完整，可能导致复制、跳转、分享或导入参数丢失的问题。
- 修复时间范围查询只填写单侧时间时，前后端仍按完整区间处理导致无法查询单侧边界数据的问题。
- 修复查询条件默认值里的固定日期只能选择不能直接输入的问题，支持手动输入 `YYYY-MM-DD` 格式日期。
- 修复日期动态值“今年”等规则在末尾条件中可能被判定为空条件的问题。
- 修复明细表和普通表的表头排序在部分事件对象下只显示空浮层、不触发排序的问题。
- 修复表格导出汇总行在无维度列时可能用汇总标签覆盖首个指标汇总值的问题。
- 修复表格汇总行启用“总计”时可能影响图表数据加载的问题。
- 修复模板导出后再导入时，历史或不完整组件配置可能导致编辑器空白、样式异常或静态资源缺失的问题。
- 修复模板导入流程依赖新窗口打开导致被浏览器拦截的问题，改为当前页内完成文件选择和后续编辑跳转。
- 修复模板导出未完整收集组合、Tab 和嵌套组件中的图片、视频、字体等静态资源的问题。
- 修复画布组合相关配置面板中英文 key 泄露、文字重叠、背景色不一致和配置入口过于突出的展示问题。
- 修复 Element Plus 二次封装在颜色选择器和空子节点场景下的兼容警告，避免配置面板出现无效提示和异常样式。

### 改进

- 查询组件时间范围支持只填写开始时间或结束时间，分别用于查询某个时间之后或之前的数据。
- 查询组件固定日期支持直接键盘输入，保留选择器输入方式，便于录入精确日期。
- 明细表和普通表的已有列汇总支持求和、平均值、最大值、最小值和自定义公式。
- 明细表支持新增虚拟汇总指标列，明细行显示空值占位，汇总行显示按过滤后全量数据计算的结果，并在预览、分享、嵌入和导出中保持一致。
- 表格汇总列支持配置名称、指标字段、公式、数字格式、单位和空值展示，配置项在编辑器中使用中文文案展示。
- 新增背景卡片能力，用于把多个图表或组件呈现为统一视觉区块；保留历史组合组件兼容，并支持无缝布局、背景色和层级配置。
- 预览、分享、嵌入、导出和画布编辑态统一处理查询组件、组合组件、背景卡片和图表默认颜色，减少不同入口的展示差异。

### 验证

- 增加路径访问、SQL 单语句策略、SSRF、异常信息脱敏、WebSocket 身份、用户角色绑定和接口权限边界回归测试。
- 增加公共分享内部跳转 URL 构造、hash 查询参数读取、跳转配置 mapper 和嵌入参数传递回归测试。
- 增加时间单侧过滤 SQL、动态日期规则、固定日期手动输入和查询条件默认值回归测试。
- 增加表格汇总公式优先级、虚拟汇总指标列、汇总行高度、表头排序浮层和导出汇总数据回归测试。
- 增加模板本地导入流程、历史模板兼容、嵌套静态资源收集和导入默认值补齐回归测试。
- 增加背景卡片、组合组件、画布组件默认值、图表颜色默认值和过滤后 Excel 导出回归测试。
- 执行后端安全定向测试、报表功能定向测试和完整测试。
- 执行前端类型检查、lint、生产构建、全量依赖审计和生产依赖审计。
- 核查后端关键依赖版本，确认 `jackson-databind`、`netty-handler` 已收敛到目标版本，且未引入 `log4j:log4j` 1.x 和 `io.netty:netty-codec`。
- 渲染 Kubernetes 四套部署 overlay，并确认渲染后的 YAML 可离线解析。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.14` 和 `ghcr.io/sevoniva/crest-web:v1.5.14`。
- 默认离线包为 `crest-offline-v1.5.14-linux-amd64.tar.gz` 和 `crest-offline-v1.5.14-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.14-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.14-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.14.tar.gz`。
- 从 v1.5.13 或更早 v1.5.x 版本升级到 v1.5.14 时，请先执行适用的升级 SQL，再替换前后端镜像。

## v1.5.13 - 2026-06-26

v1.5.13 是 v1.5.12 之后的稳定补丁版本，重点修复大屏和仪表盘编辑态配置跳转设置时，目标图表列表仍读取正式表数据导致未发布图表为空或不可选的问题。本版本没有新增数据库结构、系统参数或初始化数据变更。

### 修复

- 修复大屏和仪表盘编辑态配置跳转设置时，目标图表列表可能为空或展示旧图表的问题。
- 修复目标图表列表为空时，追加外部参数或查询组件可能触发前端异常的问题。
- 收紧跳转目标图表过滤逻辑，按组件配置中的图表 ID 精确匹配，避免字符串包含导致图表误入列表。

### 验证

- 增加跳转设置编辑态使用 snapshot 数据源的契约测试。
- 增加大屏组件配置图表 ID 解析和目标图表精确过滤回归测试。
- 执行后端定向 Maven 测试验证接口契约和过滤逻辑。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.13` 和 `ghcr.io/sevoniva/crest-web:v1.5.13`。
- 默认离线包为 `crest-offline-v1.5.13-linux-amd64.tar.gz` 和 `crest-offline-v1.5.13-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.13-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.13-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.13.tar.gz`。
- 从 v1.5.12 升级到 v1.5.13 时只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.12 - 2026-06-24

v1.5.12 是 v1.5.11 之后的稳定补丁版本，重点修复数据大屏、表单组件和查询框等前端组件配置上传背景图片后展示不生效的问题。本版本没有新增数据库结构、系统参数或初始化数据变更。

### 修复

- 修复数据大屏画布配置上传背景图片后，预览和展示样式没有稳定应用背景图的问题。
- 修复表单组件、查询框及通用组件背景配置使用外部图片时，背景图可能被组件内部样式覆盖的问题。
- 修复标题类组件背景图使用 CSS `background` 简写时，上传图片路径包含空格等字符可能解析不稳定的问题。

### 验证

- 增加组件通用背景和模糊背景图片样式回归测试。
- 增加数据大屏画布上传背景图片样式回归测试。
- 执行前端类型检查和基础构建验证。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.12` 和 `ghcr.io/sevoniva/crest-web:v1.5.12`。
- 默认离线包为 `crest-offline-v1.5.12-linux-amd64.tar.gz` 和 `crest-offline-v1.5.12-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.12-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.12-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.12.tar.gz`。
- 从 v1.5.11 升级到 v1.5.12 时只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.11 - 2026-06-14

v1.5.11 是 v1.5.10 之后的稳定补丁版本，重点修复 Excel 数据源导入、数据集字段转换、应用模板导入映射和跳转设置相关问题。本版本没有新增数据库结构、系统参数或初始化数据变更。

### 修复

- 修复 Excel 数据源字段名较长、包含中文、重复列或空值占位符时，后续数据集转换和预览可能生成不稳定字段别名的问题。
- 修复 Excel 字段类型推断在整数、小数、日期文本、空值占位符和混合文本场景下不一致的问题。
- 修复基于 Excel 数据源的数据集执行日期函数转换时，目标数据库缺少 Crest 自定义函数导致 SQL 执行失败的问题。
- 修复数据集选择字段时，未持久化的字段 ID 为空时可能误继承其他字段物理别名的问题。
- 修复 SQL 单语句校验把字符串、注释或反引号字段中的分号误判为多语句的问题，同时继续拦截真正的多语句执行。
- 修复应用模板导入保存时，Excel 数据源对应的物理表名没有同步替换到数据集 SQL，导致导入后数据集无法直接运行的问题。
- 修复应用模板导入过程中进行 ID 映射时，Base64 编码的原始 SQL 被误替换的问题。
- 修复应用模板导入后组件 ID 类型不一致，导致部分图表数据和跳转配置无法稳定匹配的问题。
- 修复大屏或仪表盘“跳转设置”读取字段信息时，后端把字段物理类型当成字段分类读取，导致页面报“系统内部错误”的问题。
- 修复前端构建依赖安全告警，更新相关依赖锁定版本。

### 验证

- 增加 Excel 表头识别、字段名生成、字段类型推断、空值占位符和日期文本转换测试。
- 增加 Excel 字段转换 SQL、日期函数翻译和数据集 SQL 预览测试。
- 增加应用模板导入时数据源表名映射、数据集 SQL 映射、组件 ID 映射和内部跳转配置回归测试。
- 增加 SQL 单语句校验和跳转设置字段映射回归测试。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.11` 和 `ghcr.io/sevoniva/crest-web:v1.5.11`。
- 默认离线包为 `crest-offline-v1.5.11-linux-amd64.tar.gz` 和 `crest-offline-v1.5.11-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.11-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.11-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.11.tar.gz`。
- 从 v1.5.10 升级到 v1.5.11 时只需要替换前后端镜像，不需要新增升级 SQL。

## v1.5.10 - 2026-06-13

v1.5.10 是 v1.5.8 之后的稳定补丁版本，合并菜单国际化、应用模板本地导入和应用模板导入导出完整性修复。v1.5.9 不作为稳定交付版本；已有 v1.5.8 环境应直接升级到 v1.5.10。本版本没有新增数据库结构、系统参数或初始化数据变更。

### 新增

- 数据大屏和仪表盘资源列表支持“导入应用模板”，可上传 `.DET2` 或 `.DET2APP` 文件并进入编辑器。
- 应用模板导入后保留数据源、数据集、图表、联动和下钻配置，保存时继续使用现有应用映射流程绑定目标环境资源。

### 修复

- 修复后端默认语言包缺少菜单 key 时，工作台、数据准备、系统管理等菜单显示 `i18n_menu.*` 的问题。
- 补齐简体中文、繁体中文、英文和默认语言包的菜单翻译，并增加菜单语言包完整性测试，防止后续新增菜单漏配兜底文案。
- 修复 `.DET2`、`.DET2APP` 文件无法通过本地文件导入创建数据大屏或仪表盘的问题。
- 修复外部应用模板导入时未保留模板版本和应用映射数据，导致数据源、数据集映射流程无法继续的问题。
- 修复历史模板缺少 `scaleHeight` 时导入后画布高度缩放可能异常的问题。
- 修复 `.DET2APP` 应用模板导入保存时只保留外部链接，内部大屏跳转配置未写入的问题。
- 修复应用模板导入后跳转源图表 ID 未按新图表 ID 映射，导致点击指标无法匹配跳转配置的问题。
- 修复应用模板导入后跳转目标字段配置未保存的问题。
- 修复应用模板导入未完成数据源或数据集匹配时返回“系统内部错误”的问题，改为返回明确的匹配提示。
- 修复刚导入但未发布的大屏或仪表盘再次导出应用模板时，图表和跳转配置可能缺失的问题。

### 验证

- 增加菜单语言包完整性单元测试。
- 增加 `.DET2`、`.DET2APP` 本地导入解析单元测试。
- 增加应用模板内部跳转导入保存单元测试。
- 验证应用模板数据源映射、数据集映射、内部跳转导出和保存流程。
- 验证刚导入未发布资源的应用模板导出流程。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.10` 和 `ghcr.io/sevoniva/crest-web:v1.5.10`。
- 默认离线包为 `crest-offline-v1.5.10-linux-amd64.tar.gz` 和 `crest-offline-v1.5.10-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.10-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.10-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.10.tar.gz`。
- 从 v1.5.8 升级到 v1.5.10 时只需要替换前后端镜像，不需要新增升级 SQL。
- 从 v1.5.7 升级到 v1.5.10 时，需要先执行 `upgrade-sql/v1.5.7_to_v1.5.8.sql`，再替换 v1.5.10 前后端镜像。

## v1.5.8 - 2026-06-13

v1.5.8 是 v1.5 系列的数据资产和缓存管理版本，新增数据资产中心、缓存任务中心，完善数据集缓存、用户管理、Excel 数据源、公开分享下钻和数据准备表格能力。已有 v1.5.x 环境升级时请先备份元数据库和运行目录，并执行 `upgrade-sql/v1.5.7_to_v1.5.8.sql`。

### 新增

- 新增数据资产中心，统一查看数据源、数据表、数据集、仪表盘、大屏和分享资源，支持负责人、说明、标签、更新时间、使用次数和上下游影响信息。
- 新增数据集治理状态，数据集可标记为已认证、推荐使用或已废弃；已废弃状态与已认证、推荐使用互斥。
- 新增缓存任务中心，集中查看数据集缓存任务、最近执行状态、任务耗时、失败原因和手动更新入口。
- 新增数据资产菜单和缓存任务菜单，安装和升级 SQL 会同步写入系统菜单与默认角色权限。
- 用户管理支持新建或编辑本地账号时设置密码，重置本地密码后返回本次生成的新密码，便于管理员交付给用户。
- Excel 数据源导入支持跳过首行疑似 SQL 说明或无效表头的工作表，并保留可导入工作表。

### 优化

- 数据集缓存设置页优化未保存配置、增量不可用、立即更新和定时更新的状态提示，减少“未缓存”“0 行”等中间状态造成的误解。
- 缓存任务点击“立即更新”后进入轮询，任务状态可自动刷新；短暂刷新失败会重试，不需要手动刷新页面才能看到结果。
- 数据资产页、数据集页、数据源页和详情页表格统一使用 `crest-data-table` 样式，数据准备模块视觉风格保持一致。
- 数据门户和大屏、仪表盘展示页不再向普通访问用户展示缓存技术状态，缓存状态集中在管理端查看。
- 公开分享页中的内部下钻保持在分享上下文内打开，支持弹窗查看目标资源并下载明细数据。
- Excel 数据源使用安全的物理字段名保存长表头、中文表头和重复表头，前端仍展示原始业务字段名。
- 明细表序号列改为独立序号字段，开启表头说明、表头分组、合计行或合并单元格时不再占用第一列业务字段。
- 上传文件大小限制支持通过 `CREST_UPLOAD_MAX_FILE_SIZE` 和 `CREST_UPLOAD_MAX_REQUEST_SIZE` 配置，超出限制时返回明确提示。
- 前端构建产物文件名包含版本号和内容 hash，降低升级后浏览器缓存旧资源的概率。

### 修复

- 修复数据资产筛选在部分资产类型下报错的问题。
- 修复数据资产负责人、治理状态和资产说明在非管理员但有资产维护权限时无法保存的问题。
- 修复数据资产详情页多个 tab 样式与列表页不一致的问题。
- 修复数据资产 SQL 按固定组织读取资源，导致管理员也可能无法编辑部分资产的问题。
- 修复缓存任务轮询遇到短暂接口错误后过早停止的问题。
- 修复数据资产查询在不同表字符集或排序规则不一致时可能报错的问题。
- 修复公开分享页点击内部下钻后跳到受后台权限保护页面，导致未登录访问需要重新登录的问题。
- 修复分享弹窗下载按钮不可用或导出空数据的问题。
- 修复 Excel 文件中说明页首行包含长 SQL 文本时，导入报字段名过长或编辑时报未知列的问题。
- 修复上传文件超出限制时返回通用内部错误的问题。
- 修复放大弹窗中图表样式调整后无法按原事件重新渲染的问题。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.8` 和 `ghcr.io/sevoniva/crest-web:v1.5.8`。
- 默认离线包为 `crest-offline-v1.5.8-linux-amd64.tar.gz` 和 `crest-offline-v1.5.8-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.8-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.8-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.8.tar.gz`。
- 本版本包含数据库变更；已有环境升级时需要执行 `upgrade-sql/v1.5.7_to_v1.5.8.sql`。

## v1.5.7 - 2026-06-10

v1.5.7 是 v1.5 系列补丁版本，修复公开分享链接、数据集缓存、API 数据源和数据源本地校验相关问题。已有 v1.5.6 环境升级时只需替换前后端镜像，不需要执行新增升级 SQL。

### 修复

- 修复已登录后台用户访问 `/link/:uuid` 公开分享链接时，页面被后台菜单权限校验重定向到工作台的问题；登录和未登录访问同一分享链接时展示保持一致。
- 登录态下，公开分享、预览等只读路由优先按只读访问策略放行；工作台、数据源、数据集等后台路由仍按菜单权限校验。
- 数据集缓存支持保存配置后立即更新，并在开启缓存前校验数据集状态；“立即更新”在提交同步任务前保存当前配置。
- 数据集缓存任务保留最近执行状态、缓存可用状态和定时任务状态；切换增量字段或更新类型时清理旧增量水位，防止后续同步沿用过期状态。
- 数据集缓存校验结果改为“源数据与缓存一致”“数据一致性校验失败”等状态描述。
- 修复 API 数据源使用 JsonPath 时，查看数据结构仍按整个响应解析，导致 `$data.dataList` 下的子节点不能按行正确识别的问题。
- API 数据源启用 JsonPath 后支持继续编辑字段选择，用户可选择写入缓存表的字段。
- 修复数据源详情页对 19 位雪花 ID 调用 `Number(...)` 导致精度丢失的问题；数据源校验、编辑等接口会使用原始字符串 ID。
- 修复数据源详情页校验在异常节点或旧状态下可能抛出“系统内部错误”的问题，缺失数据源时返回明确提示。
- MySQL 数据源查询时动态注册 Calcite schema，避免数据集、缓存或预览使用新建数据源时找不到 schema；不可用数据源会跳过并记录原因。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.7` 和 `ghcr.io/sevoniva/crest-web:v1.5.7`。
- 默认离线包为 `crest-offline-v1.5.7-linux-amd64.tar.gz` 和 `crest-offline-v1.5.7-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.7-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.7-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.7.tar.gz`。
- 本版本没有数据库结构、系统参数或初始化数据变更；从 v1.5.6 升级到 v1.5.7 时不需要新增升级 SQL。

## v1.5.6 - 2026-06-10

v1.5.6 是 v1.5 系列的数据大屏资源权限补丁版本，重点修复后台资源树中只读资源错误展示复制等管理入口、授权管理者复制数据大屏被创建人校验拦截、复制请求状态字段不兼容、数据集导出失败缺少提示、明细表合并单元格不可按需选择，以及本地开发端口变化后登录被 CORS 拦截的问题。已有 v1.5.5 环境升级时只需替换前后端镜像，不需要执行新增升级 SQL。

### 修复

- 修复数据大屏和仪表盘资源树将只读可见资源按可管理资源返回的问题；只读资源不再展示复制、移动、重命名、删除等管理操作。
- 修复非创建人但已获得资源管理权限时复制数据大屏或仪表盘会被服务端创建人校验拦截，导致前端提示“系统内部错误，请联系管理员”的问题。
- 修复资源树复制数据大屏或仪表盘时，前端携带布尔型发布状态导致后端参数解析失败，页面提示“系统内部错误，请联系管理员”的问题。
- 修复本地开发模式登录后 WebSocket 代理地址解析异常导致页面白屏的问题。
- 修复数据集导出请求被取消、后端返回 JSON 错误或异常响应时，前端仍可能关闭弹窗且不给出明确失败反馈的问题；导出确认按钮在请求处理中会禁用，避免重复点击。
- 明细表“合并单元格”支持选择合并字段。可选字段为当前明细表第一个指标字段之前的维度字段；指标字段不参与合并。
- 合并规则改为按上下相邻且值相同的单元格纵向合并；选择后置维度时，会在前置维度相同的范围内合并，避免不同上级分类下的同名值被合到一起。
- 清空“合并字段”表示不合并任何字段；历史图表未保存合并字段时，继续沿用原来的默认合并规则。
- 修复快速增删合并字段时页面卡顿、不同上级分类下的同名值被合并、序号列跟随自定义字段合并后展示混乱的问题。
- 统一 `/login` 登录入口，本地账号和管理员应急登录都从该入口进入；旧 token 损坏或过期时，会清理登录态并回到登录页。
- 修复本地开发模式下 Vite 端口从 8080 切换到 8081 等端口后，登录请求被后端 CORS 拦截并返回 403 的问题。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.6` 和 `ghcr.io/sevoniva/crest-web:v1.5.6`。
- 默认离线包为 `crest-offline-v1.5.6-linux-amd64.tar.gz` 和 `crest-offline-v1.5.6-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.6-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.6-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.6.tar.gz`。
- 本版本没有数据库结构、系统参数或初始化数据变更；从 v1.5.5 升级到 v1.5.6 时不需要新增升级 SQL。
- 从 v1.5.4 升级到 v1.5.6 时，请先执行 `upgrade-sql/v1.5.4_to_v1.5.5.sql`，再替换 v1.5.6 前后端镜像。

## v1.5.5 - 2026-06-06

v1.5.5 是面向数据门户和可视化交付场景的功能修复版本。版本提供独立数据门户，支持普通用户访问已发布资源、门户预览筛选、数据大屏导出、表格表头说明、账号默认密码管理和离线升级 SQL。已有 v1.5.4 环境升级时，请同时替换前后端镜像并执行离线包内的升级 SQL。

### 新增

- 新增数据门户后端接口和前端页面，支持概览统计、我的收藏、全部资源、最近使用、关键字搜索、资源类型筛选、分页、排序和收藏切换。
- 新增门户资源查看页，普通用户可在授权范围内查看已发布的数据大屏和仪表盘；页面提供返回、刷新、全屏和账号菜单。
- 新增门户资源权限判断，按创建人、默认组织、用户/组织/角色资源授权计算可读范围；管理员和系统管理员保留后台入口。
- 新增数据大屏“导出按钮”组件，支持绑定表格视图，配置按钮文案、导出范围和透视表带格式导出。
- 新增表格表头说明配置，支持说明行高度、字号、颜色和对齐方式；表头说明参与复制和 Excel 导出。
- 新增账号安全参数分组，包含默认密码、密码策略、禁用初始密码、密码有效期、登录限制次数和限制时间。
- 新增 `/user/default-password` 管理端接口；新建本地用户和重置密码后展示默认密码，并支持复制。
- 新增 `upgrade-sql/v1.5.4_to_v1.5.5.sql`，离线包同步携带 SQL 和执行说明。
- Release 附件新增 `crest-source-v1.5.5.tar.gz` 源码包，并纳入 `crest-v1.5.5-checksums.txt`。
- Release 附件新增 `crest-app-images-v1.5.5-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.5-linux-arm64.tar.gz` 应用镜像升级包；包内包含前端镜像、后端镜像和增量升级 SQL。

### 变更

- 登录默认入口改为 `/portal`；`/admin-login` 仍面向管理员进入后台，普通用户访问后台路由会回到门户。
- 当前用户信息增加 `admin` 和 `backendAccess` 标识，前端据此控制后台入口、快捷创建和只读路由。
- 系统名称统一来自站点设置；浏览器标题不再自动添加 `Crest-` 前缀，favicon 使用 Crest 标识。
- 门户首页默认展示“我的收藏”，资源列表列调整为名称、类型、更新时间和操作。
- 数据门户和门户查看页顶栏统一 logo、系统名称、资源标题和账号区布局。
- 数据集枚举值、树形筛选和级联筛选请求携带可视化资源 ID，用于门户预览场景下的只读授权。
- 导出中心分页和状态参数兼容 `undefined`、`null` 等异常值；下载票据校验改为基于任务、用户和票据时间。
- 分离部署示例不再提供固定默认密码和固定加密参数，启动时必须显式设置敏感环境变量。

### 修复

- 修复普通用户在门户预览中打开大屏筛选下拉后无可选项的问题。
- 修复数据大屏按筛选结果导出 Excel 时未带入当前筛选上下文的问题。
- 修复导出中心下载链接校验过严、任务有效期为空和导出任务分页异常问题。
- 修复表格复制时表头层级、说明行和富文本单元格处理不完整的问题。
- 修复表格多级表头、隐藏字段和说明字段导致的 Excel 表头错位问题。
- 修复数据集自定义 SQL 保存按钮、编辑器拖拽插入、复制数据集路由参数和资源树默认排序异常。
- 修复已发布资源预览时使用后台资源树权限导致普通用户无法读取的问题。
- 修复嵌入式 token 参数读取路径和旧参数格式兼容问题。
- 修复旧版可视化组件名称在重命名后无法加载的问题。
- 修复审计日志对新路由、权限接口、导出下载和用户密码操作的描述不准确问题。
- 修复可视化编辑器、查询组件、联动配置、颜色配置、钻取、动态图表和移动端筛选组件在空数据或历史配置下的运行时异常。

### 安全与质量

- 远程下载 URL 增加协议和地址校验，默认阻止本地、私有和组播地址。
- 文件上传、读取和删除路径使用规范化校验，避免路径越界。
- 跨源和查询取数字段场景增加单语句、只读 SQL 校验。
- SQL 占位符、schema alias 和标识符替换改为边界感知处理，避免误替换。
- 增加 API 路由合同、门户权限、嵌入 token、表头说明、导出中心、SQL 兼容和前端构建警告相关测试。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.5` 和 `ghcr.io/sevoniva/crest-web:v1.5.5`。
- 默认离线包为 `crest-offline-v1.5.5-linux-amd64.tar.gz` 和 `crest-offline-v1.5.5-linux-arm64.tar.gz`。
- 应用镜像升级包为 `crest-app-images-v1.5.5-linux-amd64.tar.gz` 和 `crest-app-images-v1.5.5-linux-arm64.tar.gz`。
- 默认源码包为 `crest-source-v1.5.5.tar.gz`。
- 离线包内包含 `upgrade-sql/v1.5.4_to_v1.5.5.sql`；从 v1.5.4 升级时请先备份元数据库，再执行该文件。
- 全新环境可直接安装 v1.5.5；v1.5.4 升级到 v1.5.5 时请替换前后端镜像并执行升级 SQL，更早版本迁移前请先备份元数据库和运行目录，导出业务资源，再在新环境中导入。

## v1.5.4 - 2026-06-02

v1.5.4 是 v1.5 系列的可观测性增强版本，面向企业内网部署和统一监控接入场景。此版本默认不暴露指标；需要监控时可通过参数开启 Prometheus metrics，并使用 Bearer Token 保护抓取入口。

### 新增

- 后端接入 Spring Boot Actuator 和 Micrometer Prometheus registry，提供 `/api/v1/actuator/prometheus` 指标端点。
- 指标端点默认关闭；开启后必须配置 `CREST_PROMETHEUS_TOKEN`，Prometheus 通过 Bearer Token 抓取。
- 新增 `CREST_PROMETHEUS_ENABLED`、`CREST_PROMETHEUS_TOKEN`、`CREST_PROMETHEUS_MAX_URI_TAGS`、`CREST_GRAFANA_ENABLED` 等部署参数。
- 新增 Grafana provisioning，看板覆盖接口吞吐、错误率、P95 延迟、状态码分布、Top 接口、接口明细、JVM、GC、运行时线程池、CPU 和采集状态。
- 新增 Prometheus 告警规则，覆盖服务采集中断、API 路由清单缺失、HTTP 5xx 错误率、P95 延迟、CPU、JVM 堆内存、线程池队列和数据库连接池。
- 新增单机 Compose observability profile，可选启动 Prometheus 和 Grafana。
- 新增 Kubernetes observability overlay，支持内置 MySQL 和外部 MySQL 两种部署形态。
- 系统参数页新增“可观测性”页签，管理员可查看 Prometheus/Grafana 启用状态、指标端点、token 配置状态和看板信息。

### 调整

- 前端网关默认屏蔽 `/api/v1/actuator/prometheus`，指标只允许 Prometheus 从后端内网地址抓取。
- Grafana 状态码分布、接口吞吐 Top 20、接口 P95 Top 20 等面板增加空数据兜底，低流量窗口不再显示 No data。
- 离线包制作脚本支持按需打入 Prometheus 和 Grafana 镜像，适配无公网环境。
- 文档补充企业已有 Prometheus/Grafana 时的接入方式，可直接启用 Crest metrics 并导入 Grafana 看板和 Prometheus 告警规则。
- 后端运行镜像补齐 `jdk.management` 模块，确保 JVM、GC、线程和 CPU 指标完整输出。
- README、安装文档、Kubernetes 文档、工作流默认镜像标签和工程版本号更新至 v1.5.4。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.4` 和 `ghcr.io/sevoniva/crest-web:v1.5.4`。
- 默认离线包为 `crest-offline-v1.5.4-linux-amd64.tar.gz` 和 `crest-offline-v1.5.4-linux-arm64.tar.gz`。
- Prometheus/Grafana 默认不启动。需要内置监控组件时设置 `CREST_PROMETHEUS_ENABLED=true` 和 `CREST_GRAFANA_ENABLED=true`。
- 已有企业 Prometheus/Grafana 时，只需开启 `CREST_PROMETHEUS_ENABLED=true`，配置抓取 token，并导入仓库内 Grafana 看板和 Prometheus 告警规则。
- v1.5.4 面向全新环境安装；早期环境迁移前请先备份元数据库和运行目录，导出业务资源，再在新环境中导入。

## v1.5.3 - 2026-06-02

v1.5.3 是 v1.5 系列的部署形态升级版本。发布时后端服务镜像和前端网关镜像分别构建，Kubernetes 清单也按前端、后端和 MySQL 拆分探针与资源边界。

### 调整

- Kubernetes 应用清单拆分为 `crest-service` 后端 Deployment 和 `crest` 前端 Deployment。
- 前端网关使用独立 Nginx 配置，保留静态资源缓存、API 转发、WebSocket 转发和安全响应头。
- 后端 readiness 检查元数据库连接，liveness 只检查应用进程响应；前端通过 `/healthz` 检查 Nginx 网关。
- Kubernetes 部署不再由 Kustomize 生成占位 Secret，改为部署前在目标 namespace 创建 `crest-db-secret`。
- README、安装文档、Kubernetes 文档、开发文档和 CI 默认镜像标签更新至 v1.5.3。
- 前端和后端工程版本号更新为 `1.5.3`。

### 修复

- 修复公开分享链接独立打开时被登录态拦截的问题；分享页可无登录进入，并通过受限分享 token 访问被分享资源。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.3` 和 `ghcr.io/sevoniva/crest-web:v1.5.3`。
- 默认离线包为 `crest-offline-v1.5.3-linux-amd64.tar.gz` 和 `crest-offline-v1.5.3-linux-arm64.tar.gz`。
- v1.5.3 面向全新环境安装；早期环境迁移前请先备份元数据库和运行目录，导出业务资源，再在新环境中导入。

## v1.5.2 - 2026-06-01

v1.5.2 是 v1.5 系列安装交付修复版本，重点修正单机离线安装包在全新环境中的数据库初始化流程。已成功运行的 v1.5.1 环境可继续使用；重新部署或分发离线包时建议使用 v1.5.2。

### 修复

- 修复演示资源清理 SQL 被 MySQL 首次初始化流程误执行的问题。
- 修复 `crestctl` 在同时存在 `mysql-crest` 和 `crest` 容器时误判服务状态的问题。

### 调整

- 内置 MySQL 使用 `MYSQL_DATABASE=${CREST_DB_NAME}` 创建元数据库，不再通过初始化 SQL 写死数据库名。
- `cleanup-demo-data.sql` 移至手动维护目录，仅由 `cleanup-demo-data.sh` 调用。
- 离线包制作流程清理 macOS 扩展属性文件，避免包内出现 `._*` 或 `.DS_Store`。
- 文档、镜像标签、Kubernetes 示例和安装包示例统一更新至 v1.5.2。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest:v1.5.2`。
- 默认离线包为 `crest-offline-v1.5.2-linux-amd64.tar.gz` 和 `crest-offline-v1.5.2-linux-arm64.tar.gz`。
- 默认安装加载内置演示资源；生产环境不需要示例数据时，安装前设置 `CREST_LOAD_DEMO=false`。
- 已加载演示资源的单机环境可执行 `bash cleanup-demo-data.sh --yes` 清理。

## v1.5.1 - 2026-06-01

v1.5.1 是 v1.5 系列维护发布，重点整理安装交付、演示资源初始化和公开文档。默认安装仍加载零售经营与研发效能演示资源；生产环境可在安装前关闭。

### 调整

- 内置演示资源迁移到独立目录 `db/demo-migration`，通过 `CREST_LOAD_DEMO` 和 `CREST_FLYWAY_LOCATIONS` 控制是否初始化。
- 单机安装包、Kubernetes 清单和运行配置模板同步增加演示资源开关。
- API 文档页改为 Crest OpenAPI，并按当前产品模块整理接口分组。
- README、安装文档、Kubernetes 文档、开发文档和数据血缘文档补充演示资源开关和清理说明。
- 安装和运维脚本改为安全读取运行目录 `.env`，避免 JDBC 参数中的 `&` 被 shell 截断。
- 演示资源脚本不再写死元数据库名，自定义 `CREST_DB_NAME` 的新安装环境可正常初始化。

### 新增

- `cleanup-demo-data.sh`：单机环境演示资源清理入口。
- `cleanup-demo-data.sql`：固定演示资源和 `crest_demo_retail` 演示库清理 SQL，可按运维流程用于 Kubernetes 环境。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest:v1.5.1`。
- 默认离线包为 `crest-offline-v1.5.1-linux-amd64.tar.gz` 和 `crest-offline-v1.5.1-linux-arm64.tar.gz`。
- 生产环境不需要示例数据时，安装前设置 `CREST_LOAD_DEMO=false`。
- 已加载演示资源的单机环境可执行 `bash cleanup-demo-data.sh --yes` 清理。

## v1.5.0 - 2026-06-01

Crest v1.5.0 面向企业内网和私有化部署场景发布，提供元数据库初始化、内置演示资源、平台管理、单点登录、审计日志、服务治理和国产密码套件适配能力。v1.5.0 不支持从早期开发版本直接执行 Flyway 原地升级；已有环境请先导出业务资源，再在新环境导入。

### 新增

- 数据源、物理表、字段、数据集字段、图表字段、仪表盘和数据大屏的字段级血缘。
- 基于 OceanBase Connector/J 的 OceanBase Oracle 模式数据源。
- 内置零售经营演示库，包含数据源、数据集、图表和数据大屏。
- 内置研发效能分析资源，包含研发经营、需求流动、人力容量、工程活动、质量风险等主题大屏。
- 基于 OIDC / OAuth2 授权码模式的单点登录，包含管理配置、登录入口、一次性回调票据和用户来源标识。
- 统一的平台管理能力，覆盖用户、角色、权限、单点登录、审计日志、站点设置、字体管理和系统参数。
- 服务治理命名规范，接口、表结构、资源类型和对外文档统一使用 Crest 语义。
- 国产密码套件适配配置，支持在配置文件中启用 SM2 / SM3 / SM4 相关能力。
- 支持内置 MySQL 和外部 MySQL 的 Kubernetes 部署清单。
- GHCR 镜像发布 workflow，以及基于 JDK 21 Alpine 构建阶段的精简运行镜像。
- 按 Crest 当前模块分组的运行时 API 文档。

### 调整

- 管理员初始密码由安装脚本或 `CREST_INITIAL_PASSWORD` 配置提供，默认发布包不内置固定弱密码。
- 产品品牌、导航、登录页和 API 文档统一使用 Crest 命名。
- 移除可见导航和文档中未纳入本版本交付范围的入口。
- 内置演示数据源在应用启动时根据当前元数据库连接自动同步，安装 SQL 不写入宿主机地址。
- 工作台页面改为浅色布局，保留现有业务标签页，快捷创建和资源列表沿用系统文案。
- 新安装库只执行两段 Flyway 初始化：`V1.5.0.1__initial_schema.sql` 和 `V1.5.0.2__demo_resources.sql`。

### 修复

- DataV 图表明细弹窗在没有本地缓存时会主动获取图表数据。
- 自定义 SQL 数据集编辑不再请求缺失的 `/sysVariable/query` 路由。
- 大规模血缘图改用 canvas 渲染，并优化尺寸计算，减少模糊和布局抖动。
- 工作台快捷入口和资源面板在加载中保持稳定间距。
- 初始化 SQL 合并为全新安装脚本，清理历史升级脚本中的重复补丁语句。
- 用户姓名增加服务端校验，并在升级时清理异常展示名。
- 最近使用资源查询不再依赖缺失的社区扩展表。
- 登录页按系统配置显示单点登录入口，未启用时保持账号密码登录体验。

### 安装说明

- v1.5.0 按全新安装发布，不支持从早期开发版本直接执行 Flyway 升级。
- 元数据库初始化脚本为 `V1.5.0.1__initial_schema.sql`。
- 内置零售经营与研发效能演示资源脚本为 `V1.5.0.2__demo_resources.sql`。
- 本版本默认镜像为 `ghcr.io/sevoniva/crest:v1.5.0`。
- 离线部署包提供 `linux-amd64` 和 `linux-arm64` 两个架构版本。
- Crest 仍遵循 GPLv3。再分发时请保留上游版权和许可声明。
