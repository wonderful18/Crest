-- Crest v1.5.25 -> v1.5.26 元数据库升级脚本。
-- 执行前请先备份 Crest 元数据库。

START TRANSACTION;

-- 数据集缓存按天更新支持自定义执行时点(0-23点)，NULL=0点(向后兼容)。
-- 与 Flyway 迁移 V1.5.9.1__dataset_sync_task_simple_cron_hour.sql 等价，供手动升级使用。
ALTER TABLE `dataset_sync_task`
  ADD COLUMN `simple_cron_hour` int DEFAULT NULL COMMENT '简单重复按天执行时点(0-23)，NULL=0点(向后兼容)';

COMMIT;
