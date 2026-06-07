# Crest 升级 SQL

本目录保存同发布线升级使用的 SQL。

只有升级已有环境时才需要执行对应 SQL。全新安装不需要执行本目录文件，应用镜像已经包含最新初始化脚本。

执行前请先确认：

1. 已备份 Crest 元数据库；
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

外部 MySQL 环境可在数据库服务器或运维机执行同一 SQL 文件，目标库为 Crest 元数据库。
