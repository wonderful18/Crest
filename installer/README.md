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
| `upgrade-sql/` | 同发布线升级时可手动执行的 SQL |
| `make-offline-package.sh` | 完整离线安装包制作脚本 |
| `make-app-image-package.sh` | 前后端应用镜像升级包制作脚本 |
| `LICENSE` | GPLv3 许可证副本 |

## 默认部署形态

Crest 单机安装默认采用前后端分离容器：

- `crest`：前端 Nginx 网关，对外暴露 Web 端口；
- `crest-service`：后端 Spring Boot 服务，只在 Compose 网络内访问；
- `mysql-crest`：Crest 系统库。
- `redis-crest`：Redis 实例，用于生产模式下的分布式缓存、任务队列、锁和 WebSocket 广播。

可观测性组件默认关闭。启用后会额外启动：

- `crest-prometheus`：在 Compose 网络内抓取后端指标，不映射宿主机端口；
- `crest-grafana`：加载 Prometheus 数据源和 Crest 看板，默认映射 `3000/tcp`。

数据血缘、导出中心、系统管理、数据源、数据集、仪表盘和数据大屏都在后端服务内运行，不需要单独部署同步任务服务或截图服务。

前端和后端容器默认以非 root 用户运行，并启用只读根文件系统、`no-new-privileges`、最小 capability、进程数边界和日志滚动。运行时写入目录集中在 `/opt/crest`，便于做备份、权限审计和磁盘容量监控。

前端容器通过 `/healthz` 判断网关是否可用。后端容器通过 `/api/v1/actuator/health/readiness` 判断服务是否可以接收业务流量；该检查会访问 Crest 系统库。`/api/v1/actuator/health/liveness` 只判断应用进程是否仍能响应，适合作为存活检查。

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
| `CREST_BACKEND_IMAGE` | `ghcr.io/sevoniva/crest-service:v2.0.0` | Crest 后端镜像 |
| `CREST_FRONTEND_IMAGE` | `ghcr.io/sevoniva/crest-web:v2.0.0` | Crest 前端镜像 |
| `CREST_MYSQL_IMAGE` | `mysql:8.0.43` | MySQL 镜像 |
| `CREST_REDIS_IMAGE` | `redis:7.4.5-alpine` | Redis 镜像 |
| `CREST_LOAD_DEMO` | `true` | 是否初始化内置演示资源；生产环境不需要示例数据时设为 `false` |
| `CREST_PRODUCTION_MODE` | `false` | 生产多副本模式开关；启用后必须使用 Redis，并会禁用启动期 Flyway 和演示数据初始化 |
| `CREST_FLYWAY_ENABLED` | 空 | Crest 系统库 Flyway 初始化开关；留空时 MySQL 默认 `true`，OceanBase 默认 `false` |
| `CREST_EXTERNAL_MYSQL` | `false` | 单机安装是否连接外部系统库；默认安装内置 MySQL 容器，`CREST_DB_TYPE=ob-mysql/ob-oracle` 时脚本会自动切换为外部库模式 |
| `CREST_DB_TYPE` | `mysql` | Crest 系统库类型，支持 `mysql`、`ob-mysql`、`ob-oracle` |
| `CREST_DB_HOST` | `mysql-crest` | Crest 系统库地址；OceanBase 建议填写 OBProxy 地址，直连 observer 时填写 observer 地址 |
| `CREST_DB_PORT` | `3306` | Crest 系统库端口；OceanBase 常用 OBProxy `2883`，observer 直连 `2881` |
| `CREST_DB_NAME` | `crest` | Crest 系统库名 |
| `CREST_DB_DRIVER_CLASS_NAME` | `com.mysql.cj.jdbc.Driver` | JDBC 驱动；OceanBase 类型留空或保持默认时脚本会自动使用 `com.oceanbase.jdbc.Driver` |
| `CREST_DB_URL` | 空 | JDBC URL 覆盖项；留空时安装脚本按 `CREST_DB_TYPE` 自动生成 |
| `CREST_DB_USERNAME` | `root` | Crest 系统库用户；OBProxy 连接 OceanBase 时使用 `用户@租户#集群`，observer 直连时使用 `用户@租户` |
| `CREST_DB_PASSWORD` | 空 | 留空时安装脚本自动生成 |
| `CREST_INTERNAL_REDIS` | `true` | 是否启动安装包内置 `redis-crest` 容器；使用外部 Redis 或旧版单机兼容模式时可设为 `false` |
| `CREST_CACHE_TYPE` | `jcache` | 缓存类型；生产模式会自动切换为 `redis` |
| `CREST_REDIS_HOST` | `redis-crest` | Redis 地址；默认连接包内 Redis 容器 |
| `CREST_REDIS_PORT` | `6379` | Redis 端口 |
| `CREST_REDIS_USERNAME` | 空 | Redis ACL 用户名 |
| `CREST_REDIS_PASSWORD` | 空 | Redis 密码；填写后包内 Redis 会启用 `requirepass` |
| `CREST_REDIS_DATABASE` | `0` | Redis 逻辑库；Redis Cluster 必须使用 `0` |
| `CREST_REDIS_CLUSTER_NODES` | 空 | Redis Cluster 节点列表，格式 `host1:6379,host2:6379`；配置后应用按 Cluster 连接 |
| `CREST_TASK_QUEUE_ENABLED` | 空 | 是否启用 Redis Streams 任务队列；未配置时生产模式默认启用，旧版单机模式默认关闭 |
| `CREST_WEBSOCKET_BROADCAST_ENABLED` | 空 | 是否启用 WebSocket Redis 广播；未配置时生产模式默认启用，旧版单机模式默认关闭 |
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

### Redis 使用模式

默认安装使用包内 Redis：

```bash
CREST_INTERNAL_REDIS=true
CREST_REDIS_HOST=redis-crest
CREST_CACHE_TYPE=jcache
```

旧版单机环境如果暂时不使用 Redis，可显式关闭包内 Redis，并保持单机兼容模式：

```bash
CREST_INTERNAL_REDIS=false
CREST_PRODUCTION_MODE=false
CREST_CACHE_TYPE=jcache
CREST_TASK_QUEUE_ENABLED=false
CREST_WEBSOCKET_BROADCAST_ENABLED=false
```

该模式不会启动 `redis-crest`，也不会把后端服务依赖到 Redis。它只适用于单机或旧版兼容部署；多副本、任务队列、WebSocket 跨节点广播和生产模式必须使用 Redis。

使用外部 Redis 时，不启动包内 Redis，改填外部连接：

```bash
CREST_INTERNAL_REDIS=false
CREST_REDIS_HOST=redis.example.com
CREST_REDIS_PORT=6379
CREST_REDIS_PASSWORD=your_password
CREST_CACHE_TYPE=redis
```

如启用 `CREST_PRODUCTION_MODE=true`，必须配置可用 Redis；`CREST_INTERNAL_REDIS=false` 时不能继续使用默认的 `CREST_REDIS_HOST=redis-crest`。

OceanBase 系统库示例：

OceanBase 按企业外部系统库处理。生产环境建议先由 DBA 创建租户、数据库或 schema、账号和权限，使用 `obclient --default-character-set=utf8mb4` 执行离线包 `init-sql/ob-mysql/` 或 `init-sql/ob-oracle/` 下的初始化 SQL，再配置 Crest 连接信息启动服务。安装脚本在 OceanBase 模式下默认设置 `CREST_FLYWAY_ENABLED=false`，避免应用启动时直接修改外部系统库；开发验证需要应用自动初始化空库时，才显式设置 `CREST_FLYWAY_ENABLED=true`。

```bash
# OceanBase MySQL tenant through OBProxy
CREST_DB_TYPE=ob-mysql
CREST_DB_HOST=10.0.0.10
CREST_DB_PORT=2883
CREST_DB_NAME=crest
CREST_DB_USERNAME=<user>@<tenant>#<cluster>
CREST_DB_PASSWORD=<password>
```

`CREST_DB_TYPE=ob-mysql` 使用独立的 OceanBase MySQL 兼容初始化脚本，保留 MySQL 兼容表结构，同时去除 OceanBase 会提示 warning 的 MySQL 专属表引擎选项；不会加载 MySQL 演示迁移。默认 MySQL 安装仍使用原有迁移目录和自动初始化行为。

```bash
# OceanBase Oracle tenant through OBProxy
CREST_DB_TYPE=ob-oracle
CREST_DB_HOST=10.0.0.10
CREST_DB_PORT=2883
CREST_DB_USERNAME=<user>@<tenant>#<cluster>
CREST_DB_PASSWORD=<password>
```

如需 observer 直连，把端口改为 `2881`，用户名使用 `用户@租户`，不要带 `#集群`。`CREST_DB_URL` 留空时脚本会自动生成；只有需要追加特殊 JDBC 参数时才手工覆盖。

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

企业已有 Prometheus/Grafana 时，不需要启动内置 `crest-prometheus` 和 `crest-grafana`。只开启 `CREST_PROMETHEUS_ENABLED=true`，设置 `CREST_PROMETHEUS_TOKEN`，由企业 Prometheus 抓取后端服务地址，再导入 `crest/observability/grafana/provisioning/dashboards/*.json` 和 `crest/observability/prometheus/rules/crest-alerts.yml`。

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

全新环境可直接使用安装脚本部署 v2.0.0。已有 v1.5.x 环境升级到 v2.0.0 时，请先备份 Crest 系统库和运行目录，再按当前版本发布说明执行对应升级步骤；更早版本迁移到 v2.0.0 时，请先备份 Crest 系统库和运行目录，导出业务资源，再在新环境中导入。

同一发布线的小版本升级前：

```bash
crestctl backup stop
```

确认 release notes 明确支持原地升级后，替换镜像标签并在运行目录执行：

```bash
cd /opt/crest
docker-compose up -d --no-deps crest-service crest
```

如果发布说明提供升级 SQL，请先确认已经完成备份，再执行对应文件。MySQL 系统库从 v1.5.7 及更早版本升级到 v2.0.0 时，先执行 v1.5.7 到 v1.5.8 的历史升级 SQL：

```bash
set -a
source /opt/crest/.env
set +a
docker exec -i mysql-crest mysql -uroot -p"${CREST_DB_PASSWORD}" "${CREST_DB_NAME:-crest}" < upgrade-sql/v1.5.7_to_v1.5.8.sql
```

如果源版本是 v1.5.4，先执行 v1.5.4 到 v1.5.5 的历史升级 SQL，再执行上面的 v1.5.7 到 v1.5.8 SQL：

```bash
set -a
source /opt/crest/.env
set +a
docker exec -i mysql-crest mysql -uroot -p"${CREST_DB_PASSWORD}" "${CREST_DB_NAME:-crest}" < upgrade-sql/v1.5.4_to_v1.5.5.sql
docker exec -i mysql-crest mysql -uroot -p"${CREST_DB_PASSWORD}" "${CREST_DB_NAME:-crest}" < upgrade-sql/v1.5.7_to_v1.5.8.sql
```

如安装时修改过 `CREST_BASE`，请把 `/opt/crest/.env` 换成实际运行目录中的 `.env`。外部 MySQL 环境可在数据库服务器或运维机执行同一 SQL 文件，目标库为 Crest 系统库。

继续按数据库类型执行 v2.0.0 和 v2.0.1 的增量 SQL。MySQL 系统库示例：

```bash
for sql in \
  upgrade-sql/v2.0.0_export_task_queue_mysql.sql \
  upgrade-sql/v2.0.0_datasource_sync_task_queue_mysql.sql \
  upgrade-sql/v2.0.0_dataset_sync_task_queue_mysql.sql \
  upgrade-sql/v2.0.0_dataset_sync_task_queue_metadata_mysql.sql \
  upgrade-sql/v2.0.0_scheduled_task_queue_state_mysql.sql \
  upgrade-sql/v2.0.0_audit_log_filter_index_mysql.sql \
  upgrade-sql/v2.0.0_to_v2.0.1_metadata_tables_mysql.sql \
  upgrade-sql/v2.0.0_to_v2.0.1_runtime_identifiers_mysql.sql
do
  docker exec -i mysql-crest mysql -uroot -p"${CREST_DB_PASSWORD}" "${CREST_DB_NAME:-crest}" < "${sql}"
done
```

OceanBase MySQL 系统库使用文件名带 `ob_mysql` 的脚本：

```bash
for sql in \
  upgrade-sql/v2.0.0_export_task_queue_ob_mysql.sql \
  upgrade-sql/v2.0.0_datasource_sync_task_queue_ob_mysql.sql \
  upgrade-sql/v2.0.0_dataset_sync_task_queue_ob_mysql.sql \
  upgrade-sql/v2.0.0_dataset_sync_task_queue_metadata_ob_mysql.sql \
  upgrade-sql/v2.0.0_scheduled_task_queue_state_ob_mysql.sql \
  upgrade-sql/v2.0.0_audit_log_filter_index_ob_mysql.sql \
  upgrade-sql/v2.0.0_to_v2.0.1_metadata_tables_ob_mysql.sql \
  upgrade-sql/v2.0.0_to_v2.0.1_runtime_identifiers_ob_mysql.sql
do
  obclient --default-character-set=utf8mb4 -h<obproxy-host> -P2883 -u'<user>@<tenant>#<cluster>' -p'<password>' <database> < "${sql}"
done
```

OceanBase Oracle 系统库使用文件名带 `ob_oracle` 的脚本，并以 Crest 系统库 schema 用户执行：

```bash
for sql in \
  upgrade-sql/v2.0.0_export_task_queue_ob_oracle.sql \
  upgrade-sql/v2.0.0_datasource_sync_task_queue_ob_oracle.sql \
  upgrade-sql/v2.0.0_dataset_sync_task_queue_ob_oracle.sql \
  upgrade-sql/v2.0.0_dataset_sync_task_queue_metadata_ob_oracle.sql \
  upgrade-sql/v2.0.0_scheduled_task_queue_state_ob_oracle.sql \
  upgrade-sql/v2.0.0_repair_seed_text_encoding_ob_oracle.sql \
  upgrade-sql/v2.0.0_audit_log_filter_index_ob_oracle.sql \
  upgrade-sql/v2.0.0_to_v2.0.1_metadata_tables_ob_oracle.sql \
  upgrade-sql/v2.0.0_to_v2.0.1_runtime_identifiers_ob_oracle.sql
do
  obclient --default-character-set=utf8mb4 -h<obproxy-host> -P2883 -u'<user>@<tenant>#<cluster>' -p'<password>' < "${sql}"
done
```

升级后检查：

```bash
crestctl status
docker logs crest --tail 200
```

确认点：

- `crest` 和 `crest-service` 容器健康；内置 MySQL 模式还需要确认 `mysql-crest` 健康；
- MySQL 模式确认 Flyway 迁移成功；OceanBase 模式确认对应增量 SQL 已由 DBA 执行完成；
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

脚本会删除固定演示资源 ID、`crest_demo_retail` 演示库，并把运行配置改为后续不再初始化演示资源。执行前建议先备份 Crest 系统库。

## 离线包制作

发布包按 CPU 架构分别制作：

```bash
bash make-offline-package.sh v2.0.0 linux-amd64
bash make-offline-package.sh v2.0.0 linux-arm64
bash make-app-image-package.sh v2.0.0 linux-amd64
bash make-app-image-package.sh v2.0.0 linux-arm64
```

默认输出目录：

```text
../offline-packages
```

发布包名：

```text
crest-offline-v2.0.0-linux-amd64.tar.gz
crest-offline-v2.0.0-linux-arm64.tar.gz
crest-app-images-v2.0.0-linux-amd64.tar.gz
crest-app-images-v2.0.0-linux-arm64.tar.gz
```

制作前需要本机已有对应架构镜像。下面的 `*-amd64`、`*-arm64` 是本地制作离线包时使用的镜像标签；GitHub Actions 会从多架构发布镜像生成这些本地标签后再打包。

```text
ghcr.io/sevoniva/crest-service:v2.0.0-amd64
ghcr.io/sevoniva/crest-service:v2.0.0-arm64
ghcr.io/sevoniva/crest-web:v2.0.0-amd64
ghcr.io/sevoniva/crest-web:v2.0.0-arm64
mysql:8.0.43
redis:7.4.5-alpine
```

如需把 Prometheus 和 Grafana 镜像也放入离线包，先准备对应架构镜像，再设置：

```bash
INCLUDE_OBSERVABILITY_IMAGES=true \
PROMETHEUS_IMAGE=prom/prometheus:v3.12.0 \
GRAFANA_IMAGE=grafana/grafana-oss:13.0.1 \
bash make-offline-package.sh v2.0.0 linux-amd64
```

也可以通过环境变量指定来源镜像：

```bash
SOURCE_BACKEND_IMAGE=ghcr.io/sevoniva/crest-service:v2.0.0-amd64 \
BACKEND_IMAGE=ghcr.io/sevoniva/crest-service:v2.0.0-amd64 \
SOURCE_FRONTEND_IMAGE=ghcr.io/sevoniva/crest-web:v2.0.0-amd64 \
FRONTEND_IMAGE=ghcr.io/sevoniva/crest-web:v2.0.0-amd64 \
SOURCE_MYSQL_IMAGE=mysql:8.0.43-amd64 \
MYSQL_IMAGE=mysql:8.0.43 \
SOURCE_REDIS_IMAGE=redis:7.4.5-alpine-amd64 \
REDIS_IMAGE=redis:7.4.5-alpine \
bash make-offline-package.sh v2.0.0 linux-amd64
```

完整离线安装包脚本会校验前端、后端、MySQL 和 Redis 镜像架构，架构不一致会直接停止。`SOURCE_*_IMAGE` 用于本机制作来源，包内 `install.conf` 仍写入指定的目标镜像名。

离线包会包含 `upgrade-sql/` 目录。全新安装不需要手动执行；升级已有环境时按发布说明选择对应 SQL。

应用镜像升级包只包含 Crest 后端镜像、前端镜像、`init-sql/`、`upgrade-sql/` 和加载说明，不包含 MySQL、Redis、Prometheus、Grafana 镜像，也不包含完整安装脚本。已有环境只需要替换应用服务时，优先使用 `crest-app-images-*`。

## 离线安装

在目标服务器执行：

```bash
tar -zxf crest-offline-v2.0.0-linux-amd64.tar.gz
cd crest-offline-v2.0.0-linux-amd64
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
| `/opt/crest/data/redis` | 包内 Redis 数据 |
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
| 数据库连接失败 | 内置 MySQL 模式检查 `mysql-crest` 健康和 `.env` 密码；外部 OceanBase 模式检查 OBProxy/observer 地址、租户用户名、密码和网络连通性 |
| 升级后启动失败 | MySQL 模式检查 Flyway 迁移日志；OceanBase 模式检查增量 SQL 执行结果、数据库备份是否可恢复、镜像架构是否正确 |
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
