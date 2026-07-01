-- Crest v1.5.13 -> v1.5.14 元数据库升级脚本。
-- 执行前请先备份 Crest 元数据库。

START TRANSACTION;

UPDATE `sys_setting`
SET `pval` = ''
WHERE `pkey` = 'template.accessKey'
  AND `pval` = 'crest';

COMMIT;
