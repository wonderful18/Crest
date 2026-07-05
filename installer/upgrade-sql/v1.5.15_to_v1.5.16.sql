-- Crest v1.5.15 -> v1.5.16 元数据库升级脚本。
-- 执行前请先备份 Crest 元数据库。

START TRANSACTION;

-- 补齐血缘关系菜单的权限记录，避免老环境升级后接口未纳入菜单权限控制。
INSERT IGNORE INTO `iam_role_menu_permission` (`id`, `rid`, `menu_id`, `permission`, `create_time`)
VALUES
  (6601, 1, 66, 'manage', 1779664240000),
  (6602, 2, 66, 'read', 1779664240000),
  (6603, 3, 66, 'read', 1779664240000);

COMMIT;
