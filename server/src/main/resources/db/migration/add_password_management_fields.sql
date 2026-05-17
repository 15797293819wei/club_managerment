-- 为users表添加密码管理相关字段
ALTER TABLE `users` 
ADD COLUMN `password_changed_time` DATETIME DEFAULT NULL COMMENT '密码修改时间' AFTER `password`,
ADD COLUMN `login_failure_count` INT NOT NULL DEFAULT 0 COMMENT '登录失败次数' AFTER `last_login_time`,
ADD COLUMN `account_locked_until` DATETIME DEFAULT NULL COMMENT '账户锁定截止时间' AFTER `login_failure_count`,
ADD COLUMN `password_reset_token` VARCHAR(255) DEFAULT NULL COMMENT '密码重置令牌' AFTER `account_locked_until`,
ADD COLUMN `password_reset_token_expiry` DATETIME DEFAULT NULL COMMENT '密码重置令牌过期时间' AFTER `password_reset_token`,
ADD INDEX `idx_users_password_reset_token` (`password_reset_token`);

-- 为现有用户设置密码修改时间为创建时间（如果密码修改时间为空）
UPDATE `users` SET `password_changed_time` = `created_time` WHERE `password_changed_time` IS NULL;

