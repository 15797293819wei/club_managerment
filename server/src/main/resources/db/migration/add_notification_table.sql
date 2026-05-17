CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '接收人ID',
    `type` VARCHAR(50) NOT NULL COMMENT '消息类型',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` VARCHAR(1000) NOT NULL COMMENT '内容',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `read_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    INDEX `idx_notifications_user_id` (`user_id`),
    INDEX `idx_notifications_read_flag` (`read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内消息通知表';


