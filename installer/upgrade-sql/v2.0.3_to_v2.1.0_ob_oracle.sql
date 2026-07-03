-- Crest v2.0.3 -> v2.1.0
-- 清理 v2.0.3 初始化数据中的公开模板访问密钥默认值。
-- 仅处理仍保持历史默认值的环境；用户已经自定义的 template.accessKey 不会被覆盖。

UPDATE core_system_setting
SET pval = ''
WHERE pkey = 'template.accessKey'
  AND DBMS_LOB.SUBSTR(pval, 4000, 1) = 'crest';

SELECT pkey, DBMS_LOB.SUBSTR(pval, 4000, 1) AS pval
FROM core_system_setting
WHERE pkey = 'template.accessKey';
