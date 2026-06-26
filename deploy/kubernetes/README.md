# Crest Kubernetes 平台部署

本目录是标准 Kubernetes 平台交付清单。每个 YAML 文件只包含一个资源，按文件名前缀从 `00` 到 `12` 依次导入。

适用场景：OceanBase Oracle 系统库、Redis 6.2.19、一个后端 Pod、一个前端 Pod、后端持久卷、Redis 持久卷。只有前端 `crest` Service 使用 `NodePort`，后端和 Redis 只在集群内访问。

## 文件顺序

| 文件 | 资源 |
| --- | --- |
| `00-crest-env-configmap.yaml` | 运行参数 |
| `01-crest-db-secret.yaml` | 数据库、加密和初始密码 |
| `02-crest-redis-secret.yaml` | Redis 密码 |
| `03-crest-data-pvc.yaml` | 后端数据卷 |
| `04-crest-redis-data-pvc.yaml` | Redis 数据卷 |
| `05-crest-redis-configmap.yaml` | Redis 配置 |
| `06-crest-redis-deployment.yaml` | Redis 实例 |
| `07-crest-redis-service.yaml` | Redis 集群内访问 |
| `08-crest-service-deployment.yaml` | 后端服务 |
| `09-crest-service-service.yaml` | 后端集群内访问 |
| `10-crest-nginx-configmap.yaml` | 前端 Nginx 配置 |
| `11-crest-web-deployment.yaml` | 前端服务 |
| `12-crest-web-service.yaml` | 前端 NodePort 访问 |

## 必须修改

`00-crest-env-configmap.yaml`

```yaml
CREST_ORIGIN_LIST: "http://CHANGE_ME_NODE_IP:CHANGE_ME_NODEPORT"
CREST_DB_HOST: "CHANGE_ME_OB_HOST"
CREST_DB_PORT: "2883"
CREST_DB_URL: "jdbc:oceanbase://CHANGE_ME_OB_HOST:2883"
```

`01-crest-db-secret.yaml`

```yaml
CREST_DB_USERNAME: "CHANGE_ME_OB_USER@ob_oracle#obcluster"
CREST_DB_PASSWORD: "CHANGE_ME_OB_PASSWORD"
CREST_AES_KEY: "CHANGE_ME_AES_KEY_32_CHARS_00000"
CREST_AES_IV: "CHANGE_ME_IV_16_"
CREST_INITIAL_PASSWORD: "CHANGE_ME_INITIAL_ADMIN_PASSWORD"
CREST_TOKEN_SECRET: "CHANGE_ME_LONG_RANDOM_TOKEN_SECRET"
```

如果通过 observer 直连，把端口改为 `2881`，用户名使用 `用户@租户`。

`02-crest-redis-secret.yaml`

```yaml
CREST_REDIS_PASSWORD: "CHANGE_ME_REDIS_PASSWORD"
```

`CREST_AES_KEY` 必须为 32 位，`CREST_AES_IV` 必须为 16 位。升级已有环境时沿用原值，不要重新生成。

## 保持默认

```text
SPRING_QUARTZ_JDBC_INITIALIZE_SCHEMA
```

Redis username 不需要填写。OceanBase Oracle 不填写 `DB_NAME`。

## 按平台修改

如平台要求指定存储类，取消两个 PVC 文件中的 `storageClassName` 注释并填写实际值。前端 Service 不固定 `nodePort`，由平台自动分配。

## 初始化数据库

由 DBA 创建 Crest 专用用户后，执行一次性空库 SQL：

```bash
obclient --default-character-set=utf8mb4 \
  -h <obproxy-host> -P 2883 \
  -u '<user>@<tenant>#<cluster>' \
  -p'<password>' \
  < installer/upgrade-sql/v2.0.3_full_schema_ob_oracle.sql
```

## 校验

```bash
node scripts/verify-kubernetes-production.mjs deploy/kubernetes
kubectl create --dry-run=client -f deploy/kubernetes -o name
```

部署后确认后端 readiness 返回数据库和 Redis 都为 `UP`，后端日志不应持续出现 `ERROR`、`WARN`、`Exception`、数据库连接失败或 Redis 连接失败。
