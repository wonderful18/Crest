-- Crest v2.0.0 export task queue upgrade for MySQL and OceanBase MySQL.
-- Execute after backing up the Crest metadata database.

ALTER TABLE `export_task`
  ADD COLUMN `worker_id` varchar(128) DEFAULT NULL COMMENT '执行 Worker 编号',
  ADD COLUMN `heartbeat_time` bigint DEFAULT NULL COMMENT 'Worker 心跳时间',
  ADD COLUMN `retry_count` int DEFAULT '0' COMMENT '重试次数',
  ADD COLUMN `lock_version` bigint DEFAULT '0' COMMENT '任务抢占版本号',
  ADD COLUMN `next_fire_time` bigint DEFAULT NULL COMMENT '下次调度时间',
  ADD COLUMN `last_enqueue_time` bigint DEFAULT NULL COMMENT '最近投递时间',
  ADD COLUMN `last_error` longtext COMMENT '最近执行错误';
