# Crest 开发说明

本文面向参与 Crest 开发、部署和发布的人，说明仓库结构、构建方式、数据库迁移、公开仓库规则和提交前检查。

Crest 基于 DataEase 2.10.22 开源版本继续开发，并合入 2.10.23 相关安全加固和依赖升级。项目继续按 GPLv3 发布。开发时要保留上游版权和许可证声明，不引入不能公开分发的依赖、驱动或数据。

## 目录结构

| 路径 | 说明 |
| --- | --- |
| `core/core-backend` | Spring Boot 后端服务，包含接口、业务实现、Flyway 迁移和最终 JAR 打包入口 |
| `core/core-frontend` | Vue 3.3、Vite、TypeScript、Element Plus、Pinia 和 vxe-table 前端工程 |
| `sdk/api` | 对内 API、DTO、VO 和接口契约 |
| `sdk/common` | 认证、通用模型、工具类、异常处理和 Spring 配置 |
| `sdk/extensions/extensions-datasource` | 数据源扩展、JDBC 数据源定义和方言能力 |
| `drivers` | 随仓库维护的 JDBC 驱动，当前跟踪 `oceanbase-client-2.4.17.jar` |
| `installer` | 单机安装脚本、Docker Compose 模板、控制脚本和离线包制作脚本 |
| `deploy/kubernetes` | Kubernetes 清单，支持内置 MySQL、外部 MySQL 和外部 OceanBase 系统库配置 |
| `docs` | 产品能力、部署运维、开发和发布文档 |
| `.github/workflows/docker-publish.yml` | GHCR 镜像构建和发布流程 |

## 产品边界

Crest 维护以下功能范围：

- 数据源、数据集、数据集缓存同步；
- 图表编辑、仪表盘、数据大屏；
- 字段级数据血缘；
- 分享页、导出中心；
- 登录、用户、系统参数、站点设置、字体管理和关于系统；
- OceanBase Oracle 模式数据源；
- 零售经营和研发效能演示资源。

新增页面、菜单或对外交付能力前，必须确认产品边界、接口权限、部署依赖和运维责任。

“关于系统”由右上角用户菜单提供，只展示版本号、Commit 和构建信息等交付追溯字段，不放置营销文案或未维护入口。

分享模块保留 `core_share`、`CoreShare*`、`ShareTicket` 和 `de-link` 等历史兼容名称。这些名称关联数据库表、接口路径、Mapper 语句和旧分享链接。调整前必须先设计迁移、回滚和链接兼容方案。

## 工具链

| 工具 | 建议版本 |
| --- | --- |
| JDK | 21 |
| Maven | 3.9 或兼容版本 |
| Node.js | 22 |
| pnpm | 11 |
| MySQL | 8.0 或兼容版本 |
| Docker | 20.10+ |
| Docker Buildx | 多架构镜像发布时需要 |

Maven 使用仓库内 `.mvn/settings.xml`。前端依赖使用 `core/core-frontend/pnpm-lock.yaml`，`flushbonading` 子包使用自己的 `package-lock.json`。

不要引入私有制品仓库、个人代理源、无法公开访问的镜像仓库或授权不清的二进制文件。确需新增第三方驱动或前端包时，需要在提交说明中写明用途和许可证。

## Maven 模块命名

构建输出统一使用 `crest-*` 名称。该名称来自各 `pom.xml` 的 `<name>`，不会改变目录、artifactId 或已有依赖坐标。

| 构建模块名 | artifactId | 说明 |
| --- | --- | --- |
| `crest-platform` | `crest` | 根工程 |
| `crest-sdk` | `sdk` | SDK 聚合模块 |
| `crest-sdk-common` | `common` | 通用模型、认证和工具 |
| `crest-extensions` | `extensions` | 扩展聚合模块 |
| `crest-extension-datasource` | `extensions-datasource` | 数据源扩展 |
| `crest-extension-view` | `extensions-view` | 可视化扩展 |
| `crest-api` | `api` | API 聚合模块 |
| `crest-api-base` | `api-base` | 基础接口契约 |
| `crest-api-permissions` | `api-permissions` | 权限接口契约 |
| `crest-api-sync` | `api-sync` | 同步接口契约 |
| `crest-core` | `core` | 应用聚合模块 |
| `crest-web` | `core-frontend` | 前端资源构建 |
| `crest-service` | `core-backend` | 后端服务和最终 JAR |

## 常用命令

编译数据源扩展相关模块：

```bash
mvn -s .mvn/settings.xml -pl sdk/extensions/extensions-datasource -am \
  -DskipTests -Dmaven.test.skip=true -Dmaven.antrun.skip=true \
  test-compile
```

安装前端依赖：

```bash
cd core/core-frontend
pnpm install --frozen-lockfile
```

构建前端：

```bash
cd core/core-frontend
pnpm run build:base
```

前端轻量构建检查：

```bash
cd core/core-frontend
pnpm run build:lite:check
```

打包后端：

```bash
mvn -s .mvn/settings.xml -pl :core-backend -am clean package -Pstandalone -DskipTests -Dmaven.test.skip=true -Dcrest.copy.frontend.skip=true
```

构建本地镜像：

```bash
docker build -f Dockerfile.backend -t ghcr.io/sevoniva/crest-service:local .
docker build -f Dockerfile.frontend -t ghcr.io/sevoniva/crest-web:local .
```

源码方式启动后端：

```bash
export CREST_DB_HOST=127.0.0.1
export CREST_DB_PORT=3306
export CREST_DB_NAME=crest
export CREST_DB_USERNAME=root
export CREST_DB_PASSWORD='<mysql-password>'
export CREST_AES_KEY='<32-character-aes-key>'
export CREST_AES_IV='<16-character-aes-iv>'
export CREST_CRYPTO_MODE='standard'
export CREST_SM4_KEY='<16-character-sm4-key-when-crypto-mode-is-sm-suite>'
export CREST_INITIAL_PASSWORD='<admin-initial-password>'
./run.sh start
```

前端开发服务：

```bash
cd core/core-frontend
pnpm dev
```

## 运行时配置

后端主要配置来自 Spring 配置文件和环境变量。单机安装时由 `installer/install.sh` 根据 `installer/install.conf` 生成运行目录配置；Kubernetes 部署时由 `ConfigMap` 和 `Secret` 注入。

关键环境变量：

| 变量 | 说明 |
| --- | --- |
| `CREST_DB_HOST` | Crest 系统库地址；OceanBase 可填写 OBProxy 或 observer 地址 |
| `CREST_DB_PORT` | Crest 系统库端口；OceanBase 常用 OBProxy `2883`，observer 直连 `2881` |
| `CREST_DB_NAME` | Crest 系统库名称 |
| `CREST_DB_TYPE` | Crest 系统库类型，默认 `mysql`；OceanBase 适配见 `docs/oceanbase-support.md` |
| `CREST_DB_URL` | 可选完整 JDBC URL，配置后优先于主机、端口和库名 |
| `CREST_DB_USERNAME` | Crest 系统库账号 |
| `CREST_DB_PASSWORD` | Crest 系统库密码 |
| `CREST_RUNTIME_ROLE` | 后端运行角色，默认 `all`；可选 `all`、`api`、`scheduler`、`worker` |
| `CREST_PRODUCTION_MODE` | 生产模式开关，默认 `false`；启用后会强制校验 Redis、固定密钥、Flyway 和演示数据开关 |
| `CREST_DATASOURCE_POOL_PRELOAD_ENABLED` | 是否启动时预热业务数据源连接池；旧 all-in-one 非生产默认开启，生产多副本默认关闭 |
| `CREST_TOKEN_SECRET` | 登录令牌签名密钥；配置后优先于旧 `substitule.json` 中的 `tokenSecret` |
| `CREST_REDIS_HOST` / `CREST_REDIS_PORT` | Redis 地址和端口；生产模式下 readiness 会校验 Redis 连通性 |
| `CREST_REDIS_PASSWORD` / `CREST_REDIS_DATABASE` | Redis 密码和逻辑库；Redis Cluster 必须使用 `0` |
| `CREST_REDIS_CLUSTER_NODES` | Redis Cluster 节点列表，格式 `host1:6379,host2:6379`；配置后优先使用 Cluster |
| `CREST_REDIS_USERNAME` / `CREST_REDIS_SSL_ENABLED` | Redis ACL 用户名和 TLS 开关 |
| `CREST_REDIS_CONNECT_TIMEOUT` / `CREST_REDIS_TIMEOUT` | Redis 连接和读写超时；生产建议 `2s` / `5s` |
| `CREST_REDIS_CLUSTER_REFRESH_ADAPTIVE` / `CREST_REDIS_CLUSTER_REFRESH_PERIOD` | Redis Cluster 拓扑刷新；生产建议 `true` / `30s` |
| `CREST_LOCK_KEY_PREFIX` | Redis 分布式锁 Key 前缀，默认 `crest:lock` |
| `CREST_LOCK_DEFAULT_TTL_SECONDS` | Redis 分布式锁默认过期时间，默认 1800 秒 |
| `CREST_WEBSOCKET_BROADCAST_ENABLED` | 是否启用 WebSocket Redis 广播；未配置时生产模式默认启用，旧部署默认关闭 |
| `CREST_WEBSOCKET_BROADCAST_CHANNEL` | WebSocket 广播 Redis channel，默认 `crest:pubsub:websocket` |
| `CREST_TASK_QUEUE_ENABLED` | 是否启用 Redis Streams 任务队列；未配置时生产模式默认启用，旧部署默认关闭 |
| `CREST_EXPORT_TASK_STREAM` | 导出任务 Redis Stream 名称，默认 `crest:stream:export-task` |
| `CREST_EXPORT_TASK_CONSUMER_GROUP` | 导出任务 Worker 消费组，默认 `crest-export-workers` |
| `CREST_EXPORT_TASK_STALE_MILLIS` | 导出任务 Worker 心跳超时时间，默认 30 分钟 |
| `CREST_EXPORT_TASK_ENQUEUE_RETRY_MILLIS` | 待执行任务重复投递保护间隔，默认 60 秒 |
| `CREST_EXPORT_TASK_PENDING_IDLE_MILLIS` | 导出任务 Redis pending 消息认领等待时间，默认 60 秒 |
| `CREST_SYNC_TASK_STREAM` | 数据集同步任务 Redis Stream 名称，默认 `crest:stream:dataset-sync-task` |
| `CREST_SYNC_TASK_CONSUMER_GROUP` | 数据集同步任务 Worker 消费组，默认 `crest-dataset-sync-workers` |
| `CREST_SYNC_TASK_STALE_MILLIS` | 数据集同步任务 Worker 心跳超时时间，默认 30 分钟 |
| `CREST_SYNC_TASK_ENQUEUE_RETRY_MILLIS` | 数据集同步任务重复投递保护间隔，默认 60 秒 |
| `CREST_SYNC_TASK_PENDING_IDLE_MILLIS` | 数据集同步 Redis pending 消息认领等待时间，默认 60 秒 |
| `CREST_DATASOURCE_SYNC_TASK_STREAM` | 数据源同步任务 Redis Stream 名称，默认 `crest:stream:datasource-sync-task` |
| `CREST_DATASOURCE_SYNC_TASK_CONSUMER_GROUP` | 数据源同步任务 Worker 消费组，默认 `crest-datasource-sync-workers` |
| `CREST_DATASOURCE_SYNC_TASK_STALE_MILLIS` | 数据源同步任务 Worker 心跳超时时间，默认 30 分钟 |
| `CREST_DATASOURCE_SYNC_TASK_ENQUEUE_RETRY_MILLIS` | 数据源同步任务重复投递保护间隔，默认 60 秒 |
| `CREST_DATASOURCE_SYNC_TASK_PENDING_IDLE_MILLIS` | 数据源同步 Redis pending 消息认领等待时间，默认 60 秒 |
| `CREST_SCHEDULED_TASK_STREAM` | 报表和填报调度任务 Redis Stream 名称，默认 `crest:stream:scheduled-task` |
| `CREST_SCHEDULED_TASK_CONSUMER_GROUP` | 报表和填报调度任务 Worker 消费组，默认 `crest-scheduled-workers` |
| `CREST_SCHEDULED_TASK_PENDING_IDLE_MILLIS` | 报表和填报调度 Redis pending 消息认领等待时间，默认 60 秒 |
| `CREST_WORKER_ID` | Worker 实例编号；未配置时自动使用主机名和随机 ID |
| `CREST_INITIAL_PASSWORD` | 管理员初始密码，不能为空 |
| `CREST_AES_KEY` | 32 位加密 Key |
| `CREST_AES_IV` | 16 位加密 IV |
| `CREST_CRYPTO_MODE` | 加密模式，默认 `standard`；需要 SM2/SM3/SM4 时设为 `sm-suite` |
| `CREST_SM4_KEY` | SM4 使用的 16 位 Key；仅在 `CREST_CRYPTO_MODE=sm-suite` 时必填，启动阶段会校验 |
| `CREST_ORIGIN_LIST` | 允许来源列表 |
| `CREST_LOGIN_TIMEOUT` | 登录超时时间，默认 960 分钟 |

源码启动时如果 `CREST_INITIAL_PASSWORD` 为空，后端会拒绝启动。安装脚本会自动生成该值，Kubernetes 清单要求部署前在 Secret 中填写。

`CREST_RUNTIME_ROLE=all` 是兼容模式，保留旧单进程行为。`api` 角色只处理 HTTP 接口，`scheduler` 角色负责调度类启动任务，`worker` 角色为后续异步任务执行预留。非 API 角色只开放健康检查和指标端点，避免被误接入业务流量。

`CREST_PRODUCTION_MODE=true` 用于多副本生产部署。启用后必须显式配置 `CREST_TOKEN_SECRET`，并保持 `CREST_FLYWAY_ENABLED=false`、`CREST_LOAD_DEMO=false`、`spring.cache.type=redis` 和 Redis 健康检查启用。生产数据库初始化和升级 SQL 由部署流程人工执行，不由应用启动时自动迁移。

生产模式下默认不在启动阶段预热业务数据源连接池，避免多副本同时占用业务库连接。确需提前预热时，可显式设置 `CREST_DATASOURCE_POOL_PRELOAD_ENABLED=true`，并按副本数评估业务库最大连接数。

生产模式下导出任务、数据集同步任务和数据源同步任务默认写入 Redis Streams，由 `all` 或 `worker` 角色消费执行。Worker 执行前通过数据库 CAS 抢占任务，心跳超时后会重新投递。旧部署未启用生产模式时仍保持原执行路径。

生产模式下 WebSocket 消息默认通过 Redis pub/sub 广播到所有 API Pod，各 Pod 只向本机连接投递。旧部署未启用生产模式时仍保持本机 WebSocket 投递。

导出文件已通过 `StorageService` 访问。默认实现仍使用本地路径；多副本生产部署需要把 `/opt/crest/data` 下的业务文件目录挂载到 RWX PVC，确保 Worker Pod 生成的文件可被任意 API Pod 下载。

## 数据库迁移

后端使用 Flyway 管理数据库结构和初始化数据。核心迁移目录：

```text
core/core-backend/src/main/resources/db/migration
```

可选演示资源迁移目录：

```text
core/core-backend/src/main/resources/db/demo-migration
```

`v1.5.4` 面向全新环境安装，不支持从旧版本直接通过 Flyway 原地升级。当前保留一份核心初始化脚本和一份可选演示资源脚本：

| 脚本 | 说明 |
| --- | --- |
| `db/migration/V1.5.0.1__initial_schema.sql` | 创建运行所需表结构、默认管理员、基础菜单、系统参数、平台管理、单点登录、审计日志、国产密码套件配置和内置驱动 |
| `db/demo-migration/V1.5.0.2__demo_resources.sql` | 在启用 `CREST_LOAD_DEMO` 时创建 `crest_demo_retail` 零售经营演示库，写入研发效能主题数据、演示数据源、数据集、图表和大屏 |

迁移规则：

- `v1.5.4` 之前的本地实验脚本不作为公开升级链路保留；
- 后续正式发布如支持原地升级，新增脚本必须从已发布版本向前迁移，不改写已发布脚本；
- 初始化数据必须环境无关，不能写入本地 IP、个人账号、外部库密码或临时资源；
- 新增演示资源要能说明来源字段、指标口径、下钻字段和展示目的；
- 演示资源必须放在 `db/demo-migration`，通过 `CREST_LOAD_DEMO` 和 `CREST_FLYWAY_LOCATIONS` 控制是否初始化；
- 数据结构变化要在 release notes 中明确是否支持原地升级，必要时提供回滚说明；
- 迁移脚本应能在空库首次安装时一次执行成功。

空库验证建议：

```bash
docker exec crest-mysql-local mysql -uroot -p'<mysql-root-password>' \
  -e "DROP DATABASE IF EXISTS crest_verify; CREATE DATABASE crest_verify DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

验证点：

- `crest_schema_version` 包含最新迁移；
- 只有一个默认管理员账号；
- `core_workspace_recent_resource`、`core_export_task` 等运行残留为空；
- 启用 `CREST_LOAD_DEMO` 时，零售经营和研发效能演示资源存在；
- `sso.` 配置项存在，默认不开启单点登录；
- 启用 `CREST_LOAD_DEMO` 时，演示数据源连接信息不含本地环境地址；
- `/index.html` 返回 200；
- `/doc.html` 可打开或跳转到登录流程；
- `/v3/api-docs` 在未登录时返回受保护状态，登录或放行接口文档后可正常读取；
- 启用 `CREST_LOAD_DEMO` 时，数据血缘能从演示数据源追踪到字段、数据集、图表和大屏。

## 前端开发约定

前端工程说明见 [../core/core-frontend/README.md](../core/core-frontend/README.md)。这里列出跨页面需要遵守的约定：

- 顶部导航、Logo、字体、主题色和常用列表样式应复用现有组件或公共样式，不在单页重复写一套；
- 业务文案沿用现有产品词，不新增营销式口号；
- “仪表盘”是标准称呼，不再使用旧称；
- 通用图表组件必须从数据集中读取维度和阶段，不把阶段、角色或状态写死在代码里；
- 修改大屏编辑器或大屏运行态时，要确认已有大屏展示不被破坏；
- 页面切换和操作反馈保持克制，不使用影响管理系统效率的夸张动效；
- 涉及数据血缘、数据集、数据源、工作台和系统管理页面时，要在宽屏、普通桌面和较窄窗口下检查布局。

## 后端开发约定

- 控制器、接口 DTO 和 VO 要带清楚的 OpenAPI 注解；
- 数据库访问优先沿用已有 Mapper、Service 和管理类结构；
- 异常信息面向用户可理解，不泄露数据库密码、完整连接串或内部堆栈；
- 外部数据源连接参数必须做危险参数过滤；
- 删除数据源、数据集、图表和展示资源前，要保持影响范围检查；
- 涉及元数据关系的改动，需要同步检查数据血缘；
- 导出、缓存和同步任务要避免在请求线程里做长时间阻塞。

## 接口文档

运行后访问：

```text
/doc.html
/v3/api-docs
```

配置入口：

```text
sdk/common/src/main/java/io/crest/doc/SwaggerConfig.java
```

新增接口时检查：

- 所在包是否进入对应 `GroupedOpenApi` 分组；
- 控制器是否有清楚的 `@Tag` 和 `@Operation`；
- 请求和响应对象是否有必要的 `@Schema`；
- 删除或隐藏的功能不要出现在接口文档分组里。

## OceanBase Oracle 回归

数据源类型为 `obOracle`，驱动类：

```text
com.oceanbase.jdbc.Driver
```

用户名格式：

- OBServer 直连：`username@tenant`
- OBProxy/ODP：`username@tenant#cluster`

Schema 留空时，系统默认使用用户名中 `@` 或 `#` 前面的账号名，并按 Oracle 风格转为大写。

相关变更至少覆盖：

- OBServer 直连，例如 `test@obora` + `2881`；
- OBProxy/ODP，例如 `test@obora#obcluster` + `2883`；
- 表和字段元数据读取，包含字段备注；
- 数据集预览；
- 全量缓存同步；
- 增量缓存同步；
- 仪表盘查询。

## 数据血缘回归

数据血缘属于 Crest 主功能，随系统服务一起构建和发布。

后端入口：

- `core/core-backend/src/main/java/io/crest/relation/server/RelationServer.java`
- `core/core-backend/src/main/java/io/crest/relation/manage/RelationManage.java`
- `core/core-backend/src/main/java/io/crest/relation/dto/*`

前端入口：

- `core/core-frontend/src/api/relation/index.ts`
- `core/core-frontend/src/views/visualized/data/lineage/index.vue`
- `core/core-frontend/src/components/relation-chart/GraphView.vue`

链路模型：

```text
datasource -> table -> table_field -> dataset_field -> dataset -> chart_field -> chart -> dv
```

节点类型：

- `datasource`
- `table`
- `table_field`
- `dataset_field`
- `dataset`
- `chart_field`
- `chart`
- `dv`

回归场景：

- 全局概览能加载；
- 数据源级字段血缘能加载；
- 数据集级字段血缘能加载；
- 仪表盘和数据大屏血缘能加载；
- 表选择器列出当前图里的全部物理表；
- 字段选择器跟随所选表更新；
- 选择字段后图谱收敛到上下游节点；
- 关键字搜索能命中字段和资源；
- 被使用的数据源或数据集下线前能给出影响范围；
- 数据源、数据集、仪表盘和数据大屏页面能正常打开。

用户文档是 [data-lineage.md](./data-lineage.md)。血缘语义、接口、节点类型或边界发生变化时，同步更新该文档。

## 部署命名

新安装默认使用 Crest 命名：

| 项目 | 名称 |
| --- | --- |
| 运行目录 | `/opt/crest` |
| 控制命令 | `crestctl` |
| systemd 服务 | `crest.service` |
| Docker 服务和容器 | `crest` |
| Docker 网络 | `crest-network` |
| 默认数据库 | `crest` |

安装脚本仍识别旧运行目录和旧控制命令，用于存量环境升级。不要在同一个版本里移除这些兼容判断。

## 公开仓库规则

不要提交：

- `.env`、账号密码、token、私钥、证书和真实连接串；
- 本地运行目录、离线包、镜像导出包、日志、pid、压测报告和浏览器测试输出；
- `node_modules`、Maven `target`、前端 `dist`、`.flattened-pom.xml`；
- 真实客户、员工、供应商、合同、内部地址或生产库数据；
- 私有制品仓库、私有 npm 源或内部镜像地址。

可以提交：

- 开源源码、配置模板、迁移脚本、脱敏样例和公开演示数据；
- 公开可再分发的驱动文件；
- 构建、部署、验证和排障文档。

## 文档规则

- 根目录 `README.md` 只保留项目概览、快速入口和核心配置；
- 正式文档入口维护在 `docs/README.md`；
- 安装、离线包和升级说明维护在 `installer/README.md`；
- Kubernetes 和 split 部署说明分别维护在对应部署目录；
- 发布说明只放在 `docs/release/`，历史版本不改写；
- 临时审查报告、个人环境记录、一次性计划和过程复盘不放入公开 docs；
- 文档中的版本号、镜像名、端口、默认账号和 SQL 路径必须与代码和脚本一致；
- 文案使用交付口径，避免承诺式、聊天式和计划式表达。

清理被忽略文件前先预览：

```bash
git clean -fdXn
```

确认后再执行：

```bash
git clean -fdX
```

## 提交前检查

按改动范围选择检查项：

| 改动 | 建议检查 |
| --- | --- |
| 前端页面、样式、组件 | `pnpm run build:lite:check`，必要时再跑 `pnpm run build:base` |
| 后端接口、数据源、导出、血缘 | Maven 打包或相关模块编译，结合服务做接口回归 |
| 数据库迁移 | 空库启动，检查 Flyway 版本和初始化数据 |
| 安装脚本或 Dockerfile | 前后端镜像构建，单机安装或离线包验证 |
| Kubernetes 清单 | `kubectl apply -k` 到测试命名空间并检查 rollout |
| 文档 | 检查版本号、镜像名、端口、账号和路径是否与代码一致 |

提交要求：

- 一个提交只解决一类问题；
- commit message 使用简洁的英文 Conventional Commit 风格；
- 代码范围保持清楚，避免把无关格式化混入；
- 修改部署、镜像、默认账号、功能边界或数据口径时，同步更新 README 和对应文档。
