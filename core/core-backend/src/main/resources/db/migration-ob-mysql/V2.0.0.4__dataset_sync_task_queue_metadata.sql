ALTER TABLE `dataset_sync_task`
  ADD COLUMN `worker_id` varchar(128) DEFAULT NULL COMMENT '队列执行者标识',
  ADD COLUMN `retry_count` int DEFAULT '0' COMMENT '队列执行重试次数',
  ADD COLUMN `lock_version` bigint DEFAULT '0' COMMENT '队列抢占版本',
  ADD COLUMN `next_fire_time` bigint DEFAULT NULL COMMENT '下次可投递时间',
  ADD COLUMN `last_error` text COMMENT '最近一次队列错误';

CREATE INDEX `idx_dataset_sync_task_hb` ON `dataset_sync_task` (`task_status`, `heartbeat_time`);
