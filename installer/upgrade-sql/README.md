# Crest 升级 SQL

本目录保存同发布线升级使用的 SQL。

默认安装不需要执行本目录文件，应用镜像已经包含最新初始化脚本。

如果由 DBA 先手工初始化空系统库，并关闭服务端自动迁移，可执行对应数据库类型的一次性空库初始化 SQL：

- MySQL：`v2.1.0_full_schema_mysql.sql`
- OceanBase MySQL 模式：`v2.1.0_full_schema_ob_mysql.sql`
- OceanBase Oracle 模式：`v2.1.0_full_schema_ob_oracle.sql`

一次性空库初始化 SQL 只适用于新建系统库。已有系统库升级时不要执行这些完整建库 SQL，应执行对应源版本到目标版本的增量 SQL。

执行前请先确认：

1. 已备份 Crest 系统库；
2. 已暂停业务访问或安排维护窗口；
3. SQL 文件名中的源版本和目标版本与当前升级路径一致。

默认单机部署环境，在离线包解压目录执行：

```bash
set -a
source /opt/crest/.env
set +a
docker exec -i mysql-crest mysql -uroot -p"${CREST_DB_PASSWORD}" "${CREST_DB_NAME:-crest}" < upgrade-sql/v1.5.4_to_v1.5.5.sql
```

如果安装时修改过 `CREST_BASE`，请把 `/opt/crest/.env` 换成实际运行目录中的 `.env`。

外部 MySQL 环境可在数据库服务器或运维机执行 MySQL 脚本，目标库为 Crest 系统库。

OceanBase MySQL 租户执行文件名带 `ob_mysql` 的脚本。OceanBase Oracle 租户执行文件名带 `ob_oracle` 的脚本，并以 Crest 系统库 schema 用户执行。执行 OceanBase SQL 时，`obclient` 必须指定 `--default-character-set=utf8mb4`。

同一 Crest 系统库只能选择一种迁移方式。已由 Flyway 自动迁移的环境不要再手工执行同版本 SQL；生产模式手工执行 SQL 后，所有连接该系统库的服务都应关闭 Flyway。

`v2.0.0_export_task_queue_*`、`v2.0.0_datasource_sync_task_queue_*`、`v2.0.0_dataset_sync_task_queue_*`、`v2.0.0_dataset_sync_task_queue_metadata_*`、`v2.0.0_scheduled_task_queue_state_*` 用于补齐生产多副本任务队列字段和状态表。`v2.0.0_audit_log_filter_index_*` 用于优化审计日志筛选和按时间倒序查询。`v2.0.0_repair_seed_text_encoding_ob_oracle.sql` 用于修复早期 OB Oracle 初始化时可能写入的内置中文乱码。生产环境升级时按文件名顺序执行对应数据库类型脚本，再启动 `CREST_PRODUCTION_MODE=true` 的服务。

2.0.0 升级到 2.0.1 时，按数据库类型选择对应脚本，并按下面顺序执行：

- MySQL：`v2.0.0_to_v2.0.1_metadata_tables_mysql.sql`、`v2.0.0_to_v2.0.1_runtime_identifiers_mysql.sql`
- OceanBase MySQL 模式：`v2.0.0_to_v2.0.1_metadata_tables_ob_mysql.sql`、`v2.0.0_to_v2.0.1_runtime_identifiers_ob_mysql.sql`
- OceanBase Oracle 模式：`v2.0.0_to_v2.0.1_metadata_tables_ob_oracle.sql`、`v2.0.0_to_v2.0.1_runtime_identifiers_ob_oracle.sql`

2.0.1 元数据改名脚本处理 Crest 自有业务表；运行配置标识脚本处理已保存的图表、可视化、模板和外部参数配置。2.0.2 继续统一调度元数据表、模板分类关系表、导出下载任务表和调度实例列名，并清理早期初始化脚本写入的固定系统密钥。执行前必须备份系统库；如果同一张元数据表的新旧名称同时存在，脚本会中断，需先人工确认数据后再继续。

2.0.1 升级到 2.0.2 时，按数据库类型选择对应脚本，并按下面顺序执行：

- MySQL：`v2.0.1_to_v2.0.2_metadata_names_mysql.sql`、`v2.0.1_to_v2.0.2_rotate_default_crypto_key_mysql.sql`
- OceanBase MySQL 模式：`v2.0.1_to_v2.0.2_metadata_names_ob_mysql.sql`、`v2.0.1_to_v2.0.2_rotate_default_crypto_key_ob_mysql.sql`
- OceanBase Oracle 模式：`v2.0.1_to_v2.0.2_metadata_names_ob_oracle.sql`、`v2.0.1_to_v2.0.2_rotate_default_crypto_key_ob_oracle.sql`

2.0.2 升级到 2.0.3 时没有新增数据库升级 SQL，替换前后端应用镜像即可。

1.5.14 升级到 2.1.0 时，MySQL 环境在离线包根目录执行 `upgrade-sql/v1.5.14_to_v2.1.0_mysql.sql`。该入口脚本会先检查 v1.5.14 元数据表，再按顺序调用已有 2.0.x 增量脚本和 `v2.0.3_to_v2.1.0_mysql.sql`。不要对已有 v1.5.14 系统库执行 full schema 文件。

2.0.3 升级到 2.1.0 时，按数据库类型选择对应脚本：

- MySQL：`v2.0.3_to_v2.1.0_mysql.sql`
- OceanBase MySQL 模式：`v2.0.3_to_v2.1.0_ob_mysql.sql`
- OceanBase Oracle 模式：`v2.0.3_to_v2.1.0_ob_oracle.sql`
