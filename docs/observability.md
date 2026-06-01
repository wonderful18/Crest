# Crest 可观测性

Crest 可以按需启用 Prometheus 和 Grafana。默认安装不启动监控组件，后端也不会暴露 Prometheus 指标端点。

启用后，后端通过 `/api/v1/actuator/prometheus` 输出指标，Prometheus 在内网直接抓取 `crest-service:8100`。抓取请求必须携带 Bearer Token；没有 token、token 错误或端点未开启时，服务端不会返回指标。前端 Nginx 默认对外屏蔽该路径。

Grafana 通过 provisioning 自动加载 Prometheus 数据源和“Crest 可观测性总览”看板。看板覆盖：

- API 请求速率、错误率、P95 延迟和状态码分布；
- 按 URI、Method、Status 汇总的接口明细；
- JVM 内存、GC、线程；
- 运行时线程池；
- 进程 CPU 和采集状态。

## 接入模式

Crest 不要求必须使用内置 Prometheus/Grafana。生产环境可按现场监控体系选择：

| 模式 | 适用场景 | 做法 |
| --- | --- | --- |
| 只开启 metrics | 企业已有 Prometheus 和 Grafana | 设置 `CREST_PROMETHEUS_ENABLED=true` 和 `CREST_PROMETHEUS_TOKEN`，在企业 Prometheus 中增加抓取任务，Grafana 导入 Crest dashboard |
| 内置 Prometheus/Grafana | 单机验证、小规模内网部署、无统一监控平台 | 设置 `CREST_PROMETHEUS_ENABLED=true` 和 `CREST_GRAFANA_ENABLED=true`，由 Compose 或 Kubernetes overlay 启动配套组件 |

企业 Prometheus 抓取示例：

```yaml
scrape_configs:
  - job_name: crest-service
    metrics_path: /api/v1/actuator/prometheus
    scheme: http
    bearer_token: <prometheus-scrape-token>
    static_configs:
      - targets:
          - crest-service:8100
```

Grafana dashboard 文件：

```text
installer/crest/observability/grafana/provisioning/dashboards/crest-observability.json
```

## 单机安装

安装前在 `installer/install.conf` 中打开：

```bash
CREST_PROMETHEUS_ENABLED=true
CREST_GRAFANA_ENABLED=true
CREST_GRAFANA_PORT=3000
```

`CREST_PROMETHEUS_TOKEN` 留空时，安装脚本会生成随机 token，并写入：

```text
${CREST_BASE}/crest/conf/prometheus-token
```

Grafana 管理员密码由 `CREST_GRAFANA_ADMIN_PASSWORD` 控制，留空时同样自动生成。安装完成后：

```bash
crestctl status
docker ps --filter name=crest-prometheus
docker ps --filter name=crest-grafana
```

访问 Grafana：

```text
http://服务器IP:3000
```

Prometheus 端口不对宿主机开放。需要临时排查时，在服务器上进入容器网络或使用运维侧端口转发，不建议直接暴露 `9090`。

## 本地 split 验证

先准备 token 文件：

```bash
printf '%s' 'local-prometheus-token' > deploy/split/prometheus-token
chmod 0400 deploy/split/prometheus-token
```

启动：

```bash
COMPOSE_PROFILES=observability \
CREST_PROMETHEUS_ENABLED=true \
CREST_PROMETHEUS_TOKEN=local-prometheus-token \
CREST_PROMETHEUS_TOKEN_FILE=./prometheus-token \
CREST_GRAFANA_ENABLED=true \
CREST_GRAFANA_ADMIN_PASSWORD=admin \
CREST_WEB_PORT=18100 \
docker compose -f deploy/split/docker-compose.yml up -d
```

验证：

```bash
curl -i http://127.0.0.1:18100/api/v1/actuator/prometheus
docker exec split-crest-prometheus wget -qO- http://crest-service:8100/api/v1/actuator/prometheus \
  --header='Authorization: Bearer local-prometheus-token' | head
```

第一条应返回 `404`，说明前端网关没有公开指标路径。第二条应返回 Prometheus 文本指标。

Grafana 地址：

```text
http://127.0.0.1:3000
```

## Kubernetes

启用可观测性时，需要额外创建 `crest-observability-secret`。

内置 MySQL 模式：

```bash
kubectl -n crest-internal create secret generic crest-observability-secret \
  --from-literal=CREST_PROMETHEUS_TOKEN='<prometheus-scrape-token>' \
  --from-literal=GF_SECURITY_ADMIN_PASSWORD='<grafana-admin-password>' \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -k deploy/kubernetes/internal-mysql-observability
```

外部 MySQL 模式：

```bash
kubectl -n crest-external create secret generic crest-observability-secret \
  --from-literal=CREST_PROMETHEUS_TOKEN='<prometheus-scrape-token>' \
  --from-literal=GF_SECURITY_ADMIN_PASSWORD='<grafana-admin-password>' \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -k deploy/kubernetes/external-mysql-observability
```

Grafana 默认是 ClusterIP 服务，按现场网关规范暴露。临时查看可使用：

```bash
kubectl -n crest-internal port-forward svc/crest-grafana 3000:3000
```

Prometheus 同样只在集群内访问：

```bash
kubectl -n crest-internal port-forward svc/crest-prometheus 9090:9090
```

## 系统页面

管理员登录后进入：

```text
系统设置 -> 系统参数 -> 可观测性
```

页面展示 Prometheus 是否开启、指标端点、token 是否已配置、Grafana 访问地址和已加载看板名称。页面不会展示或保存 Prometheus token。

## 指标口径

接口指标来自 Spring Boot Actuator 和 Micrometer。核心指标包括：

| 指标 | 口径 |
| --- | --- |
| `http_server_requests_seconds_count` | HTTP 请求次数，按 `method`、`uri`、`status`、`outcome` 等标签聚合 |
| `http_server_requests_seconds_sum` | HTTP 请求耗时总和 |
| `http_server_requests_seconds_bucket` | HTTP 请求耗时直方图，用于 P95、P99 计算 |
| `jvm_memory_used_bytes` | JVM 内存使用量 |
| `jvm_gc_pause_seconds_*` | GC 停顿次数和耗时 |
| `jvm_threads_*` | JVM 线程数量 |
| `executor_*` | WebSocket、审计日志、调度等运行时线程池状态 |
| `process_cpu_usage` | 当前进程 CPU 使用率 |
| `up` | Prometheus 采集状态 |

`CREST_PROMETHEUS_MAX_URI_TAGS` 用于限制 `http.server.requests` 的 URI 标签数量，默认 `200`。生产环境 URI 模板异常增长时，应先排查路由或代理配置，避免高基数指标压垮 Prometheus。

## 安全建议

- 不要把 `CREST_PROMETHEUS_TOKEN` 写入公开文档、截图或前端页面；
- 生产环境通过 Docker env、Kubernetes Secret 或企业密钥系统管理 token；
- Prometheus 服务不直接暴露公网端口；
- Grafana 使用独立强密码，并接入现场 SSO 或网关认证；
- 集群支持 NetworkPolicy 时，保留 `deploy/kubernetes/observability/networkpolicy.yaml`，只允许前端网关和 Prometheus 访问后端服务；
- 指标数据可能包含接口路径、状态码和异常标签，按内部运维数据管理。
