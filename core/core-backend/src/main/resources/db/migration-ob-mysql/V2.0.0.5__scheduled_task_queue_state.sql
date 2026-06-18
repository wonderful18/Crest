CREATE TABLE `scheduled_task_queue_state` (
  `state_id` varchar(64) NOT NULL COMMENT '队列状态主键',
  `task_type` varchar(50) NOT NULL COMMENT '调度任务类型',
  `task_id` varchar(128) DEFAULT NULL COMMENT '业务任务编号',
  `payload_hash` varchar(64) NOT NULL COMMENT '任务参数摘要',
  `status` varchar(32) NOT NULL COMMENT '队列执行状态',
  `worker_id` varchar(128) DEFAULT NULL COMMENT '执行 Worker 编号',
  `enqueue_time` bigint NOT NULL COMMENT '投递时间',
  `start_time` bigint DEFAULT NULL COMMENT '开始执行时间',
  `end_time` bigint DEFAULT NULL COMMENT '结束执行时间',
  `heartbeat_time` bigint DEFAULT NULL COMMENT 'Worker 心跳时间',
  `retry_count` int DEFAULT '0' COMMENT '执行重试次数',
  `lock_version` bigint DEFAULT '0' COMMENT '抢占版本号',
  `last_error` longtext COMMENT '最近执行错误',
  PRIMARY KEY (`state_id`),
  KEY `idx_scheduled_task_queue_status` (`status`, `heartbeat_time`),
  KEY `idx_scheduled_task_queue_task` (`task_type`, `task_id`)
) COMMENT='报表和填报调度队列状态';
