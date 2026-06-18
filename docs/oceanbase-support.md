# OceanBase 支持

Crest 支持将 OceanBase 作为业务数据源，也支持将 OceanBase 作为 Crest 服务自身的系统库。MySQL 仍是默认系统库，默认安装和升级行为保持不变。

## 支持范围

| 类型 | 用途 | 状态 |
| --- | --- | --- |
| `mysql` | 默认系统库和业务数据源 | 默认启用 |
| `ob-mysql` | OceanBase MySQL 租户，作为系统库或业务数据源 | MySQL 兼容路径，独立配置入口 |
| `ob-oracle` | OceanBase Oracle 租户，作为系统库或业务数据源 | 独立方言和初始化脚本 |

业务数据源连接支持 observer 直连，也支持 OBProxy / ODP 代理连接。

## 系统库部署原则

MySQL 默认由应用通过 Flyway 初始化。OceanBase 属于企业外部数据库，生产环境按以下顺序部署：

1. DBA 创建租户、数据库或 schema、账号和权限；
2. DBA 按文件名顺序执行离线包中的初始化 SQL；
3. 运维配置 Crest 系统库连接信息；
4. 启动 Crest，保持 `CREST_FLYWAY_ENABLED=false`。

执行 OceanBase 初始化和升级 SQL 时，`obclient` 必须指定 `--default-character-set=utf8mb4`，避免中文初始化数据在客户端导入阶段乱码。

离线包 SQL 目录：

| 目录 | 用途 |
| --- | --- |
| `init-sql/mysql` | MySQL 空库初始化 SQL |
| `init-sql/ob-mysql` | OceanBase MySQL 租户初始化 SQL |
| `init-sql/ob-oracle` | OceanBase Oracle 租户初始化 SQL |

开发或受控验证环境允许显式设置 `CREST_FLYWAY_ENABLED=true`，由应用初始化空库。生产环境不建议这样做。

## 配置项

| 变量 | 说明 |
| --- | --- |
| `CREST_DB_TYPE` | `mysql`、`ob-mysql` 或 `ob-oracle` |
| `CREST_DB_DRIVER_CLASS_NAME` | OceanBase 使用 `com.oceanbase.jdbc.Driver` |
| `CREST_DB_HOST` | OBProxy / ODP 或 observer 地址 |
| `CREST_DB_PORT` | OBProxy / ODP 常用 `2883`，observer 常用 `2881` |
| `CREST_DB_NAME` | MySQL 兼容系统库名；Oracle 租户通常不配置 |
| `CREST_DB_URL` | 完整 JDBC URL，配置后优先于 host、port 和 db name |
| `CREST_DB_USERNAME` | OBProxy 用户名使用 `用户@租户#集群`，observer 直连使用 `用户@租户` |
| `CREST_DB_PASSWORD` | Crest 系统库账号密码 |
| `CREST_FLYWAY_ENABLED` | 留空自动判断：MySQL 为 `true`，OceanBase 为 `false` |
| `CREST_FLYWAY_LOCATIONS` | 一般不手工配置；需要覆盖时使用对应数据库目录 |

## 配置示例

OceanBase MySQL 通过 OBProxy 连接：

```bash
CREST_DB_TYPE=ob-mysql
CREST_DB_HOST=10.0.0.10
CREST_DB_PORT=2883
CREST_DB_NAME=crest
CREST_DB_USERNAME=<user>@<tenant>#<cluster>
CREST_DB_PASSWORD=<password>
```

OceanBase Oracle 通过 OBProxy 连接：

```bash
CREST_DB_TYPE=ob-oracle
CREST_DB_HOST=10.0.0.10
CREST_DB_PORT=2883
CREST_DB_USERNAME=<user>@<tenant>#<cluster>
CREST_DB_PASSWORD=<password>
```

observer 直连时端口通常为 `2881`，用户名不要带 `#集群`。

## OB 集群要求

生产环境优先通过 OBProxy / ODP 连接 OceanBase 集群。observer 直连只用于受控验证或 DBA 指定场景，避免应用绕过集群入口。

上线前至少检查以下项：

1. 使用与生产一致的租户模式、字符集、账号格式和连接入口验证启动链路；
2. 对审计日志、用户角色权限、资源索引、任务队列表执行典型查询的 `EXPLAIN`；
3. 批量导入或升级 SQL 执行后由 DBA 刷新统计信息；
4. 大表按业务增长量压测分页、筛选、排序和缓存同步；
5. OceanBase 生产环境保持 `CREST_FLYWAY_ENABLED=false`，由 DBA 执行初始化和升级 SQL。

## 业务数据源

业务数据源应显式选择 OceanBase 类型，不要把 OceanBase 连接伪装成普通 MySQL：

- OceanBase MySQL：选择 `obMysql`；
- OceanBase Oracle：选择 `obOracle`。

连接校验需要覆盖：

- OBProxy / ODP 代理连接；
- observer 直连；
- schema、表、字段元数据读取；
- SQL 预览和分页查询；
- 数据集预览、图表查询和缓存同步。

生产环境建议使用只读账号，并按业务库最小权限授权。

## 验收清单

- MySQL 默认安装和升级行为不变；
- `ob-mysql` 系统库能连接已初始化库并完成登录；
- `ob-oracle` 系统库能连接已初始化 schema 并完成登录；
- OceanBase 业务数据源通过 OBProxy / ODP 和 observer 直连均可用；
- 元数据读取、SQL 预览、分页查询、数据集预览和图表查询可用；
- 离线包包含 `init-sql` 和 `upgrade-sql`；
- 相关配置已写入安装、Kubernetes 和运维文档。
