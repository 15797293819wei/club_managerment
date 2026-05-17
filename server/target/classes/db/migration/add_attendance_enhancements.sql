ALTER TABLE `attendances`
    ADD COLUMN `exception_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '是否异常',
    ADD COLUMN `exception_status` TINYINT NOT NULL DEFAULT 0 COMMENT '异常处理状态：0-待处理，1-已处理',
    ADD COLUMN `exception_reason` VARCHAR(255) NULL COMMENT '异常原因或备注',
    ADD COLUMN `handled_by` BIGINT NULL COMMENT '处理人ID',
    ADD COLUMN `handled_time` DATETIME NULL COMMENT '处理时间';

CREATE TABLE IF NOT EXISTS `attendance_rules` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id` BIGINT NOT NULL UNIQUE COMMENT '社团ID',
    `late_threshold` INT NOT NULL DEFAULT 10 COMMENT '迟到阈值（分钟）',
    `leave_early_threshold` INT NOT NULL DEFAULT 10 COMMENT '早退阈值（分钟）',
    `absence_threshold` INT NOT NULL DEFAULT 30 COMMENT '缺勤阈值（分钟）',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_attendance_rules_club` FOREIGN KEY (`club_id`) REFERENCES `clubs`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社团考勤规则配置表';

