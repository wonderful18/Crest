# Crest 单机安装与离线包

本目录用于单机 Docker 部署和离线安装包制作。源码仓库只提交安装脚本、配置模板和说明文档；离线包、镜像归档和本地运行目录不提交到 Git。

## 文件说明

| 文件 | 说明 |
| --- | --- |
| `install.sh` | 安装和同发布线升级入口 |
| `install.conf` | 安装配置模板 |
| `cleanup-demo-data.sh` | 内置演示资源清理脚本 |
| `uninstall.sh` | 卸载脚本 |
| `crestctl` | 安装后的运维控制命令 |
| `crest/docker-compose.yml` | Crest 应用容器模板 |
| `crest/observability` | Prometheus 配置和 Grafana provisioning 文件 |
| `crest/bin/manual/cleanup-demo-data.sql` | 手动清理内置演示资源使用的 SQL |
| `crest/templates/application.yml` | 后端运行配置模板 |
| `make-offline-package.sh` | 离线包制作脚本 |
| `LICENSE` | GPLv3 许可证副本 |

## 默认部署形态

Crest 单机安装默认采用前后端分离容器：

- `crest`：前端 Nginx 网关，对外暴露 Web 端口；
- `crest-service`：后端 Spring Boot 服务，只在 Compose 网络内访问；
- `mysql-crest`：元数据库。

可观测性组件默认关闭。启用后会额外启动：

- `crest-prometheus`：在 Compose 网络内抓取后端指标，不映射宿主机端口；
- `crest-grafana`：加载 Prometheus 数据源和 Crest 看板，默认映射 `3000/tcp`。

数据血缘、导出中心、系统管理、数据源、数据集、仪表盘和数据大屏都在后端服务内运行，不需要单独部署同步任务服务或截图服务。

前端和后端容器默认以非 root 用户运行，并启用只读根文件系统、`no-new-privileges`、最小 capability、进程数边界和日志滚动。运行时写入目录集中在 `/opt/crest`，便于做备份、权限审计和磁盘容量监控。

前端容器通过 `/healthz` 判断网关是否可用。后端容器通过 `/api/v1/actuator/health/readiness` 判断服务是否可以接收业务流量；该检查会访问元数据库。`/api/v1/actuator/health/liveness` 只判断应用进程是否仍能响应，适合作为存活检查。

## 安装前检查

建议服务器满足：

| 项目 | 建议 |
| --- | --- |
| 操作系统 | Linux x86_64 或 arm64 |
| CPU | 2 核及以上 |
| 内存 | 4 GB 及以上 |
| 磁盘 | `/opt` 所在磁盘至少 20 GB 可用 |
| 端口 | 默认开放 `8100/tcp` |
| 容器运行时 | Docker 和 Docker Compose；脚本可在线安装，也可离线包内置 |

生产环境建议：

- 使用固定域名和 HTTPS 反向代理；
- 修改 `CREST_ORIGIN_LIST` 为实际访问地址；
- 使用高强度 `CREST_DB_PASSWORD`、`CREST_AES_KEY`、`CREST_AES_IV` 和 `CREST_INITIAL_PASSWORD`；
- 首次登录后修改管理员密码；
- 将 `/opt/crest` 纳入备份。

## 配置项

`install.conf` 中主要配置：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `CREST_BASE` | `/opt` | 安装基目录，实际运行目录为 `${CREST_BASE}/crest` |
| `CREST_PORT` | `8100` | Web 访问端口 |
| `CREST_BACKEND_IMAGE` | `ghcr.io/sevoniva/crest-service:v1.5.4` | Crest 后端镜像 |
| `CREST_FRONTEND_IMAGE` | `ghcr.io/sevoniva/crest-web:v1.5.4` | Crest 前端镜像 |
| `CREST_MYSQL_IMAGE` | `mysql:8.0.43` | MySQL 镜像 |
| `CREST_LOAD_DEMO` | `true` | 是否初始化内置演示资源；生产环境不需要示例数据时设为 `false` |
| `CREST_EXTERNAL_MYSQL` | `false` | 单机安装是否连接外部 MySQL；默认安装内置 MySQL 容器 |
| `CREST_DB_NAME` | `crest` | 元数据库名 |
| `CREST_DB_USERNAME` | `root` | MySQL 用户 |
| `CREST_DB_PASSWORD` | 空 | 留空时安装脚本自动生成 |
| `CREST_AES_KEY` | 空 | 留空时自动生成 32 位值；升级时不要改 |
| `CREST_AES_IV` | 空 | 留空时自动生成 16 位值；升级时不要改 |
| `CREST_CRYPTO_MODE` | `standard` | 加密模式，`standard` 使用 AES/RSA，`sm-suite` 使用 SM2/SM3/SM4 |
| `CREST_SM4_KEY` | 空 | SM4 使用的 16 位 Key；`CREST_CRYPTO_MODE=sm-suite` 且留空时自动生成，服务启动会校验，升级时不要改 |
| `CREST_INITIAL_PASSWORD` | 空 | 管理员初始密码，留空时自动生成并输出 |
| `CREST_SUBSTITULE_PATH` | 空 | 登录兼容配置文件路径；留空时自动使用 `${CREST_CACHE_PATH}/substitule.json` |
| `CREST_ORIGIN_LIST` | `http://localhost:${CREST_PORT},http://127.0.0.1:${CREST_PORT}` | 允许来源，正式环境要改成实际域名 |
| `CREST_PROMETHEUS_ENABLED` | `false` | 是否启用 Prometheus 指标端点和采集容器 |
| `CREST_PROMETHEUS_TOKEN` | 空 | Prometheus 抓取后端指标使用的 Bearer Token；开启后留空自动生成 |
| `CREST_PROMETHEUS_MAX_URI_TAGS` | `200` | 接口 URI 指标标签数量上限 |
| `CREST_GRAFANA_ENABLED` | `false` | 是否启用 Grafana 容器 |
| `CREST_GRAFANA_PORT` | `3000` | Grafana 对外端口 |
| `CREST_GRAFANA_ADMIN_USER` | `admin` | Grafana 管理员账号 |
| `CREST_GRAFANA_ADMIN_PASSWORD` | 空 | Grafana 管理员密码；开启后留空自动生成 |
| `CREST_GRAFANA_PUBLIC_URL` | 空 | Grafana 页面访问地址；留空时安装脚本按本机端口生成 |

## 在线安装

```bash
cd installer
bash install.sh
```

安装脚本会：

1. 读取 `install.conf`；
2. 创建运行目录；
3. 生成缺省密码和加密参数；
4. 根据 `CREST_LOAD_DEMO` 决定是否加载内置演示资源；
5. 复制配置模板；
6. 安装或复用 Docker 与 Docker Compose；
7. 加载 `images/` 中的离线镜像，如果目录不存在则从镜像仓库拉取；
8. 注册 `crest.service`；
9. 启动服务；
10. 输出访问地址、账号和初始密码。

默认账号：

```text
用户名：admin
密码：安装完成后终端输出的初始密码
```

## 可观测性

启用 Prometheus 和 Grafana：

```bash
CREST_PROMETHEUS_ENABLED=true
CREST_GRAFANA_ENABLED=true
```

Prometheus 抓取后端 `/api/v1/actuator/prometheus` 时使用 Bearer Token。安装脚本会把 token 写入运行目录的 `conf/prometheus-token`，Prometheus 以只读文件方式挂载。前端网关会对外屏蔽指标路径，Prometheus 直接访问 Compose 网络内的 `crest-service:8100`。

企业已有 Prometheus/Grafana 时，不需要启动内置 `crest-prometheus` 和 `crest-grafana`。只开启 `CREST_PROMETHEUS_ENABLED=true`，设置 `CREST_PROMETHEUS_TOKEN`，由企业 Prometheus 抓取后端内网地址，再导入 `crest/observability/grafana/provisioning/dashboards/*.json` 和 `crest/observability/prometheus/rules/crest-alerts.yml`。

Grafana 启动后访问：

```text
http://服务器IP:${CREST_GRAFANA_PORT}
```

默认加载 Crest 服务总览、API 监控、告警与 SLO、JVM、数据库连接池、任务与缓存看板。告警规则覆盖服务可用性、API 错误率、P95 延迟、JVM 堆内存、数据库连接池和线程池队列。

## 运维命令

安装后使用 `crestctl`：

```bash
crestctl status
crestctl start
crestctl stop
crestctl restart
crestctl clear-logs
crestctl backup
crestctl restore crest-backup-YYYYMMDD_HHMMSS.tar.gz
crestctl version
```

备份建议：

- 重要升级前先停服备份；
- 备份文件不要放在 `/opt/crest` 目录内；
- 备份文件可能包含数据库内容、导出文件和业务配置，不提交到 Git。

## 迁移和同发布线升级

v1.5.4 面向全新环境安装，不支持从早期开发版本直接原地升级。已有早期环境迁移到 v1.5.4 时，请先备份元数据库和运行目录，导出业务资源，再在新环境中导入。

同一发布线的小版本升级前：

```bash
crestctl backup stop
```

确认 release notes 明确支持原地升级后，替换镜像标签并在运行目录执行：

```bash
cd /opt/crest
docker-compose up -d --no-deps crest-service crest
```

升级后检查：

```bash
crestctl status
docker logs crest --tail 200
```

确认点：

- `crest`、`crest-service` 和 `mysql-crest` 容器健康；
- Flyway 迁移成功；
- 登录、工作台、数据源、数据集、仪表盘、数据大屏、数据血缘和导出中心可打开；
- 日志中没有持续的 `ERROR` 或 `Exception`。

## 内置演示资源

默认安装会加载零售经营和研发效能演示资源，便于验证数据源、数据集、图表、大屏和数据血缘链路。生产环境不需要示例数据时，安装前在 `install.conf` 中关闭：

```bash
CREST_LOAD_DEMO=false
```

已加载演示资源的环境可执行清理脚本：

```bash
bash cleanup-demo-data.sh --yes
```

脚本会删除固定演示资源 ID、`crest_demo_retail` 演示库，并把运行配置改为后续不再初始化演示资源。执行前建议先备份元数据库。

## 离线包制作

发布包按 CPU 架构分别制作：

```bash
bash make-offline-package.sh v1.5.4 linux-amd64
bash make-offline-package.sh v1.5.4 linux-arm64
```

默认输出目录：

```text
../offline-packages
```

发布包名：

```text
crest-offline-v1.5.4-linux-amd64.tar.gz
crest-offline-v1.5.4-linux-arm64.tar.gz
```

制作前需要本机已有对应架构镜像：

```text
ghcr.io/sevoniva/crest-service:v1.5.4-amd64
ghcr.io/sevoniva/crest-service:v1.5.4-arm64
ghcr.io/sevoniva/crest-web:v1.5.4-amd64
ghcr.io/sevoniva/crest-web:v1.5.4-arm64
mysql:8.0.43
```

如需把 Prometheus 和 Grafana 镜像也放入离线包，先准备对应架构镜像，再设置：

```bash
INCLUDE_OBSERVABILITY_IMAGES=true \
PROMETHEUS_IMAGE=prom/prometheus:v3.12.0 \
GRAFANA_IMAGE=grafana/grafana-oss:13.0.1 \
bash make-offline-package.sh v1.5.4 linux-amd64
```

也可以通过环境变量指定来源镜像：

```bash
SOURCE_BACKEND_IMAGE=ghcr.io/sevoniva/crest-service:v1.5.4-amd64 \
BACKEND_IMAGE=ghcr.io/sevoniva/crest-service:v1.5.4-amd64 \
SOURCE_FRONTEND_IMAGE=ghcr.io/sevoniva/crest-web:v1.5.4-amd64 \
FRONTEND_IMAGE=ghcr.io/sevoniva/crest-web:v1.5.4-amd64 \
SOURCE_MYSQL_IMAGE=mysql:8.0.43-amd64 \
MYSQL_IMAGE=mysql:8.0.43 \
bash make-offline-package.sh v1.5.4 linux-amd64
```

脚本会校验前端、后端和 MySQL 镜像架构，架构不一致会直接停止。`SOURCE_*_IMAGE` 用于本机制作来源，包内 `install.conf` 仍写入不带架构后缀的目标镜像名。

## 离线安装

在目标服务器执行：

```bash
tar -zxf crest-offline-v1.5.4-linux-amd64.tar.gz
cd crest-offline-v1.5.4-linux-amd64
vi install.conf
bash install.sh
```

离线包内的 `images/` 目录会被安装脚本自动加载：

```bash
docker load -i images/<image>.tar.gz
```

安装完成后访问：

```text
http://服务器IP:8100
```

## 数据持久化

默认运行目录：

```text
/opt/crest
```

重要目录：

| 路径 | 内容 |
| --- | --- |
| `/opt/crest/.env` | 安装时生成的环境变量 |
| `/opt/crest/conf` | 应用配置 |
| `/opt/crest/data/mysql` | MySQL 数据 |
| `/opt/crest/data/exportData` | 导出文件 |
| `/opt/crest/data/static-resource` | 静态资源 |
| `/opt/crest/data/font` | 字体文件 |
| `/opt/crest/logs` | 应用和容器日志 |
| `/opt/crest/cache` | 应用缓存 |

备份和迁移时至少保留 `/opt/crest` 目录和数据库数据。

## 故障排查

查看服务：

```bash
crestctl status
docker ps
docker logs crest --tail 200
docker logs mysql-crest --tail 200
```

查看应用日志：

```bash
tail -f /opt/crest/logs/crest/info.log
tail -f /opt/crest/logs/crest/error.log
```

常见问题：

| 现象 | 检查点 |
| --- | --- |
| 页面打不开 | 端口是否开放、`crest` 容器是否健康、反向代理是否转发到前端 Web 端口 |
| 登录失败 | 初始密码是否来自安装输出，是否已经被修改 |
| 数据库连接失败 | `mysql-crest` 是否健康，`CREST_DB_PASSWORD` 是否和 `.env` 一致 |
| 升级后启动失败 | Flyway 迁移日志、数据库备份是否可恢复、镜像架构是否正确 |
| 导出文件不可下载 | `/opt/crest/data/exportData` 权限和磁盘空间 |

## 不提交的内容

以下内容只存在本机或发布流程，不进入 Git：

- `offline-packages/`
- `images/`
- `install.log`
- `/opt/crest` 运行目录备份
- `.env`
- 备份压缩包
- 镜像导出包
- 含真实连接串或账号的数据文件
