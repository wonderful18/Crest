ALTER TABLE `dataset_sync_task`
  ADD COLUMN `simple_cron_hour` int DEFAULT NULL COMMENT '简单重复按天执行时点(0-23)，NULL=0点(向后兼容)';
