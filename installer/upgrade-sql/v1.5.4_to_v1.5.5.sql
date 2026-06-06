-- Crest v1.5.4 -> v1.5.5 元数据库升级脚本。
-- 执行前请先备份 Crest 元数据库。

START TRANSACTION;

INSERT INTO `sys_setting` (`id`, `pkey`, `pval`, `type`, `sort`)
SELECT 1048232869488627721, 'basic.initialPassword', '', 'pwd', 15
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_setting` WHERE `pkey` = 'basic.initialPassword'
);

UPDATE `sys_setting`
SET `type` = 'pwd', `sort` = 15
WHERE `pkey` = 'basic.initialPassword';

INSERT INTO `sys_setting` (`id`, `pkey`, `pval`, `type`, `sort`)
SELECT 1048232869488627722, 'basic.pwdStrategy', 'true', 'text', 16
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_setting` WHERE `pkey` = 'basic.pwdStrategy'
);

UPDATE `sys_setting`
SET `type` = 'text', `sort` = 16
WHERE `pkey` = 'basic.pwdStrategy';

INSERT INTO `sys_setting` (`id`, `pkey`, `pval`, `type`, `sort`)
SELECT 1048232869488627723, 'basic.dip', 'false', 'text', 17
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_setting` WHERE `pkey` = 'basic.dip'
);

UPDATE `sys_setting`
SET `type` = 'text', `sort` = 17
WHERE `pkey` = 'basic.dip';

INSERT INTO `sys_setting` (`id`, `pkey`, `pval`, `type`, `sort`)
SELECT 1048232869488627724, 'basic.pvp', '0', 'text', 18
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_setting` WHERE `pkey` = 'basic.pvp'
);

UPDATE `sys_setting`
SET `type` = 'text', `sort` = 18
WHERE `pkey` = 'basic.pvp';

INSERT INTO `sys_setting` (`id`, `pkey`, `pval`, `type`, `sort`)
SELECT 1048232869488627725, 'basic.loginLimit', 'false', 'text', 19
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_setting` WHERE `pkey` = 'basic.loginLimit'
);

UPDATE `sys_setting`
SET `type` = 'text', `sort` = 19
WHERE `pkey` = 'basic.loginLimit';

INSERT INTO `sys_setting` (`id`, `pkey`, `pval`, `type`, `sort`)
SELECT 1048232869488627726, 'basic.loginLimitRate', '5', 'text', 20
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_setting` WHERE `pkey` = 'basic.loginLimitRate'
);

UPDATE `sys_setting`
SET `type` = 'text', `sort` = 20
WHERE `pkey` = 'basic.loginLimitRate';

INSERT INTO `sys_setting` (`id`, `pkey`, `pval`, `type`, `sort`)
SELECT 1048232869488627727, 'basic.loginLimitTime', '30', 'text', 21
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_setting` WHERE `pkey` = 'basic.loginLimitTime'
);

UPDATE `sys_setting`
SET `type` = 'text', `sort` = 21
WHERE `pkey` = 'basic.loginLimitTime';

COMMIT;
