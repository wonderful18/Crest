ALTER TABLE `core_rsa`
    ADD COLUMN `sm2_private_key` TEXT NULL COMMENT 'SM2 private key',
    ADD COLUMN `sm2_public_key` TEXT NULL COMMENT 'SM2 public key',
    ADD COLUMN `sm2_create_time` BIGINT NULL COMMENT 'SM2 key creation time';
