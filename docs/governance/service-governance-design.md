# Crest 服务治理设计

本文定义 Crest 服务治理的目标、边界、命名标准、API 标准、数据库标准和质量门禁。治理目标是把项目从历史 DataEase 二开形态整理为 Crest 自有产品形态。除开源许可证、上游来源声明和必须保留的第三方组件名称外，产品运行态、接口、配置、数据库和代码语义都应使用 Crest 或明确的业务领域命名。

## 目标

1. 对外接口统一、可读、可演进，新增接口统一使用 `/api/v1`。
2. 后端分层清晰，HTTP、业务编排、领域规则、持久化边界明确。
3. 前端接口调用集中维护，不在页面中散落旧路径和历史命名。
4. 数据库表和字段按业务领域命名，不继续保留 `de`、`dataease`、`per` 等历史语义。
5. 部署配置统一使用 `CREST_*`，安装包、Kubernetes 清单、README 和脚本保持一致。
6. 审计日志、权限、导出、SSO、数据血缘等关键链路有统一质量门禁。

## 明确不做

本轮治理不做旧 DataEase/DE API、旧环境变量、旧数据库表名或旧前端路由的兼容层。迁移完成后，旧名称应从运行路径中消失。例外仅限：

- `LICENSE`、版权声明、README 中对上游 DataEase 来源的合规说明；
- Quartz 等第三方框架自带表名；
- 历史迁移文件中不可避免的来源记录，但新增迁移和运行代码不得继续引入旧命名。

## 当前主要问题

### API

当前接口混用资源名、动作名和历史名，例如 `/dataVisualization/detail`、`/datasetTree/tree`、`/exportCenter/removal/{id}`、`/sysParameter/basic/list`。部分修改类操作使用 GET，例如 `/sync/task/lifecycle/lifecycle/start/{id}`、`/sync/task/lifecycle/lifecycle/stop/{id}`、`/datasource/removal/{datasourceId}`。这些路径难以通过语义判断资源、动作和风险等级，也不利于审计、网关限流和权限规则。

### 后端结构

后端存在大量 `*Server` 类承担 Controller 职责，部分类同时处理 HTTP 参数、权限、业务编排、日志和持久化。例如 `DataVisualizationServer`、`DatasourceServer`、`DatasetDataManage`、`ChartDataManage` 文件体量较大，服务治理时不应继续扩大这些类。

### 命名

`DE_*` 环境变量、`DeApiPath`、`DeLog`、`DEException`、`DataEaseBi`、`dataease_name`、`de_sync_dataset_*` 等名称仍在配置、代码、前端和数据库中出现。这些名称会让维护者无法判断当前系统边界，也影响公开版本的产品一致性。

### 数据库

数据库表前缀混杂：`core_*`、`crest_*`、`per_*`、`data_visualization_*`、`snapshot_*`、`de_*` 同时存在。部分表名表达技术来源而不是领域含义，例如 `core_dataset_group`、`data_visualization_info`、`per_busi_resource`。字段层面也存在 `dataease_name`、`de_type`、`de_extract_type` 等历史名称。

## 命名标准

### 环境变量

所有对外配置统一使用 `CREST_*`。安装脚本、Docker Compose、Kubernetes、README、SECURITY 和运行模板必须一致。

| 旧名称 | 新名称 |
| --- | --- |
| `DE_BASE` | `CREST_BASE` |
| `DE_PORT` | `CREST_PORT` |
| `DE_APP_IMAGE` | `CREST_APP_IMAGE` |
| `DE_MYSQL_IMAGE` | `CREST_MYSQL_IMAGE` |
| `DE_MYSQL_HOST` | `CREST_DB_HOST` |
| `DE_MYSQL_PORT` | `CREST_DB_PORT` |
| `DE_MYSQL_DB` | `CREST_DB_NAME` |
| `DE_MYSQL_USER` | `CREST_DB_USERNAME` |
| `DE_MYSQL_PASSWORD` | `CREST_DB_PASSWORD` |
| `DE_CRYPTO_MODE` | `CREST_CRYPTO_MODE` |
| `DE_AES_KEY` | `CREST_AES_KEY` |
| `DE_AES_IV` | `CREST_AES_IV` |
| `DE_SM4_KEY` | `CREST_SM4_KEY` |
| `DE_INITIAL_PASSWORD` | `CREST_INITIAL_PASSWORD` |
| `DE_ORIGIN_LIST` | `CREST_ORIGIN_LIST` |

### Java 命名

| 旧名称 | 新名称 |
| --- | --- |
| `DeApiPath` | `CrestApiPath` |
| `DeLog` | `CrestAudit` |
| `DeLogAspect` | `CrestAuditAspect` |
| `DEException` | `CrestException` |
| `DeModel` | `CrestModel` |
| `DeI18nStarter` | `CrestI18nStarter` |
| `DeReloadableResourceBundleMessageSource` | `CrestReloadableResourceBundleMessageSource` |
| `*Server` Controller | `*Controller` |

新代码不允许新增 `De`、`DE`、`DataEase` 作为 Crest 运行语义。测试类、注解、异常、日志和模型同步改名。

### 前端命名

| 旧名称 | 新名称 |
| --- | --- |
| `window.DataEaseBi` | `window.CrestBi` |
| `DeResourceTree.vue` | `CrestResourceTree.vue` |
| `DeResourceGroupOpt.vue` | `CrestResourceGroupActions.vue` |
| `DeAppApply.vue` | `CrestAppApply.vue` |
| `DeRichTextView.vue` | `CrestRichTextView.vue` |
| `DeRichEditor.vue` | `CrestRichEditor.vue` |
| `DePreview.vue` | `CrestPreview.vue` |

前端 API 路径必须从 `src/api` 或领域 API 模块导出，页面组件不得直接拼旧接口路径。

## API 标准

### 基本规则

1. 新接口统一以 `/api/v1` 开头。
2. URL 使用小写 kebab-case，资源名使用复数名词。
3. GET 只读；POST 创建或执行命令；PUT 全量更新；PATCH 局部更新；DELETE 删除。
4. 分页使用 query 参数：`?page=1&pageSize=20`，不再使用 `/{page}/{pageSize}`。
5. 命令类动作使用子资源或动作后缀，不使用 `findById`、`updateBase`、`deleteLogic`。
6. 公开接口、登录前接口和分享接口必须明确标注权限策略。
7. 每个写接口、导出接口、下载接口、SSO 配置接口必须进入审计日志。

`@CrestApiPath` 和 Controller 上的路径只写领域资源根路径；运行时由 `CorsConfig` 通过 `AuthConstant.CREST_API_PREFIX` 统一挂载到 `/api/v1` 下。已经完成治理的接口不得再使用 `record-new`、`change`、`removal`、`batch/del`、`chartRemoval`、`removeJumpSet`、`removeLinkage`、`canvas-change`、`base-change` 等动作式片段；创建使用资源根路径 `POST`，修改使用 `PUT`，删除使用 `DELETE`。

### 资源路径

| 领域 | 标准路径 |
| --- | --- |
| 登录会话 | `/api/v1/auth/sessions` |
| 公钥 | `/api/v1/auth/public-key` |
| 当前用户 | `/api/v1/users/me` |
| 用户 | `/api/v1/users` |
| 角色 | `/api/v1/roles` |
| 权限 | `/api/v1/permissions` |
| 系统设置 | `/api/v1/settings` |
| SSO | `/api/v1/sso` |
| 数据源 | `/api/v1/data-sources` |
| 数据集 | `/api/v1/datasets` |
| 字段 | `/api/v1/dataset-fields` |
| 图表数据 | `/api/v1/charts` |
| 仪表盘 | `/api/v1/dashboards` |
| 数据大屏 | `/api/v1/screens` |
| 可视化资源收藏 | `/api/v1/workspace/favorites` |
| 最近使用 | `/api/v1/workspace/recent-resources` |
| 导出任务 | `/api/v1/export-tasks` |
| 数据血缘 | `/api/v1/lineage` |
| 分享 | `/api/v1/shares` |
| 同步任务 | `/api/v1/sync-tasks` |

### 命令示例

| 当前接口 | 治理后接口 |
| --- | --- |
| `POST /dataVisualization/detail` | `GET /api/v1/visualizations/{id}` |
| `POST /dataVisualization/publish-status` | `POST /api/v1/visualizations/{id}/publish` |
| `POST /dataVisualization/recoverToPublished` | `POST /api/v1/visualizations/{id}/restore-published` |
| `POST /datasetTree/tree` | `GET /api/v1/datasets/tree` |
| `GET /exportCenter/generateDownloadUri/{id}` | `POST /api/v1/export-tasks/{id}/download-tickets` |
| `GET /sync/task/lifecycle/lifecycle/start/{id}` | `POST /api/v1/sync-tasks/{id}/start` |
| `GET /sync/task/lifecycle/lifecycle/stop/{id}` | `POST /api/v1/sync-tasks/{id}/stop` |
| `GET /dekey` | `GET /api/v1/auth/public-key` |

## 后端架构标准

### 分层

每个业务领域采用以下结构：

```text
io.crest.<domain>
  controller       HTTP 入参、状态码、认证上下文读取
  application      用例编排、事务、审计上下文
  domain           领域模型、规则、枚举、策略
  infrastructure   Mapper、外部系统、文件、消息、缓存
  dto              API 请求和响应模型
```

短期内允许保留 `manage`，但新功能和被治理的接口必须迁移到 `application`。`controller` 不直接调用 Mapper，不直接写复杂 SQL，不处理跨领域规则。

### API 契约

`sdk/api` 可以继续承载接口契约，但命名应改为 Crest 语义。接口注解使用 `@CrestApiPath`，路径必须是 `/api/v1` 标准路径。后端 Controller 实现契约接口，不在实现类中重复定义另一套路径。

### 横切能力

审计、权限、异常、幂等、下载鉴权、SSO 回调校验统一在公共模块处理：

- 审计注解：`@CrestAudit`
- 兜底审计：写接口、下载接口、导出接口、SSO 配置接口自动记录
- 异常：统一抛出 `CrestException`
- 权限：统一资源类型和权限动作枚举
- Token Header：统一改为 `X-CREST-TOKEN`

## 数据库标准

### 命名规则

1. 表名使用小写 snake_case。
2. 表名按领域前缀组织，不使用技术前缀描述业务。
3. 字段名表达业务含义，不使用 `de`、`dataease`、`per`。
4. 新迁移使用 `ALTER TABLE ... RENAME TO ...` 和 `ALTER TABLE ... CHANGE COLUMN ...` 保留数据，但迁移后旧表名和旧字段名不保留。
5. 不创建旧名 view、trigger 或兼容别名。
6. Quartz 表属于第三方框架，保留 `QRTZ_*`。

### 领域表映射

| 当前表 | 新表 |
| --- | --- |
| `crest_user` | `iam_user` |
| `crest_org` | `iam_org` |
| `crest_role` | `iam_role` |
| `crest_user_org` | `iam_user_org` |
| `crest_user_role` | `iam_user_role` |
| `crest_role_menu_permission` | `iam_role_menu_permission` |
| `crest_resource_index` | `iam_resource_index` |
| `crest_resource_permission` | `iam_resource_permission` |
| `per_busi_resource` | 删除，能力合并到 `iam_resource_index` |
| `per_auth_busi_user` | 删除，能力合并到 `iam_resource_permission` |
| `per_auth_busi_role` | 删除，能力合并到 `iam_resource_permission` |
| `core_menu` | `iam_menu` |
| `core_audit_log` | `audit_log` |
| `core_sys_setting` | `sys_setting` |
| `core_datasource` | `data_source` |
| `core_dataset_group` | `dataset_group` |
| `core_dataset_table` | `dataset_table` |
| `core_dataset_table_field` | `dataset_field` |
| `core_chart_view` | `bi_chart` |
| `snapshot_core_chart_view` | `bi_chart_snapshot` |
| `data_visualization_info` | `bi_visualization` |
| `snapshot_data_visualization_info` | `bi_visualization_snapshot` |
| `core_opt_recent` | `workspace_recent_resource` |
| `core_store` | `workspace_favorite_resource` |
| `core_export_task` | `export_task` |
| `sso_provider` | `auth_sso_provider` |
| `sso_identity_binding` | `auth_sso_identity_binding` |
| `area` | `ref_area` |
| `de_standalone_version` | `crest_schema_version` |

### 字段映射

| 当前字段 | 新字段 |
| --- | --- |
| `dataease_name` | `engine_field_name` |
| `de_type` | `semantic_type` |
| `de_extract_type` | `extract_type` |
| `uid` | `user_id` |
| `oid` | `org_id` |
| `rid` | `role_id` |
| `pid` | `parent_id` |
| `pkey` | `setting_key` |
| `pval` | `setting_value` |
| `dv_id` | `visualization_id` |

研发效能演示库中的 `eng_*` 表属于演示指标数据域，名称清晰，可保留。若后续转为平台内置指标域，再单独迁移到 `metric_*` 或 `dev_efficiency_*`。

## 前端治理标准

1. `src/api` 按领域拆分，路径常量集中定义。
2. 页面组件只调用 API 函数，不直接拼接 URL。
3. 旧 `De*` 组件统一改成 `Crest*` 或业务名。
4. 路由中不再出现 `/DeResourceTree` 等历史路径。
5. 登录、SSO、工作台、系统设置、数据源、数据集、数据血缘、导出中心都必须使用统一 Token Header 和标准 API。
6. 审计日志页面的 URL 翻译规则同步改为标准 API 名称。

## 迁移顺序

1. 建立治理扫描脚本和清单，记录当前旧名、旧 API 和旧表引用。
2. 更新运行配置：安装脚本、Docker Compose、Kubernetes、README、应用配置统一切到 `CREST_*`。
3. 改公共后端命名：异常、审计、API Path、Token Header、I18n 基础类。
4. 改数据库迁移：新增表/字段重命名迁移，更新 Mapper、实体、SQL 模板和演示数据脚本。
5. 改 API 契约：`sdk/api` 路径迁移到 `/api/v1`，Controller 实现同步重命名。
6. 改前端调用：集中替换 `src/api`，再处理页面直接引用。
7. 拆分高风险大类：优先治理数据源、数据集、可视化、导出、SSO、权限。
8. 补齐审计覆盖：写接口、导出下载、权限配置、SSO 配置、用户角色操作必须可查。
9. 跑空库、升级库、前端构建、后端构建和 Docker 启动验证。

## 质量门禁

每批提交至少运行对应门禁：

```bash
bash scripts/governance/scan-service-governance.sh
mvn -pl core/core-backend -am -DskipTests test-compile
cd core/core-frontend && pnpm run build:base
```

最终合并前必须满足：

- `scripts/governance/scan-service-governance.sh --strict` 通过；
- 后端构建和必要测试通过；
- 前端构建通过；
- 空库 Flyway 首次初始化通过；
- 已有本地库升级迁移通过；
- Docker Compose 启动后登录、SSO 状态、工作台、数据源、数据集、数据血缘、仪表盘、数据大屏、导出中心可用；
- 审计日志能覆盖导出下载、用户角色、权限、SSO、资源创建修改删除。

## 验收标准

1. 公开配置、安装包、Kubernetes 和文档中不再出现 `DE_*` 运行变量。
2. Java 运行代码中不再出现 `DeApiPath`、`DeLog`、`DEException`、`DataEase` 语义类名。
3. 前端运行代码中不再出现 `DataEaseBi`、`DeResourceTree` 等历史运行名。
4. 新 API 全部位于 `/api/v1`，不存在 GET 修改数据。
5. 数据库运行表和字段使用 Crest 领域命名，不再保留旧兼容表。
6. Flyway schema history 表使用 `crest_schema_version`。
7. 审计日志能说明谁在什么时间对哪个资源做了什么操作。
8. README、开发文档、部署文档与实际配置一致。
