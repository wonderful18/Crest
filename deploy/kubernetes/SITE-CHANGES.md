# 现场修改项

只改 `crest-ob-oracle-redis.yaml`。

## 必须改

```yaml
CREST_ORIGIN_LIST: "https://crest.example.com"
CREST_DB_HOST: "obproxy.example.com"
CREST_DB_PORT: "2883"
CREST_DB_URL: "jdbc:oceanbase://obproxy.example.com:2883"
CREST_DB_USERNAME: "CHANGE_ME_OB_USER"
CREST_DB_PASSWORD: "CHANGE_ME_OB_PASSWORD"
```

## 按现场改

```yaml
image: ghcr.io/sevoniva/crest-service:v2.0.2
image: ghcr.io/sevoniva/crest-web:v2.0.2
image: redis:6.2.19
storage: 50Gi
storage: 10Gi
# storageClassName: <storage-class>
```

## 不需要改

```yaml
CREST_REDIS_HOST: "crest-redis"
CREST_REDIS_PORT: "6379"
CREST_REDIS_DATABASE: "0"
CREST_REDIS_PASSWORD: "已生成"
```

Redis username 不需要填写。OceanBase Oracle 不填写 `DB_NAME`。
