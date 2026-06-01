# Changelog

## v1.5.4-rc.1 - 2026-06-02

v1.5.4-rc.1 是 v1.5 系列的可观测性候选版本，面向企业内网部署和统一监控接入场景。此版本默认不暴露指标；需要监控时可通过参数开启 Prometheus metrics，并使用 Bearer Token 保护抓取入口。

### 新增

- 后端接入 Spring Boot Actuator 和 Micrometer Prometheus registry，提供 `/api/v1/actuator/prometheus` 指标端点。
- 指标端点默认关闭；开启后必须配置 `CREST_PROMETHEUS_TOKEN`，Prometheus 通过 Bearer Token 抓取。
- 新增 `CREST_PROMETHEUS_ENABLED`、`CREST_PROMETHEUS_TOKEN`、`CREST_PROMETHEUS_MAX_URI_TAGS`、`CREST_GRAFANA_ENABLED` 等部署参数。
- 新增 Grafana provisioning，看板覆盖接口吞吐、错误率、P95 延迟、状态码分布、接口明细、JVM、GC、运行时线程池、CPU 和采集状态。
- 新增单机 Compose observability profile，可选启动 Prometheus 和 Grafana。
- 新增 Kubernetes observability overlay，支持内置 MySQL 和外部 MySQL 两种部署形态。
- 系统参数页新增“可观测性”页签，管理员可查看 Prometheus/Grafana 启用状态、指标端点、token 配置状态和看板信息。

### 调整

- 前端网关默认屏蔽 `/api/v1/actuator/prometheus`，指标只允许 Prometheus 从后端内网地址抓取。
- 离线包制作脚本支持按需打入 Prometheus 和 Grafana 镜像，适配无公网环境。
- 文档补充企业已有 Prometheus/Grafana 时的接入方式，可直接启用 Crest metrics 并导入 Grafana dashboard。
- 后端运行镜像补齐 `jdk.management` 模块，确保 JVM、GC、线程和 CPU 指标完整输出。
- README、安装文档、Kubernetes 文档、工作流默认镜像标签和工程版本号更新至 v1.5.4-rc.1。

### 安装说明

- 默认镜像为 `ghcr.io/sevoniva/crest-service:v1.5.4-rc.1` 和 `ghcr.io/sevoniva/crest-web:v1.5.4-rc.1`。
- 默认离线包为 `crest-offline-v1.5.4-rc.1-linux-amd64.tar.gz` 和 `crest-offline-v1.5.4-rc.1-linux-arm64.tar.gz`。
- Prometheus/Grafana 默认不启动。需要内置监控组件时设置 `CREST_PROMETHEUS_ENABLED=true` 和 `CREST_GRAFANA_ENABLED=true`。
- 已有企业 Prometheus/Grafana 时，只需开启 `CREST_PROMETHEUS_ENABLED=true`，配置抓取 token，并导入仓库内 Grafana dashboard。
- v1.5.4-rc.1 面向全新环境安装；早期环境迁移前请先备份元数据库和运行目录，导出业务资源，再在新环境中导入。

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
