-- Crest v2.0.0 dataset sync task queue upgrade for MySQL and OceanBase MySQL.
ALTER TABLE `dataset_sync_task`
  ADD COLUMN `last_enqueue_time` bigint DEFAULT NULL COMMENT '最近投递时间';

CREATE INDEX `idx_dataset_sync_task_enqueue` ON `dataset_sync_task` (`task_status`, `last_enqueue_time`);
