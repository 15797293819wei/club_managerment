CREATE TABLE IF NOT EXISTS `club_application_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `application_id` BIGINT NOT NULL COMMENT '申请ID',
    `reviewer_id` BIGINT NOT NULL COMMENT '审核人ID',
    `status` TINYINT NOT NULL COMMENT '审核状态：1-通过，2-驳回',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    INDEX `idx_history_application_id` (`application_id`),
    INDEX `idx_history_reviewer_id` (`reviewer_id`),
    CONSTRAINT `fk_history_application` FOREIGN KEY (`application_id`) REFERENCES `club_applications`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社团申请审核历史';

