# Crest

Crest 是面向私有化部署的 BI 平台，覆盖数据源接入、数据集建模、图表、仪表盘、数据大屏、分享、导出、系统管理和字段级数据血缘。当前工程版本为 `v2.0.2`。

Crest 基于 DataEase 2.10.22 开源版本继续开发，并合入 2.10.23 相关安全加固和依赖升级。仓库保留上游版权和 GPLv3 许可声明；品牌、部署脚本、演示资源、OceanBase 支持、数据血缘和研发效能分析能力由 Crest 独立维护。

## 功能范围

| 模块 | 说明 |
| --- | --- |
| 工作台 | 资源概览、收藏资源、最近使用和快捷入口 |
| 数据源 | MySQL、OceanBase MySQL、OceanBase Oracle 等连接管理 |
| 数据集 | 字段管理、计算字段、数据预览和缓存同步 |
| 图表与仪表盘 | 图表编辑、筛选、联动、跳转、分享和导出 |
| 数据大屏 | 面向驾驶舱、展示墙和专题分析的可视化页面 |
| 数据血缘 | 展示数据源、表、字段、数据集、图表和展示资源之间的依赖 |
| 系统管理 | 用户、角色、权限、站点设置、单点登录、字体、系统参数和关于系统 |
| 可观测性 | 可选接入 Prometheus 和 Grafana |

## 部署架构

```text
Browser
  -> Nginx frontend gateway
  -> Spring Boot backend service
  -> System database
  -> Business datasources
```

默认单机安装启动四个容器：

- `crest`：前端 Nginx 网关；
- `crest-service`：后端服务；
- `mysql-crest`：默认系统库；
- `redis-crest`：生产模式下使用的缓存、任务队列、锁和 WebSocket 广播组件。

OceanBase 系统库按企业外部数据库处理。生产环境应先由 DBA 创建租户、库或 schema、账号和权限，使用 `obclient --default-character-set=utf8mb4` 执行离线包中的 `init-sql/ob-mysql` 或 `init-sql/ob-oracle` 初始化 SQL，再启动 Crest。MySQL 默认行为不变，仍由应用在启动时执行 Flyway 初始化。

## 目录结构

| 路径 | 说明 |
| --- | --- |
| `core/core-backend` | Spring Boot 后端、接口实现、Flyway 脚本和最终 JAR |
| `core/core-frontend` | Vue 3、Vite、TypeScript、Element Plus 前端工程 |
| `sdk` | API 契约、公共模型、认证、工具类和扩展定义 |
| `drivers` | 随仓库分发的 JDBC 驱动 |
| `installer` | 单机安装、运维控制和离线包制作 |
| `deploy/split` | 前后端分离容器部署示例 |
| `deploy/kubernetes` | Kubernetes 部署清单 |
| `docs` | 产品能力、部署运维、开发和发布文档 |

## 快速安装

单机 Docker 部署：

```bash
cd installer
bash install.sh
```

安装完成后使用终端输出的初始密码登录：

```text
http://<server-ip>:8100
username: admin
```

登录后可在右上角用户菜单打开“关于系统”，核对前端版本、前端 Commit、后端版本和后端 Commit。

常用运维命令：

```bash
crestctl status
crestctl start
crestctl stop
crestctl restart
crestctl backup
crestctl version
```

完整配置、备份、升级和离线包说明见 [installer/README.md](./installer/README.md)。

## 关键配置

| 配置项 | 说明 |
| --- | --- |
| `CREST_PORT` | Web 访问端口，默认 `8100` |
| `CREST_BACKEND_IMAGE` | 后端镜像，默认示例 `ghcr.io/sevoniva/crest-service:v2.0.2` |
| `CREST_FRONTEND_IMAGE` | 前端镜像，默认示例 `ghcr.io/sevoniva/crest-web:v2.0.2` |
| `CREST_LOAD_DEMO` | 是否初始化内置演示资源，生产环境可设为 `false` |
| `CREST_DB_TYPE` | 系统库类型：`mysql`、`ob-mysql`、`ob-oracle` |
| `CREST_FLYWAY_ENABLED` | 留空自动判断：MySQL 默认 `true`，OceanBase 默认 `false` |
| `CREST_INITIAL_PASSWORD` | 管理员初始密码，留空时安装脚本自动生成 |
| `CREST_AES_KEY` / `CREST_AES_IV` | 敏感配置加密参数，升级已有环境时不要修改 |
| `CREST_ORIGIN_LIST` | 允许访问来源，生产环境应改为实际域名 |
| `CREST_PROMETHEUS_ENABLED` | 是否启用 Prometheus 指标端点 |

## 离线包

完整离线安装包：

```bash
cd installer
bash make-offline-package.sh v2.0.2 linux-amd64
bash make-offline-package.sh v2.0.2 linux-arm64
```

应用镜像升级包：

```bash
cd installer
bash make-app-image-package.sh v2.0.2 linux-amd64
bash make-app-image-package.sh v2.0.2 linux-arm64
```

包内会包含：

- 前端和后端镜像；
- 完整安装脚本或镜像加载脚本；
- `upgrade-sql/` 增量升级 SQL；
- `init-sql/` 和一次性空库初始化 SQL；
- 部署说明。GitHub Release 附件会另行提供 checksum 文件。

## Kubernetes

Kubernetes 清单位于 [deploy/kubernetes](./deploy/kubernetes)：

- `internal-mysql`：部署 Crest 和内置 MySQL；
- `external-mysql`：连接外部 MySQL；
- `*-observability`：启用 Prometheus 和 Grafana。

部署说明见 [deploy/kubernetes/README.md](./deploy/kubernetes/README.md)。

## 本地开发

推荐工具链：

| 工具 | 版本 |
| --- | --- |
| JDK | 21 |
| Maven | 3.9+ |
| Node.js | 22 |
| pnpm | 11 |
| MySQL | 8.0 或兼容版本 |

前端：

```bash
cd core/core-frontend
pnpm install --frozen-lockfile
pnpm run build:base
```

后端：

```bash
mvn -s .mvn/settings.xml -pl :core-backend -am clean package -Pstandalone -DskipTests -Dcrest.copy.frontend.skip=true
```

开发规范见 [docs/development.md](./docs/development.md)。

## 文档

文档入口见 [docs/README.md](./docs/README.md)。

常用文档：

- [单机安装与离线包](./installer/README.md)
- [Kubernetes 部署](./deploy/kubernetes/README.md)
- [OceanBase 支持](./docs/oceanbase-support.md)
- [可观测性](./docs/observability.md)
- [单点登录](./docs/sso.md)
- [数据血缘](./docs/data-lineage.md)
- [发布管理](./docs/release-process.md)

## 许可

Crest 按 GNU General Public License version 3 (GPLv3) 发布。使用、修改、部署或再分发时，请保留上游版权、许可证和无担保声明，并遵守 GPLv3 要求。

上游项目信息：

- DataEase: <https://github.com/dataease/dataease>
- FIT2CLOUD 飞致云: <https://fit2cloud.com/>

许可证文件见 [LICENSE](./LICENSE)。
