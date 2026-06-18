# Crest 升级 SQL

本目录保存同发布线升级使用的 SQL。

只有升级已有环境时才需要执行对应 SQL。全新安装不需要执行本目录文件，应用镜像已经包含最新初始化脚本。

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
