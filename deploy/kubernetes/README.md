# Crest Kubernetes 部署

本目录只保留一个部署清单：

```text
crest-ob-oracle-redis.yaml
```

适用场景：OceanBase Oracle 系统库、单机 Redis 6.2、一个后端 Pod、一个前端 Pod、后端持久卷、Redis 持久卷。

## 需要改的地方

在 `crest-ob-oracle-redis.yaml` 中只改这些：

| 配置 | 说明 |
| --- | --- |
| `CREST_ORIGIN_LIST` | 生产访问地址 |
| `CREST_DB_HOST` | OBProxy 或 observer 地址 |
| `CREST_DB_PORT` | OBProxy 通常 `2883`，observer 通常 `2881` |
| `CREST_DB_URL` | `jdbc:oceanbase://<host>:<port>` |
| `CREST_DB_USERNAME` | Crest 专用 OB Oracle 用户 |
| `CREST_DB_PASSWORD` | Crest 专用 OB Oracle 密码 |
| `ghcr.io/sevoniva/crest-service:v2.0.2` | 后端内网镜像地址 |
| `ghcr.io/sevoniva/crest-web:v2.0.2` | 前端内网镜像地址 |
| `redis:6.2.19` | Redis 6.2.19 内网镜像地址 |
| `50Gi` / `10Gi` | 后端和 Redis PVC 容量 |
| `storageClassName` | 需要指定存储类时取消注释并填写 |

已经生成好的值不需要再改：

```text
CREST_AES_KEY
CREST_AES_IV
CREST_INITIAL_PASSWORD
CREST_TOKEN_SECRET
CREST_REDIS_PASSWORD
```

Redis username 不需要填写。OceanBase Oracle 不填写 `DB_NAME`。

## 初始化数据库

由 DBA 创建 Crest 专用用户后，执行一次性空库 SQL：

```bash
obclient --default-character-set=utf8mb4 \
  -h <obproxy-host> -P 2883 \
  -u '<user>@<tenant>#<cluster>' \
  -p'<password>' \
  < installer/upgrade-sql/v2.0.2_full_schema_ob_oracle.sql
```

## 部署

```bash
node scripts/verify-kubernetes-production.mjs deploy/kubernetes
kubectl apply -f deploy/kubernetes/crest-ob-oracle-redis.yaml
kubectl rollout status deployment/crest-redis --timeout=300s
kubectl rollout status deployment/crest-service --timeout=300s
kubectl rollout status deployment/crest --timeout=300s
```

## 检查

```bash
kubectl get pods
kubectl logs deploy/crest-service --tail=200
```

后端日志不应持续出现 `ERROR`、`WARN`、`Exception`、数据库连接失败或 Redis 连接失败。

## 访问

清单创建 `crest` Service，端口为 `8100`。现场按集群规范接入 Ingress、网关或负载均衡。
