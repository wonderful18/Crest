-- Cleanup script for Crest built-in demo resources.
-- It only removes fixed demo resource IDs and the crest_demo_retail database.

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `core_share_ticket`
WHERE `uuid` IN (
  SELECT `uuid` FROM `core_share_link`
  WHERE `resource_id` IN (970001, 980001, 980002, 980003, 980004, 980005)
);

DELETE FROM `core_share_link`
WHERE `resource_id` IN (970001, 980001, 980002, 980003, 980004, 980005);

DELETE FROM `core_workspace_recent_resource`
WHERE `resource_id` = 910001
   OR `resource_id` BETWEEN 920000 AND 920999
   OR `resource_id` BETWEEN 930000 AND 930999
   OR `resource_id` BETWEEN 970000 AND 970999
   OR `resource_id` BETWEEN 980000 AND 980999
   OR `resource_id` BETWEEN 982000 AND 982999
   OR `resource_id` BETWEEN 983000 AND 983999;

DELETE FROM `core_workspace_favorite_resource`
WHERE `resource_id` = 910001
   OR `resource_id` BETWEEN 970000 AND 970999
   OR `resource_id` BETWEEN 980000 AND 980999;

DELETE FROM `core_iam_resource_permission`
WHERE `resource_id` REGEXP '^[0-9]+$'
  AND (
    CAST(`resource_id` AS UNSIGNED) = 910001
    OR CAST(`resource_id` AS UNSIGNED) BETWEEN 920000 AND 920999
    OR CAST(`resource_id` AS UNSIGNED) BETWEEN 970000 AND 970999
    OR CAST(`resource_id` AS UNSIGNED) BETWEEN 980000 AND 980999
    OR CAST(`resource_id` AS UNSIGNED) BETWEEN 982000 AND 982999
  );

DELETE FROM `core_iam_resource_index`
WHERE `resource_id` REGEXP '^[0-9]+$'
  AND (
    CAST(`resource_id` AS UNSIGNED) = 910001
    OR CAST(`resource_id` AS UNSIGNED) BETWEEN 920000 AND 920999
    OR CAST(`resource_id` AS UNSIGNED) BETWEEN 970000 AND 970999
    OR CAST(`resource_id` AS UNSIGNED) BETWEEN 980000 AND 980999
    OR CAST(`resource_id` AS UNSIGNED) BETWEEN 982000 AND 982999
  );

DELETE FROM `core_chart_view_snapshot`
WHERE `scene_id` = 970001
   OR `scene_id` BETWEEN 980001 AND 980005
   OR `id` BETWEEN 970000 AND 970999
   OR `id` BETWEEN 980000 AND 980999;

DELETE FROM `core_chart_view`
WHERE `scene_id` = 970001
   OR `scene_id` BETWEEN 980001 AND 980005
   OR `id` BETWEEN 970000 AND 970999
   OR `id` BETWEEN 980000 AND 980999;

DELETE FROM `core_visualization_snapshot`
WHERE CAST(`id` AS UNSIGNED) = 970001
   OR CAST(`id` AS UNSIGNED) BETWEEN 980001 AND 980005;

DELETE FROM `core_visualization`
WHERE `id` = 970001
   OR `id` BETWEEN 980001 AND 980005;

DELETE FROM `core_dataset_field`
WHERE `datasource_id` = 910001
   OR `dataset_table_id` BETWEEN 930000 AND 930999
   OR `dataset_table_id` BETWEEN 983000 AND 983999
   OR `dataset_group_id` BETWEEN 920000 AND 920999
   OR `dataset_group_id` BETWEEN 982000 AND 982999
   OR `id` BETWEEN 940000000 AND 940999999
   OR `id` BETWEEN 984000000 AND 984999999;

DELETE FROM `core_dataset_table`
WHERE `datasource_id` = 910001
   OR `id` BETWEEN 930000 AND 930999
   OR `id` BETWEEN 983000 AND 983999;

DELETE FROM `core_dataset`
WHERE `id` BETWEEN 920000 AND 920999
   OR `id` BETWEEN 982000 AND 982999;

DELETE FROM `core_datasource`
WHERE `id` = 910001;

DROP DATABASE IF EXISTS `crest_demo_retail`;

SET FOREIGN_KEY_CHECKS = 1;
