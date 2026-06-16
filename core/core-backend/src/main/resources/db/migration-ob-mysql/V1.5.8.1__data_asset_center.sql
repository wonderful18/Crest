-- Crest v1.5.8 data asset center metadata.

CREATE TABLE IF NOT EXISTS `data_asset_profile` (
  `id` bigint NOT NULL COMMENT 'ID',
  `asset_type` varchar(32) NOT NULL COMMENT '资产类型 datasource/table/dataset/chart/panel/screen/share',
  `asset_id` varchar(128) NOT NULL COMMENT '资产ID',
  `description` varchar(1024) DEFAULT NULL COMMENT '资产说明',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `certified` tinyint NOT NULL DEFAULT 0 COMMENT '是否已认证',
  `recommended` tinyint NOT NULL DEFAULT 0 COMMENT '是否推荐使用',
  `deprecated` tinyint NOT NULL DEFAULT 0 COMMENT '是否已废弃',
  `tags` varchar(1024) DEFAULT NULL COMMENT '标签，逗号分隔',
  `create_time` bigint DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_data_asset_profile_asset` (`asset_type`, `asset_id`),
  KEY `idx_data_asset_profile_status` (`asset_type`, `certified`, `recommended`, `deprecated`),
  KEY `idx_data_asset_profile_owner` (`owner_id`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据资产治理扩展信息';

UPDATE `data_asset_profile` SET `certified` = 0, `recommended` = 0 WHERE `deprecated` = 1;

INSERT IGNORE INTO `iam_menu` (`id`, `pid`, `type`, `name`, `component`, `menu_sort`, `icon`, `path`, `hidden`, `in_layout`, `auth`)
VALUES
  (77, 4, 2, 'data-asset', 'visualized/data/asset', 3, 'association', '/asset', 0, 1, 1),
  (78, 4, 2, 'cache-task', 'visualized/data/cache-task', 4, 'task', '/cache-task', 0, 1, 1);

INSERT IGNORE INTO `iam_role_menu_permission` (`id`, `rid`, `menu_id`, `permission`, `create_time`)
VALUES
  (7701, 1, 77, 'manage', 1779664240000),
  (7702, 2, 77, 'read', 1779664240000),
  (7703, 3, 77, 'read', 1779664240000),
  (7801, 1, 78, 'manage', 1779664240000),
  (7802, 2, 78, 'read', 1779664240000),
  (7803, 3, 78, 'read', 1779664240000);
