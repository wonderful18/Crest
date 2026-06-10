# Changelog

## v1.5.7 - 2026-06-10

v1.5.7 是 v1.5 系列的公开分享链接访问补丁版本，修复已登录后台用户打开公开分享链接时被重定向到工作台的问题。已有 v1.5.6 环境升级时只需替换前后端镜像，不需要执行新增升级 SQL。

### 修复

- 修复已登录后台用户访问 `/link/:uuid` 公开分享链接时，页面被后台菜单权限校验重定向到工作台的问题；登录和未登录访问同一分享链接时展示保持一致。
- 登录态下，公开分享、预览等只读路由会先按只读路由放行；工作台、数据源、数据集等后台路由仍按菜单权限校验。

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
