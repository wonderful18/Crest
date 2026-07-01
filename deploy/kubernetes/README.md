# Crest Kubernetes 部署

本目录提供 Crest 在 Kubernetes 上的基础部署清单。当前支持四种方式：

- `internal-mysql`：同时安装 Crest 和 MySQL；
- `external-mysql`：只安装 Crest，连接外部 MySQL；
- `internal-mysql-observability`：内置 MySQL，并启用 Prometheus 和 Grafana；
- `external-mysql-observability`：外部 MySQL，并启用 Prometheus 和 Grafana。

两种方式共用 `base` 中的应用 Deployment、Service、PVC 和配置模板，差异在数据库地址、数据库 Secret 和是否创建 MySQL StatefulSet。

## 目录结构

```text
deploy/kubernetes
├── base
│   ├── configmap-app.yaml
│   ├── configmap-nginx.yaml
│   ├── deployment.yaml
│   ├── kustomization.yaml
│   ├── pvc.yaml
│   ├── service.yaml
│   └── serviceaccount.yaml
├── internal-mysql
│   ├── kustomization.yaml
│   ├── mysql-init.sql
│   ├── mysql-my.cnf
│   ├── mysql.yaml
│   └── namespace.yaml
├── external-mysql
│   ├── kustomization.yaml
│   └── namespace.yaml
├── observability
│   ├── grafana
│   ├── prometheus
│   ├── kustomization.yaml
│   ├── networkpolicy.yaml
│   └── observability.yaml
├── internal-mysql-observability
│   ├── kustomization.yaml
│   └── observability-env-patch.yaml
└── external-mysql-observability
    ├── kustomization.yaml
    └── observability-env-patch.yaml
```

## 镜像

默认应用镜像：

```text
ghcr.io/sevoniva/crest-service:v1.5.14
ghcr.io/sevoniva/crest-web:v1.5.14
```

后端镜像以 Alpine 为最终基础镜像，内置 jlink 裁剪后的 Java 21 runtime。前端镜像基于 Nginx Alpine，负责静态资源、API 转发和 WebSocket 转发。两个应用容器都以固定非 root 用户运行：

```text
uid: 10001
gid: 10001
```

默认安全上下文：

- `runAsNonRoot: true`
- `allowPrivilegeEscalation: false`
- `capabilities.drop: ALL`
- `seccompProfile: RuntimeDefault`
- 应用容器 `readOnlyRootFilesystem: true`

本地构建镜像：

```bash
cd core/core-frontend
pnpm install --frozen-lockfile
pnpm run build:base
pnpm run build:lite:check

cd ../..
mvn -s .mvn/settings.xml -pl :core-backend -am clean package -Pstandalone -DskipTests -Dmaven.test.skip=true -Dcrest.copy.frontend.skip=true
docker build -f Dockerfile.backend -t ghcr.io/sevoniva/crest-service:main .
docker build -f Dockerfile.frontend -t ghcr.io/sevoniva/crest-web:main .
```

## 健康检查

后端服务使用三类探针：

| 探针 | 路径 | 阈值 | 设计目的 |
| --- | --- | --- | --- |
| startupProbe | `${CREST_CONTEXT_PATH}/api/v1/actuator/health/liveness` | 启动后延迟 30 秒开始检查，每 5 秒检查一次，超时 3 秒，最多失败 120 次 | 给镜像冷启动、Flyway 迁移和演示数据初始化预留最长 10 分钟，启动完成前不触发存活探针 |
| readinessProbe | `${CREST_CONTEXT_PATH}/api/v1/actuator/health/readiness` | 每 10 秒检查一次，超时 5 秒，连续失败 3 次摘除流量，成功 1 次恢复 | 校验数据库连接，避免服务已启动但无法处理业务请求时继续接收流量 |
| livenessProbe | `${CREST_CONTEXT_PATH}/api/v1/actuator/health/liveness` | 每 30 秒检查一次，超时 5 秒，连续失败 3 次重启容器 | 只判断进程和 Web 容器是否存活，避免因为数据库短抖动或慢查询误杀应用 |

`readiness` 依赖元数据库，数据库不可用时返回 `503`。`liveness` 不访问数据库，只用于判断应用进程是否还能响应。

前端网关使用 `/healthz` 做 startup、readiness 和 liveness 检查。该检查只判断 Nginx 进程和站点配置是否可用，不访问后端数据库，避免数据库短暂抖动导致前端容器被误重启。

前端 Nginx 不直接把客户端 `Host` 透传给后端。需要 SSO 动态回调或后端生成对外链接时，请在 `base/configmap-nginx.yaml` 中将 `server_name` 设置为实际对外域名。

## 配置项

`crest-env` ConfigMap:

| 配置项 | 说明 |
| --- | --- |
| `CREST_CONTEXT_PATH` | 应用上下文路径，默认空 |
| `CREST_LOAD_DEMO` | 是否初始化内置演示资源，默认 `true` |
| `CREST_FLYWAY_LOCATIONS` | Flyway 执行目录；关闭演示资源时设为 `classpath:db/migration` |
| `CREST_DB_HOST` | MySQL 地址 |
| `CREST_DB_PORT` | MySQL 端口 |
| `CREST_DB_NAME` | 元数据库名 |
| `CREST_DB_PARAMS` | JDBC 参数 |
| `CREST_ORIGIN_LIST` | 允许来源列表，正式环境改为实际域名 |
| `CREST_LOGIN_TIMEOUT` | 登录超时时间，默认 960 分钟 |
| `CREST_PROMETHEUS_ENABLED` | 是否启用 Prometheus 指标端点，默认 `false` |
| `CREST_PROMETHEUS_MAX_URI_TAGS` | 接口 URI 指标标签数量上限，默认 `200` |
| `CREST_GRAFANA_ENABLED` | 是否在系统页面显示 Grafana 已启用 |
| `CREST_GRAFANA_PUBLIC_URL` | Grafana 外部访问地址 |

`crest-db-secret` Secret:

| 配置项 | 说明 |
| --- | --- |
| `CREST_DB_USERNAME` | 元数据库账号 |
| `CREST_DB_PASSWORD` | 元数据库密码 |
| `MYSQL_ROOT_PASSWORD` | 内置 MySQL 模式需要；外部 MySQL 模式不需要 |
| `CREST_AES_KEY` | 32 位加密 Key |
| `CREST_AES_IV` | 16 位加密 IV |
| `CREST_CRYPTO_MODE` | 加密模式，默认 `standard`；需要 SM2/SM3/SM4 时设为 `sm-suite` |
| `CREST_SM4_KEY` | SM4 使用的 16 位 Key；`CREST_CRYPTO_MODE=sm-suite` 时必填，服务启动会校验 |
| `CREST_INITIAL_PASSWORD` | 管理员初始密码 |

`crest-db-secret` 不由 Kustomize 生成，部署前必须在目标 namespace 中创建。不要把生产密码、证书或 kubeconfig 提交到仓库。

`crest-observability-secret` Secret:

| 配置项 | 说明 |
| --- | --- |
| `CREST_PROMETHEUS_TOKEN` | Prometheus 抓取后端指标使用的 Bearer Token |
| `GF_SECURITY_ADMIN_PASSWORD` | Grafana 管理员密码 |

## 内置 MySQL 部署

适合快速验证、单集群测试或不依赖外部数据库的环境。

先创建 namespace 和 Secret：

```bash
kubectl create namespace crest-internal --dry-run=client -o yaml | kubectl apply -f -

kubectl -n crest-internal create secret generic crest-db-secret \
  --from-literal=CREST_DB_USERNAME=root \
  --from-literal=CREST_DB_PASSWORD='<strong-db-password>' \
  --from-literal=MYSQL_ROOT_PASSWORD='<strong-db-password>' \
  --from-literal=CREST_AES_KEY='<32-character-aes-key>' \
  --from-literal=CREST_AES_IV='<16-character-aes-iv>' \
  --from-literal=CREST_CRYPTO_MODE=standard \
  --from-literal=CREST_SM4_KEY='<16-character-sm4-key>' \
  --from-literal=CREST_INITIAL_PASSWORD='<initial-admin-password>' \
  --dry-run=client -o yaml | kubectl apply -f -
```

再部署应用：

```bash
kubectl apply -k deploy/kubernetes/internal-mysql
kubectl -n crest-internal rollout status statefulset/crest-mysql --timeout=180s
kubectl -n crest-internal rollout status deployment/crest-service --timeout=300s
kubectl -n crest-internal rollout status deployment/crest --timeout=300s
kubectl -n crest-internal port-forward svc/crest 18100:8100
```

访问：

```text
http://127.0.0.1:18100/index.html
```

账号：

```text
用户名：admin
密码：`crest-db-secret` 中 `CREST_INITIAL_PASSWORD` 的值
```

内置 MySQL 默认使用：

```text
mysql:8.0.43
```

如果集群无法访问 Docker Hub，需要先把 MySQL 镜像同步到内部镜像仓库，再修改 `internal-mysql/mysql.yaml`。

## 外部 MySQL 部署

适合正式环境。先准备数据库：

```sql
CREATE DATABASE crest DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

修改 `deploy/kubernetes/external-mysql/kustomization.yaml`：

```yaml
configMapGenerator:
  - name: crest-env
    literals:
      - CREST_DB_HOST=mysql.example.com
      - CREST_DB_PORT=3306
      - CREST_DB_NAME=crest
      - CREST_DB_PARAMS=autoReconnect=false&useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
```

创建 namespace 和 Secret：

```bash
kubectl create namespace crest-external --dry-run=client -o yaml | kubectl apply -f -

kubectl -n crest-external create secret generic crest-db-secret \
  --from-literal=CREST_DB_USERNAME='<db-user>' \
  --from-literal=CREST_DB_PASSWORD='<db-password>' \
  --from-literal=CREST_AES_KEY='<32-character-aes-key>' \
  --from-literal=CREST_AES_IV='<16-character-aes-iv>' \
  --from-literal=CREST_CRYPTO_MODE=standard \
  --from-literal=CREST_SM4_KEY='<16-character-sm4-key>' \
  --from-literal=CREST_INITIAL_PASSWORD='<initial-admin-password>' \
  --dry-run=client -o yaml | kubectl apply -f -
```

再部署应用：

```bash
kubectl apply -k deploy/kubernetes/external-mysql
kubectl -n crest-external rollout status deployment/crest-service --timeout=300s
kubectl -n crest-external rollout status deployment/crest --timeout=300s
kubectl -n crest-external port-forward svc/crest 18101:8100
```

访问：

```text
http://127.0.0.1:18101/index.html
```

## Prometheus 和 Grafana

可观测性 overlay 会部署：

- `crest-prometheus`：ClusterIP 服务，抓取 `crest-service:8100/api/v1/actuator/prometheus`；
- `crest-grafana`：ClusterIP 服务，自动加载 Prometheus 数据源和 Crest 看板；
- `NetworkPolicy`：只允许前端网关和 Prometheus 访问后端 `8100` 端口。

企业集群已有 Prometheus/Grafana 时，可不使用 `observability` overlay。只需在应用环境中设置 `CREST_PROMETHEUS_ENABLED=true`，通过 Secret 注入 `CREST_PROMETHEUS_TOKEN`，再由企业 Prometheus 按 Bearer Token 抓取 `crest-service:8100/api/v1/actuator/prometheus`。Grafana 可导入 `deploy/kubernetes/observability/grafana/dashboards/` 下的 Crest 看板，Prometheus 可加载 `deploy/kubernetes/observability/prometheus/rules/crest-alerts.yml`。

创建 Secret：

```bash
kubectl -n crest-internal create secret generic crest-observability-secret \
  --from-literal=CREST_PROMETHEUS_TOKEN='<prometheus-scrape-token>' \
  --from-literal=GF_SECURITY_ADMIN_PASSWORD='<grafana-admin-password>' \
  --dry-run=client -o yaml | kubectl apply -f -
```

内置 MySQL 模式：

```bash
kubectl apply -k deploy/kubernetes/internal-mysql-observability
kubectl -n crest-internal rollout status deployment/crest-prometheus --timeout=180s
kubectl -n crest-internal rollout status deployment/crest-grafana --timeout=180s
kubectl -n crest-internal port-forward svc/crest-grafana 3000:3000
```

外部 MySQL 模式把 namespace 改成 `crest-external`，并使用：

```bash
kubectl apply -k deploy/kubernetes/external-mysql-observability
```

验证指标端点：

```bash
kubectl -n crest-internal run crest-metrics-check --rm -i --restart=Never --image=busybox:1.36 -- \
  wget -qO- --header='Authorization: Bearer <prometheus-scrape-token>' \
  http://crest-service:8100/api/v1/actuator/prometheus
```

前端网关不公开 Prometheus 指标路径：

```bash
kubectl -n crest-internal port-forward svc/crest 18100:8100
curl -i http://127.0.0.1:18100/api/v1/actuator/prometheus
```

返回 `404` 即符合预期。

## 持久化

`base/pvc.yaml` 创建 `crest-data` PVC，用于保存：

- 静态资源；
- 外观配置；
- 导出文件；
- Excel 文件；
- 字体文件；
- i18n 文件。

应用日志和缓存默认使用 `emptyDir`：

| Volume | 类型 | 说明 |
| --- | --- | --- |
| `crest-cache` | `emptyDir`，内存 | 后端应用缓存 |
| `crest-logs` | `emptyDir`，内存 | 后端容器内日志 |
| `crest-tmp` | `emptyDir`，内存 | 后端临时文件 |
| `nginx-*` | `emptyDir` 或内存 | 前端 Nginx 运行目录和临时目录 |
| `crest-data` | PVC | 业务持久化数据 |
| `crest-prometheus-data` | PVC | Prometheus TSDB，启用可观测性时创建 |
| `crest-grafana-data` | PVC | Grafana 本地数据，启用可观测性时创建 |

正式环境如需长期保存应用日志，建议接入集群日志采集系统，或按现场规范改为持久化卷。

## 本地 kind 验证

构建并加载镜像：

```bash
mvn -s .mvn/settings.xml -pl :core-backend -am clean package -Pstandalone -DskipTests -Dmaven.test.skip=true -Dcrest.copy.frontend.skip=true
docker build -f Dockerfile.backend -t ghcr.io/sevoniva/crest-service:v1.5.14 .
docker build -f Dockerfile.frontend -t ghcr.io/sevoniva/crest-web:v1.5.14 .
kind load docker-image ghcr.io/sevoniva/crest-service:v1.5.14 --name crest-kind
kind load docker-image ghcr.io/sevoniva/crest-web:v1.5.14 --name crest-kind
kind load docker-image mysql:8.0.43 --name crest-kind
```

如果 `kind load` 后 MySQL 名称不是 `docker.io/library/mysql:8.0.43`，可以在 kind 节点里补标签：

```bash
docker exec crest-kind-control-plane \
  ctr --namespace=k8s.io images tag docker.io/crest-kind/mysql:8.0.43 docker.io/library/mysql:8.0.43
```

部署内置 MySQL 模式：

```bash
kubectl create namespace crest-internal --dry-run=client -o yaml | kubectl apply -f -
kubectl -n crest-internal create secret generic crest-db-secret \
  --from-literal=CREST_DB_USERNAME=root \
  --from-literal=CREST_DB_PASSWORD='<strong-db-password>' \
  --from-literal=MYSQL_ROOT_PASSWORD='<strong-db-password>' \
  --from-literal=CREST_AES_KEY='<32-character-aes-key>' \
  --from-literal=CREST_AES_IV='<16-character-aes-iv>' \
  --from-literal=CREST_CRYPTO_MODE=standard \
  --from-literal=CREST_SM4_KEY='<16-character-sm4-key>' \
  --from-literal=CREST_INITIAL_PASSWORD='<initial-admin-password>' \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -k deploy/kubernetes/internal-mysql
kubectl -n crest-internal get pods
kubectl -n crest-internal rollout status deployment/crest-service --timeout=300s
kubectl -n crest-internal rollout status deployment/crest --timeout=300s
```

外部 MySQL 模式在 Docker Desktop 上可使用宿主机地址 `host.docker.internal`。例如本机 MySQL 映射到 `13306`：

```bash
docker exec crest-mysql-local mysql -uroot -p'<mysql-root-password>' \
  -e "DROP DATABASE IF EXISTS crest_k8s_external; CREATE DATABASE crest_k8s_external DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

## 验证清单

部署后检查：

```bash
kubectl -n crest-internal get pods
kubectl -n crest-internal logs deploy/crest-service --tail=200
kubectl -n crest-internal logs deploy/crest --tail=200
kubectl -n crest-internal describe pod -l app.kubernetes.io/name=crest
```

功能验证：

- Pod 全部 Ready，`crest`、`crest-service` 和 `crest-mysql` 重启次数为 0；
- `/api/v1/actuator/health/liveness` 返回 200；
- `/api/v1/actuator/health/readiness` 返回 200；
- 前端网关访问 `/api/v1/actuator/prometheus` 返回 404；
- 启用可观测性时，Prometheus 通过 Bearer Token 能抓到 `http_server_requests_seconds_count`；
- 启用可观测性时，Grafana Crest 看板和 Prometheus 告警规则加载成功；
- `/index.html` 返回 200；
- `/doc.html` 可打开或跳转到登录流程；
- `/v3/api-docs` 在未登录时返回受保护状态，登录或放行接口文档后可正常读取；
- 使用 `CREST_INITIAL_PASSWORD` 配置的初始管理员密码可以登录；
- `crest_schema_version` 最新迁移成功；
- 启用 `CREST_LOAD_DEMO` 时，初始状态包含零售经营和研发效能演示数据源、数据集、图表和数据大屏；
- 工作台、数据源、数据集、仪表盘、数据大屏、数据血缘、系统设置和导出中心能打开；
- 应用日志没有持续的 `WARN`、`ERROR` 或 `Exception`。

## 内置演示资源

Kubernetes 清单默认加载内置演示资源。如需安装纯净环境，部署前在对应 `kustomization.yaml` 中设置：

```yaml
configMapGenerator:
  - name: crest-env
    literals:
      - CREST_LOAD_DEMO=false
      - CREST_FLYWAY_LOCATIONS=classpath:db/migration
```

已加载演示资源的环境需要先把以上两个配置改好并重启应用，再在元数据库中执行：

```text
installer/crest/bin/mysql/cleanup-demo-data.sql
```

执行前请先备份元数据库；脚本只删除固定演示资源 ID 和 `crest_demo_retail` 演示库。

## 迁移和同发布线升级

全新环境可直接使用 Kubernetes 清单部署 v1.5.14。已有 v1.5.8 及之后版本升级到 v1.5.14 时，请先备份元数据库和持久化目录，执行 `installer/upgrade-sql/v1.5.13_to_v1.5.14.sql`，再更新前后端镜像。已有 v1.5.7 环境升级到 v1.5.14 时，请先执行 `installer/upgrade-sql/v1.5.7_to_v1.5.8.sql`，再执行 `installer/upgrade-sql/v1.5.13_to_v1.5.14.sql` 并更新前后端镜像。更早版本迁移到 v1.5.14 时，请先备份元数据库和持久化目录，导出业务资源，再部署新环境导入。

同一发布线的小版本升级前：

1. 备份外部 MySQL 或内置 MySQL PVC；
2. 记录当前镜像标签；
3. 准备新镜像并确认架构；
4. 在测试命名空间完成一次迁移验证。

确认 release notes 明确支持原地升级后，更新镜像：

```bash
kubectl -n crest-internal set image deployment/crest-service crest-service=ghcr.io/sevoniva/crest-service:v1.5.14
kubectl -n crest-internal set image deployment/crest crest=ghcr.io/sevoniva/crest-web:v1.5.14
kubectl -n crest-internal rollout status deployment/crest-service --timeout=300s
kubectl -n crest-internal rollout status deployment/crest --timeout=300s
```

如需回滚：

```bash
kubectl -n crest-internal rollout undo deployment/crest-service
kubectl -n crest-internal rollout undo deployment/crest
```

数据库迁移由 Flyway 自动执行。迁移失败时不要反复重启覆盖现场，先保存日志和数据库备份。

## 清理

```bash
kubectl delete ns crest-internal
kubectl delete ns crest-external
```

删除命名空间会删除命名空间内的 PVC。正式环境清理前先确认备份。

## 公开仓库注意事项

公开清单不包含真实 Secret。部署时由运维在目标 namespace 中创建 `crest-db-secret`，或接入集群侧 Secret 管理系统。不要提交真实密码、内网地址、证书、kubeconfig、镜像仓库凭据或生产导出的 YAML。
