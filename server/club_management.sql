/*
 Navicat Premium Data Transfer

 Source Server         : mycourse
 Source Server Type    : MySQL
 Source Server Version : 80037
 Source Host           : localhost:3306
 Source Schema         : club_management

 Target Server Type    : MySQL
 Target Server Version : 80037
 File Encoding         : 65001

 Date: 15/04/2026 12:22:07
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for activities
-- ----------------------------
DROP TABLE IF EXISTS `activities`;
CREATE TABLE `activities`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `club_id` bigint(0) NOT NULL COMMENT '社团ID',
  `activity_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '活动名称',
  `activity_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '活动类型',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '活动描述',
  `start_time` datetime(0) NOT NULL COMMENT '开始时间',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '结束时间',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '活动地点',
  `max_participants` int(0) NULL DEFAULT NULL COMMENT '最大参与人数',
  `current_participants` int(0) NOT NULL DEFAULT 0 COMMENT '当前参与人数',
  `registration_deadline` datetime(0) NULL DEFAULT NULL COMMENT '报名截止时间',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态（0-待开始，1-进行中，2-已结束，3-已取消）',
  `creator_id` bigint(0) NOT NULL COMMENT '创建人ID',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_activities_club_id`(`club_id`) USING BTREE,
  INDEX `idx_activities_creator_id`(`creator_id`) USING BTREE,
  INDEX `idx_activities_status`(`status`) USING BTREE,
  INDEX `idx_activities_start_time`(`start_time`) USING BTREE,
  INDEX `idx_activities_created_time`(`created_time`) USING BTREE,
  CONSTRAINT `fk_activities_club_id` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_activities_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '活动表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of activities
-- ----------------------------
INSERT INTO `activities` VALUES (1, 1, '第一次社团会议', '会议', '进行社团的第一次会议', '2025-11-19 13:07:00', '2025-11-19 15:00:00', '教一210', NULL, 1, NULL, 2, 2, '2025-11-16 13:38:19', '2025-11-20 15:06:06');
INSERT INTO `activities` VALUES (2, 2, '见面会', '会议', '各成员进行一次见面会，熟悉团队', '2025-11-19 14:30:00', '2025-11-19 15:30:00', '教一110', NULL, 0, NULL, 2, 4, '2025-11-18 20:15:26', '2025-11-20 15:06:06');
INSERT INTO `activities` VALUES (4, 1, '测试', NULL, NULL, '2025-11-19 00:14:44', '2025-11-19 00:40:54', NULL, NULL, 1, NULL, 2, 3, '2025-11-19 00:13:06', '2025-11-19 01:08:31');
INSERT INTO `activities` VALUES (5, 1, '测试1', '1', NULL, '2025-11-19 01:21:15', '2025-11-20 00:00:00', NULL, NULL, 1, NULL, 2, 2, '2025-11-19 01:20:40', '2025-11-20 15:06:06');
INSERT INTO `activities` VALUES (6, 1, '测试2', '测试', '测试案例1', '2026-01-29 15:36:00', '2026-01-31 00:00:00', '教二101', NULL, 0, NULL, 3, 2, '2026-01-29 15:35:56', '2026-01-29 15:43:16');
INSERT INTO `activities` VALUES (7, 1, '测试3', NULL, NULL, '2026-01-30 00:00:00', '2026-01-31 00:00:00', NULL, NULL, 0, NULL, 2, 2, '2026-01-29 15:40:19', '2026-02-04 15:17:39');
INSERT INTO `activities` VALUES (8, 1, '测试12', NULL, NULL, '2026-03-18 00:00:00', '2026-03-19 00:00:00', NULL, NULL, 0, NULL, 1, 3, '2026-03-17 17:11:19', '2026-03-18 13:33:23');

-- ----------------------------
-- Table structure for announcements
-- ----------------------------
DROP TABLE IF EXISTS `announcements`;
CREATE TABLE `announcements`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `scope_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `club_id` bigint(0) NULL DEFAULT NULL,
  `club_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `publisher_id` bigint(0) NOT NULL,
  `publisher_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `publisher_role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_announcements_scope`(`scope_type`, `club_id`) USING BTREE,
  INDEX `idx_announcements_publisher`(`publisher_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of announcements
-- ----------------------------
INSERT INTO `announcements` VALUES (1, '系统启用说明', '本系统主要服务于校园社团，各社团提交审查表和证明材料，创建社团；各位同学可以向自己感兴趣的社团提交入社申请，请各位同学使用该系统愉快，谢谢大家！', 'GLOBAL', NULL, NULL, 1, '系统管理员', 'SYSTEM_ADMIN', '2025-11-18 19:01:57', '2025-11-18 19:01:57');

-- ----------------------------
-- Table structure for attendance_rules
-- ----------------------------
DROP TABLE IF EXISTS `attendance_rules`;
CREATE TABLE `attendance_rules`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `club_id` bigint(0) NOT NULL,
  `late_threshold` int(0) NOT NULL DEFAULT 10,
  `leave_early_threshold` int(0) NOT NULL DEFAULT 10,
  `absence_threshold` int(0) NOT NULL DEFAULT 30,
  `created_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_club_id`(`club_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attendance_rules
-- ----------------------------
INSERT INTO `attendance_rules` VALUES (1, 1, 10, 10, 30, '2025-11-18 23:38:32', '2025-11-18 23:38:32');

-- ----------------------------
-- Table structure for attendances
-- ----------------------------
DROP TABLE IF EXISTS `attendances`;
CREATE TABLE `attendances`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  `club_id` bigint(0) NOT NULL COMMENT '社团ID',
  `activity_id` bigint(0) NULL DEFAULT NULL COMMENT '活动ID（可为空，表示日常考勤）',
  `member_id` bigint(0) NOT NULL COMMENT '成员ID',
  `attendance_type` tinyint(0) NOT NULL DEFAULT 1 COMMENT '考勤类型（1-正常，2-迟到，3-早退，4-缺勤）',
  `attendance_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '考勤时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `exception_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否异常',
  `exception_status` tinyint(1) NULL DEFAULT 0 COMMENT '异常处理状态 0-待处理 1-已处理',
  `exception_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `exception_handler_id` bigint(0) NULL DEFAULT NULL,
  `exception_handle_time` datetime(0) NULL DEFAULT NULL,
  `handled_by` bigint(0) NULL DEFAULT NULL COMMENT '异常处理人',
  `handled_time` datetime(0) NULL DEFAULT NULL COMMENT '异常处理时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_attendances_club_id`(`club_id`) USING BTREE,
  INDEX `idx_attendances_activity_id`(`activity_id`) USING BTREE,
  INDEX `idx_attendances_member_id`(`member_id`) USING BTREE,
  INDEX `idx_attendances_attendance_time`(`attendance_time`) USING BTREE,
  CONSTRAINT `fk_attendances_activity_id` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_attendances_club_id` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_attendances_member_id` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考勤表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attendances
-- ----------------------------
INSERT INTO `attendances` VALUES (1, 1, 4, 2, 1, '2025-11-19 00:16:58', '1', '2025-11-19 00:16:57', 0, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `attendances` VALUES (2, 1, 1, 3, 1, '2025-11-19 00:36:23', '1', '2025-11-19 00:36:25', 1, 1, '没有问题', NULL, NULL, 2, '2026-01-28 21:27:27');

-- ----------------------------
-- Table structure for club_application_history
-- ----------------------------
DROP TABLE IF EXISTS `club_application_history`;
CREATE TABLE `club_application_history`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint(0) NOT NULL COMMENT '申请ID',
  `reviewer_id` bigint(0) NOT NULL COMMENT '审核人ID',
  `status` tinyint(0) NOT NULL COMMENT '审核状态：1-通过，2-驳回',
  `review_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审核意见',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '记录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_history_application_id`(`application_id`) USING BTREE,
  INDEX `idx_history_reviewer_id`(`reviewer_id`) USING BTREE,
  CONSTRAINT `fk_history_application` FOREIGN KEY (`application_id`) REFERENCES `club_applications` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社团申请审核历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of club_application_history
-- ----------------------------
INSERT INTO `club_application_history` VALUES (1, 2, 1, 1, NULL, '2025-11-17 22:46:18');
INSERT INTO `club_application_history` VALUES (2, 3, 1, 1, '申请通过', '2026-01-29 10:55:50');
INSERT INTO `club_application_history` VALUES (3, 4, 1, 1, NULL, '2026-01-30 16:51:27');
INSERT INTO `club_application_history` VALUES (4, 5, 1, 1, NULL, '2026-01-30 17:23:26');

-- ----------------------------
-- Table structure for club_applications
-- ----------------------------
DROP TABLE IF EXISTS `club_applications`;
CREATE TABLE `club_applications`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `applicant_id` bigint(0) NOT NULL COMMENT '申请人ID',
  `club_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '社团名称',
  `club_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '社团代码',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '社团简介',
  `purpose` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '社团宗旨',
  `constitution` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '社团章程',
  `logo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Logo URL',
  `attachment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '证明材料URL',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态（0-待审核，1-已通过，2-已驳回）',
  `reviewer_id` bigint(0) NULL DEFAULT NULL COMMENT '审核人ID',
  `review_time` datetime(0) NULL DEFAULT NULL COMMENT '审核时间',
  `review_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审核意见',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_club_applications_applicant_id`(`applicant_id`) USING BTREE,
  INDEX `idx_club_applications_status`(`status`) USING BTREE,
  INDEX `idx_club_applications_reviewer_id`(`reviewer_id`) USING BTREE,
  INDEX `idx_club_applications_created_time`(`created_time`) USING BTREE,
  CONSTRAINT `fk_club_applications_applicant_id` FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_club_applications_reviewer_id` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社团申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of club_applications
-- ----------------------------
INSERT INTO `club_applications` VALUES (1, 2, '计算机协会', '202201', '来到计算机协会，你将接受到不同的计算机技术，遇到问题，可以向学长学姐请教，在这个大家庭，敬请享受协会的温暖！', '码力全开，厚积薄发。', NULL, NULL, NULL, 1, 1, '2025-11-13 22:15:57', NULL, '2025-11-13 22:09:39', '2025-11-13 22:15:56');
INSERT INTO `club_applications` VALUES (2, 4, '墨渊书画协会', '202202', '墨渊书画协会成立于2015年，是由书画爱好者自发组成的非营利性文化团体，以“墨润心渊，书画传情”为宗旨，致力于传承中国传统书画艺术，推广现代创作理念。协会汇聚了专业书画家、高校艺术教师及资深爱好者，通过多元活动弘扬中华优秀传统文化。', '以书画为友，以心书写“画”章', '第一章 总则\n墨渊书画协会是由书画艺术爱好者自愿组成的非营利性文化团体，成立于2015年。本协会以\"传承中华书画艺术，弘扬优秀传统文化\"为宗旨，致力于为广大书画爱好者提供学习交流的平台。协会坚持\"以艺会友、以文育人\"的理念，通过开展各类书画活动，促进会员艺术水平的提升，推动书画艺术的普及与发展。\n第二章 会员制度\n凡年满16周岁，热爱书画艺术并认可本会章程者，均可申请入会。会员享有参与协会活动、使用协会资源、选举和被选举等权利，同时需履行缴纳会费、遵守章程、维护协会声誉等义务。协会实行年度注册制，会员连续两年未参加活动且未缴纳会费者，视为自动退会。协会设立会员代表大会，作为最高权力机构，每年召开一次会议，审议协会重大事项。', '/api/files/logos/30479312-497a-4e73-bb2f-177c44f5eca8.png', NULL, 1, 1, '2025-11-17 22:46:18', NULL, '2025-11-17 22:45:56', '2025-11-17 22:46:17');
INSERT INTO `club_applications` VALUES (3, 6, '动漫社', '202203', '本动漫社是一个以动漫文化为核心，聚集ACG（动画、漫画、游戏）爱好者的文化创意社团。我们致力于为成员提供多元化的交流平台，通过开展动画赏析、漫画创作、角色扮演、周边交流、日语学习、同人作品制作等活动，探索动漫艺术的魅力。社团旨在营造开放、包容、互助的氛围，让每一位成员都能在这里找到共鸣，释放创意，共同成长。', '1、推广动漫文化：通过举办各类活动，向社会及校园传播积极健康的动漫文化，打破对动漫的刻板印象。\n2、提供创作交流平台：鼓励成员进行原创漫画、插画、Cosplay、视频剪辑等创作，并促进经验分享与技术互助。\n3、培养多元能力：在活动中锻炼成员的组织协作、艺术表达、语言学习等综合能力。\n4、凝聚同好社群：打造温暖友善的社团环境，让热爱动漫的伙伴找到归属感，共同构建有活力的兴趣共同体。', '第一条 本社团为ACG爱好者自发组织，遵守校规，接受学校指导。\n第二条 成员权利：参与活动、选举表决、提出建议。成员义务：遵守章程、积极参与、缴纳会费、维护社团声誉。\n第三条 设立社长、副社长统筹工作，下设活动、创作、宣传等部门，定期召开会议，决议需民主通过。\n第四条 常规活动包括动画观影、绘画交流、Cosplay、漫展参与等，旨在推广动漫文化，促进创作与交流。\n第五条 经费来源于会费及学校支持，公开管理。章程修改需经全体会议多数通过，自成立之日起生效。', NULL, NULL, 1, 1, '2026-01-29 10:55:50', '申请通过', '2026-01-29 10:49:07', '2026-01-29 10:55:49');
INSERT INTO `club_applications` VALUES (4, 6, '动漫社', '202203', '本动漫社是一个以动漫文化为核心，聚集ACG（动画、漫画、游戏）爱好者的文化创意社团。我们致力于为成员提供多元化的交流平台，通过开展动画赏析、漫画创作、角色扮演、周边交流、日语学习、同人作品制作等活动，探索动漫艺术的魅力。社团旨在营造开放、包容、互助的氛围，让每一位成员都能在这里找到共鸣，释放创意，共同成长。', '推广动漫文化：通过举办各类活动，向社会及校园传播积极健康的动漫文化，打破对动漫的刻板印象。\n提供创作交流平台：鼓励成员进行原创漫画、插画、Cosplay、视频剪辑等创作，并促进经验分享与技术互助。\n培养多元能力：在活动中锻炼成员的组织协作、艺术表达、语言学习等综合能力。\n凝聚同好社群：打造温暖友善的社团环境，让热爱动漫的伙伴找到归属感，共同构建有活力的兴趣共同体。', '第一条 本社团为ACG爱好者自发组织，遵守校规，接受学校指导。\n第二条 成员权利：参与活动、选举表决、提出建议。成员义务：遵守章程、积极参与、缴纳会费、维护社团声誉。\n第三条 设立社长、副社长统筹工作，下设活动、创作、宣传等部门，定期召开会议，决议需民主通过。\n第四条 常规活动包括动画观影、绘画交流、Cosplay、漫展参与等，旨在推广动漫文化，促进创作与交流。\n第五条 经费来源于会费及学校支持，公开管理。章程修改需经全体会议多数通过，自成立之日起生效。', NULL, NULL, 1, 1, '2026-01-30 16:51:27', NULL, '2026-01-30 16:50:56', '2026-01-30 16:51:27');
INSERT INTO `club_applications` VALUES (5, 6, '动漫社', '202203', '本动漫社是一个以动漫文化为核心，聚集ACG（动画、漫画、游戏）爱好者的文化创意社团。我们致力于为成员提供多元化的交流平台，通过开展动画赏析、漫画创作、角色扮演、周边交流、日语学习、同人作品制作等活动，探索动漫艺术的魅力。社团旨在营造开放、包容、互助的氛围，让每一位成员都能在这里找到共鸣，释放创意，共同成长。', '推广动漫文化：通过举办各类活动，向社会及校园传播积极健康的动漫文化，打破对动漫的刻板印象。\n提供创作交流平台：鼓励成员进行原创漫画、插画、Cosplay、视频剪辑等创作，并促进经验分享与技术互助。\n培养多元能力：在活动中锻炼成员的组织协作、艺术表达、语言学习等综合能力。\n凝聚同好社群：打造温暖友善的社团环境，让热爱动漫的伙伴找到归属感，共同构建有活力的兴趣共同体', '第一条 本社团为ACG爱好者自发组织，遵守校规，接受学校指导。\n第二条 成员权利：参与活动、选举表决、提出建议。成员义务：遵守章程、积极参与、缴纳会费、维护社团声誉。\n第三条 设立社长、副社长统筹工作，下设活动、创作、宣传等部门，定期召开会议，决议需民主通过。\n第四条 常规活动包括动画观影、绘画交流、Cosplay、漫展参与等，旨在推广动漫文化，促进创作与交流。\n第五条 经费来源于会费及学校支持，公开管理。章程修改需经全体会议多数通过，自成立之日起生效。', NULL, NULL, 1, 1, '2026-01-30 17:23:26', NULL, '2026-01-30 17:23:14', '2026-01-30 17:23:25');

-- ----------------------------
-- Table structure for club_dissolution_applications
-- ----------------------------
DROP TABLE IF EXISTS `club_dissolution_applications`;
CREATE TABLE `club_dissolution_applications`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '解散申请ID',
  `club_id` bigint(0) NOT NULL COMMENT '社团ID',
  `applicant_id` bigint(0) NOT NULL COMMENT '申请人ID',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '解散原因',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态（0-待审核，1-已通过，2-已驳回）',
  `reviewer_id` bigint(0) NULL DEFAULT NULL COMMENT '审核人ID',
  `review_time` datetime(0) NULL DEFAULT NULL COMMENT '审核时间',
  `review_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审核意见',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_club_dissolutions_club_id`(`club_id`) USING BTREE,
  INDEX `idx_club_dissolutions_applicant_id`(`applicant_id`) USING BTREE,
  INDEX `idx_club_dissolutions_status`(`status`) USING BTREE,
  INDEX `idx_club_dissolutions_reviewer_id`(`reviewer_id`) USING BTREE,
  INDEX `idx_club_dissolutions_created_time`(`created_time`) USING BTREE,
  CONSTRAINT `fk_club_dissolutions_applicant_id` FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_club_dissolutions_club_id` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_club_dissolutions_reviewer_id` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社团解散申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of club_dissolution_applications
-- ----------------------------
INSERT INTO `club_dissolution_applications` VALUES (1, 5, 6, '测试', 2, 1, '2026-01-30 17:24:25', '测试成功', '2026-01-30 17:23:56', '2026-01-30 17:24:24');

-- ----------------------------
-- Table structure for clubs
-- ----------------------------
DROP TABLE IF EXISTS `clubs`;
CREATE TABLE `clubs`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '社团ID',
  `club_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '社团名称',
  `club_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '社团代码',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '社团简介',
  `purpose` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '社团宗旨',
  `constitution` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '社团章程',
  `logo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Logo URL',
  `contact_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系邮箱',
  `founder_id` bigint(0) NOT NULL COMMENT '创始人ID',
  `president_id` bigint(0) NULL DEFAULT NULL COMMENT '社长ID',
  `member_count` int(0) NOT NULL DEFAULT 0 COMMENT '成员数量',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态（0-待审核，1-已通过，2-已驳回，3-已解散）',
  `established_time` datetime(0) NULL DEFAULT NULL COMMENT '成立时间',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_clubs_club_name`(`club_name`) USING BTREE,
  UNIQUE INDEX `uk_clubs_club_code`(`club_code`) USING BTREE,
  INDEX `idx_clubs_founder_id`(`founder_id`) USING BTREE,
  INDEX `idx_clubs_president_id`(`president_id`) USING BTREE,
  INDEX `idx_clubs_status`(`status`) USING BTREE,
  INDEX `idx_clubs_created_time`(`created_time`) USING BTREE,
  CONSTRAINT `fk_clubs_founder_id` FOREIGN KEY (`founder_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_clubs_president_id` FOREIGN KEY (`president_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社团表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of clubs
-- ----------------------------
INSERT INTO `clubs` VALUES (1, '计算机协会', '202201', '来到计算机协会，你将接受到不同的计算机技术，遇到问题，可以向学长学姐请教，在这个大家庭，敬请享受协会的温暖！', '码力全开，厚积薄发。', NULL, NULL, NULL, NULL, NULL, 2, 2, 3, 1, '2025-11-13 22:15:57', '2025-11-13 22:15:56', '2026-01-30 17:46:58');
INSERT INTO `clubs` VALUES (2, '墨渊书画协会', '202202', '墨渊书画协会成立于2015年，是由书画爱好者自发组成的非营利性文化团体，以“墨润心渊，书画传情”为宗旨，致力于传承中国传统书画艺术，推广现代创作理念。协会汇聚了专业书画家、高校艺术教师及资深爱好者，通过多元活动弘扬中华优秀传统文化。', '以书画为友，以心书写“画”章', '第一章 总则\n墨渊书画协会是由书画艺术爱好者自愿组成的非营利性文化团体，成立于2015年。本协会以\"传承中华书画艺术，弘扬优秀传统文化\"为宗旨，致力于为广大书画爱好者提供学习交流的平台。协会坚持\"以艺会友、以文育人\"的理念，通过开展各类书画活动，促进会员艺术水平的提升，推动书画艺术的普及与发展。\n第二章 会员制度\n凡年满16周岁，热爱书画艺术并认可本会章程者，均可申请入会。会员享有参与协会活动、使用协会资源、选举和被选举等权利，同时需履行缴纳会费、遵守章程、维护协会声誉等义务。协会实行年度注册制，会员连续两年未参加活动且未缴纳会费者，视为自动退会。协会设立会员代表大会，作为最高权力机构，每年召开一次会议，审议协会重大事项。', '/api/files/logos/30479312-497a-4e73-bb2f-177c44f5eca8.png', NULL, NULL, NULL, 4, 4, 2, 1, '2025-11-17 22:46:18', '2025-11-17 22:46:17', '2025-11-18 19:33:01');
INSERT INTO `clubs` VALUES (5, '动漫社', '202203', '本动漫社是一个以动漫文化为核心，聚集ACG（动画、漫画、游戏）爱好者的文化创意社团。我们致力于为成员提供多元化的交流平台，通过开展动画赏析、漫画创作、角色扮演、周边交流、日语学习、同人作品制作等活动，探索动漫艺术的魅力。社团旨在营造开放、包容、互助的氛围，让每一位成员都能在这里找到共鸣，释放创意，共同成长。', '推广动漫文化：通过举办各类活动，向社会及校园传播积极健康的动漫文化，打破对动漫的刻板印象。\n提供创作交流平台：鼓励成员进行原创漫画、插画、Cosplay、视频剪辑等创作，并促进经验分享与技术互助。\n培养多元能力：在活动中锻炼成员的组织协作、艺术表达、语言学习等综合能力。\n凝聚同好社群：打造温暖友善的社团环境，让热爱动漫的伙伴找到归属感，共同构建有活力的兴趣共同体', '第一条 本社团为ACG爱好者自发组织，遵守校规，接受学校指导。\n第二条 成员权利：参与活动、选举表决、提出建议。成员义务：遵守章程、积极参与、缴纳会费、维护社团声誉。\n第三条 设立社长、副社长统筹工作，下设活动、创作、宣传等部门，定期召开会议，决议需民主通过。\n第四条 常规活动包括动画观影、绘画交流、Cosplay、漫展参与等，旨在推广动漫文化，促进创作与交流。\n第五条 经费来源于会费及学校支持，公开管理。章程修改需经全体会议多数通过，自成立之日起生效。', NULL, NULL, NULL, NULL, 6, 6, 1, 1, '2026-01-30 17:23:26', '2026-01-30 17:23:25', '2026-01-30 17:23:25');

-- ----------------------------
-- Table structure for evaluations
-- ----------------------------
DROP TABLE IF EXISTS `evaluations`;
CREATE TABLE `evaluations`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `activity_id` bigint(0) NOT NULL COMMENT '活动ID',
  `user_id` bigint(0) NOT NULL COMMENT '评价人ID',
  `rating` tinyint(0) NOT NULL COMMENT '评分（1-5分）',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '评价内容',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_evaluations_activity_user`(`activity_id`, `user_id`) USING BTREE,
  INDEX `idx_evaluations_activity_id`(`activity_id`) USING BTREE,
  INDEX `idx_evaluations_user_id`(`user_id`) USING BTREE,
  INDEX `idx_evaluations_rating`(`rating`) USING BTREE,
  INDEX `idx_evaluations_created_time`(`created_time`) USING BTREE,
  CONSTRAINT `fk_evaluations_activity_id` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_evaluations_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评价表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of evaluations
-- ----------------------------
INSERT INTO `evaluations` VALUES (1, 4, 3, 5, '活动可以的', '2025-11-19 01:22:03', '2025-11-19 01:22:03');

-- ----------------------------
-- Table structure for member_applications
-- ----------------------------
DROP TABLE IF EXISTS `member_applications`;
CREATE TABLE `member_applications`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `club_id` bigint(0) NOT NULL COMMENT '社团ID',
  `applicant_id` bigint(0) NOT NULL COMMENT '申请人ID',
  `application_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '申请理由',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态（0-待审核，1-已通过，2-已驳回）',
  `reviewer_id` bigint(0) NULL DEFAULT NULL COMMENT '审核人ID',
  `review_time` datetime(0) NULL DEFAULT NULL COMMENT '审核时间',
  `review_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审核意见',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_applications_club_id`(`club_id`) USING BTREE,
  INDEX `idx_member_applications_applicant_id`(`applicant_id`) USING BTREE,
  INDEX `idx_member_applications_status`(`status`) USING BTREE,
  INDEX `idx_member_applications_reviewer_id`(`reviewer_id`) USING BTREE,
  INDEX `idx_member_applications_created_time`(`created_time`) USING BTREE,
  CONSTRAINT `fk_member_applications_applicant_id` FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_member_applications_club_id` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_member_applications_reviewer_id` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '入社申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_applications
-- ----------------------------
INSERT INTO `member_applications` VALUES (1, 1, 3, '我想学计算机', 1, 2, '2025-11-17 12:20:05', NULL, '2025-11-16 13:39:37', '2025-11-17 12:20:04');
INSERT INTO `member_applications` VALUES (2, 1, 2, NULL, 1, 2, '2025-11-17 12:29:46', NULL, '2025-11-17 12:29:43', '2025-11-17 12:29:46');
INSERT INTO `member_applications` VALUES (3, 1, 5, NULL, 1, 2, '2025-11-19 01:08:51', NULL, '2025-11-18 19:32:04', '2025-11-19 01:08:50');
INSERT INTO `member_applications` VALUES (4, 2, 3, NULL, 1, 4, '2025-11-18 19:33:02', NULL, '2025-11-18 19:32:25', '2025-11-18 19:33:01');
INSERT INTO `member_applications` VALUES (5, 1, 7, NULL, 1, 2, '2026-01-29 17:01:10', NULL, '2026-01-29 17:00:41', '2026-01-29 17:01:10');

-- ----------------------------
-- Table structure for members
-- ----------------------------
DROP TABLE IF EXISTS `members`;
CREATE TABLE `members`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '成员ID',
  `club_id` bigint(0) NOT NULL COMMENT '社团ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色（MEMBER-普通成员，STAFF-干事，VICE_MINISTER-副部长，MINISTER-部长）',
  `join_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '加入时间',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态（0-已退出，1-正常）',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_members_club_user`(`club_id`, `user_id`) USING BTREE,
  INDEX `idx_members_club_id`(`club_id`) USING BTREE,
  INDEX `idx_members_user_id`(`user_id`) USING BTREE,
  INDEX `idx_members_role`(`role`) USING BTREE,
  INDEX `idx_members_status`(`status`) USING BTREE,
  CONSTRAINT `fk_members_club_id` FOREIGN KEY (`club_id`) REFERENCES `clubs` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_members_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '成员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of members
-- ----------------------------
INSERT INTO `members` VALUES (2, 1, 3, 'MINISTER', '2025-11-17 12:20:05', 1, '2025-11-17 12:20:04', '2025-11-19 00:30:33');
INSERT INTO `members` VALUES (3, 1, 2, 'MINISTER', '2025-11-17 12:29:46', 1, '2025-11-17 12:29:46', '2026-01-30 17:38:08');
INSERT INTO `members` VALUES (4, 2, 4, 'MINISTER', '2025-11-17 22:46:18', 1, '2025-11-17 22:46:17', '2025-11-17 22:46:17');
INSERT INTO `members` VALUES (5, 2, 3, 'STAFF', '2025-11-18 19:33:02', 1, '2025-11-18 19:33:01', '2025-11-18 23:18:59');
INSERT INTO `members` VALUES (6, 1, 5, 'MEMBER', '2025-11-19 01:08:51', 1, '2025-11-19 01:08:50', '2025-11-19 01:08:50');
INSERT INTO `members` VALUES (8, 1, 7, 'MEMBER', '2026-01-29 17:01:10', 0, '2026-01-29 17:01:10', '2026-03-18 13:08:56');
INSERT INTO `members` VALUES (10, 5, 6, 'MINISTER', '2026-01-30 17:23:26', 1, '2026-01-30 17:23:25', '2026-01-30 17:23:25');

-- ----------------------------
-- Table structure for notifications
-- ----------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(0) NOT NULL COMMENT '接收人ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '内容',
  `related_id` bigint(0) NULL DEFAULT NULL COMMENT '关联业务ID',
  `read_flag` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `read_time` datetime(0) NULL DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notifications_user_id`(`user_id`) USING BTREE,
  INDEX `idx_notifications_read_flag`(`read_flag`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '站内消息通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notifications
-- ----------------------------
INSERT INTO `notifications` VALUES (23, 6, 'CLUB_APPLICATION_RESULT', '社团创建申请审核通过', '您提交的社团【动漫社】创建申请已被通过。', 5, 1, '2026-01-30 17:23:25', '2026-01-30 17:24:01');
INSERT INTO `notifications` VALUES (24, 1, 'CLUB_DISSOLUTION_APPLICATION', '社团解散申请待审核', '社团【动漫社】提交了解散申请，原因：测试', 1, 1, '2026-01-30 17:23:56', '2026-01-30 17:24:13');
INSERT INTO `notifications` VALUES (25, 6, 'CLUB_APPLICATION_RESULT', '社团解散申请审核未通过', '您提交的社团【动漫社】解散申请已被驳回。 审核意见：测试成功', 1, 1, '2026-01-30 17:24:24', '2026-01-30 17:24:48');
INSERT INTO `notifications` VALUES (26, 2, 'ROLE_CHANGED', '社团角色变更通知', '您在社团【计算机协会】的角色已变更为：副部长。', 1, 1, '2026-01-30 17:38:05', '2026-01-30 17:38:36');
INSERT INTO `notifications` VALUES (27, 7, 'ROLE_CHANGED', '社团角色变更通知', '您在社团【计算机协会】的角色已变更为：副部长。', 1, 1, '2026-01-30 17:38:25', '2026-03-17 10:57:24');
INSERT INTO `notifications` VALUES (28, 7, 'ROLE_CHANGED', '社团角色变更通知', '您在社团【计算机协会】的角色已变更为：成员。', 1, 1, '2026-01-30 17:38:28', '2026-03-17 10:57:24');
INSERT INTO `notifications` VALUES (29, 3, 'ROLE_CHANGED', '社团角色变更通知', '您已成为社团【计算机协会】的社长。', 1, 1, '2026-01-30 17:46:16', '2026-01-30 17:46:48');
INSERT INTO `notifications` VALUES (30, 2, 'ROLE_CHANGED', '社团角色变更通知', '您在社团【计算机协会】的角色已变更为：成员。', 1, 1, '2026-01-30 17:46:16', '2026-01-30 17:46:23');
INSERT INTO `notifications` VALUES (31, 2, 'ROLE_CHANGED', '社团角色变更通知', '您已成为社团【计算机协会】的社长。', 1, 1, '2026-01-30 17:46:58', '2026-01-30 17:47:18');
INSERT INTO `notifications` VALUES (32, 3, 'ROLE_CHANGED', '社团角色变更通知', '您在社团【计算机协会】的角色已变更为：成员。', 1, 1, '2026-01-30 17:46:58', '2026-01-30 17:47:04');
INSERT INTO `notifications` VALUES (33, 7, 'ROLE_CHANGED', '社团角色变更通知', '您在社团【计算机协会】的角色已变更为：成员。', 1, 0, '2026-03-18 13:08:52', NULL);
INSERT INTO `notifications` VALUES (34, 7, 'ROLE_CHANGED', '社团角色变更通知', '您在社团【计算机协会】的角色已变更为：副部长。', 1, 0, '2026-03-18 13:08:54', NULL);
INSERT INTO `notifications` VALUES (35, 7, 'ROLE_CHANGED', '社团角色变更通知', '您在社团【计算机协会】的角色已变更为：成员。', 1, 0, '2026-03-18 13:08:56', NULL);

-- ----------------------------
-- Table structure for operation_logs
-- ----------------------------
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '操作用户ID',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型',
  `operation_module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作模块',
  `operation_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作描述',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '请求参数',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `operation_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '操作时间',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态（0-失败，1-成功）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation_logs_user_id`(`user_id`) USING BTREE,
  INDEX `idx_operation_logs_operation_type`(`operation_type`) USING BTREE,
  INDEX `idx_operation_logs_operation_module`(`operation_module`) USING BTREE,
  INDEX `idx_operation_logs_operation_time`(`operation_time`) USING BTREE,
  CONSTRAINT `fk_operation_logs_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of operation_logs
-- ----------------------------

-- ----------------------------
-- Table structure for registrations
-- ----------------------------
DROP TABLE IF EXISTS `registrations`;
CREATE TABLE `registrations`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `activity_id` bigint(0) NOT NULL COMMENT '活动ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `registration_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '报名时间',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态（0-已取消，1-已报名）',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_registrations_activity_user`(`activity_id`, `user_id`) USING BTREE,
  INDEX `idx_registrations_activity_id`(`activity_id`) USING BTREE,
  INDEX `idx_registrations_user_id`(`user_id`) USING BTREE,
  INDEX `idx_registrations_status`(`status`) USING BTREE,
  INDEX `idx_registrations_registration_time`(`registration_time`) USING BTREE,
  CONSTRAINT `fk_registrations_activity_id` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_registrations_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '报名表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of registrations
-- ----------------------------
INSERT INTO `registrations` VALUES (1, 1, 2, '2025-11-16 23:33:37', 1, '2025-11-16 23:33:36', '2025-11-16 23:36:55');
INSERT INTO `registrations` VALUES (3, 4, 3, '2025-11-19 00:13:10', 1, '2025-11-19 00:13:10', '2025-11-19 00:13:10');
INSERT INTO `registrations` VALUES (4, 5, 5, '2025-11-19 01:21:09', 1, '2025-11-19 01:21:08', '2025-11-19 01:21:08');
INSERT INTO `registrations` VALUES (5, 7, 2, '2026-01-29 15:40:50', 0, '2026-01-29 15:40:50', '2026-01-29 15:40:53');

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色代码',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色描述',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_roles_role_code`(`role_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of roles
-- ----------------------------
INSERT INTO `roles` VALUES (1, 'STUDENT', '学生', '普通学生用户', 1, '2025-11-11 16:24:10', '2025-11-11 16:24:10');
INSERT INTO `roles` VALUES (2, 'CLUB_ADMIN', '社团管理员', '社团管理员，可管理本社团', 1, '2025-11-11 16:24:10', '2025-11-11 16:24:10');
INSERT INTO `roles` VALUES (3, 'SYSTEM_ADMIN', '系统管理员', '系统管理员，拥有最高权限', 1, '2025-11-11 16:24:10', '2025-11-11 16:24:10');

-- ----------------------------
-- Table structure for sign_ins
-- ----------------------------
DROP TABLE IF EXISTS `sign_ins`;
CREATE TABLE `sign_ins`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '签到ID',
  `activity_id` bigint(0) NOT NULL COMMENT '活动ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `sign_in_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '签到时间',
  `sign_in_type` tinyint(0) NOT NULL DEFAULT 1 COMMENT '签到方式（1-手动签到，2-二维码签到）',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '签到地点',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sign_ins_activity_user`(`activity_id`, `user_id`) USING BTREE,
  INDEX `idx_sign_ins_activity_id`(`activity_id`) USING BTREE,
  INDEX `idx_sign_ins_user_id`(`user_id`) USING BTREE,
  INDEX `idx_sign_ins_sign_in_time`(`sign_in_time`) USING BTREE,
  CONSTRAINT `fk_sign_ins_activity_id` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_sign_ins_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '签到表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sign_ins
-- ----------------------------
INSERT INTO `sign_ins` VALUES (3, 4, 3, '2025-11-19 00:16:58', 1, '1', '1', '2025-11-19 00:16:57');

-- ----------------------------
-- Table structure for user_roles
-- ----------------------------
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `role_id` bigint(0) NOT NULL COMMENT '角色ID',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_roles_user_role`(`user_id`, `role_id`) USING BTREE,
  INDEX `idx_user_roles_user_id`(`user_id`) USING BTREE,
  INDEX `idx_user_roles_role_id`(`role_id`) USING BTREE,
  CONSTRAINT `fk_user_roles_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_roles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_roles
-- ----------------------------
INSERT INTO `user_roles` VALUES (1, 1, 3, '2025-11-11 16:24:10');
INSERT INTO `user_roles` VALUES (2, 2, 1, '2025-11-13 19:42:35');
INSERT INTO `user_roles` VALUES (3, 3, 1, '2025-11-13 21:50:02');
INSERT INTO `user_roles` VALUES (5, 4, 1, '2025-11-17 22:41:58');
INSERT INTO `user_roles` VALUES (6, 4, 2, '2025-11-17 22:46:17');
INSERT INTO `user_roles` VALUES (7, 5, 1, '2025-11-18 19:12:19');
INSERT INTO `user_roles` VALUES (10, 6, 1, '2026-01-28 21:50:16');
INSERT INTO `user_roles` VALUES (11, 6, 2, '2026-01-29 10:55:49');
INSERT INTO `user_roles` VALUES (12, 7, 1, '2026-01-29 16:27:10');
INSERT INTO `user_roles` VALUES (18, 2, 2, '2026-01-30 17:46:58');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（加密存储）',
  `password_changed_time` datetime(0) NULL DEFAULT NULL COMMENT '密码修改时间',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '真实姓名',
  `student_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `gender` tinyint(0) NULL DEFAULT 0 COMMENT '性别（0-未知，1-男，2-女）',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像URL',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
  `last_login_time` datetime(0) NULL DEFAULT NULL COMMENT '最后登录时间',
  `login_failure_count` int(0) NOT NULL DEFAULT 0 COMMENT '登录失败次数',
  `account_locked_until` datetime(0) NULL DEFAULT NULL COMMENT '账户锁定截止时间',
  `password_reset_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码重置令牌',
  `password_reset_token_expiry` datetime(0) NULL DEFAULT NULL COMMENT '密码重置令牌过期时间',
  `created_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_users_username`(`username`) USING BTREE,
  UNIQUE INDEX `uk_users_student_id`(`student_id`) USING BTREE,
  INDEX `idx_users_status`(`status`) USING BTREE,
  INDEX `idx_users_created_time`(`created_time`) USING BTREE,
  INDEX `idx_users_password_reset_token`(`password_reset_token`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin', '$2b$10$r8Xzu5gkYSK6fgZGfAU8reiTzr7j3qEl9uYnPjM4q/05NWafW9NQK', '2025-11-11 16:24:10', '系统管理员', NULL, 'admin@qq.com', '15314672161', 1, '/api/files/avatars/132667cf-123d-40d3-a362-c16f843e3332.jpg', 1, '2026-04-13 15:31:20', 0, NULL, NULL, NULL, '2025-11-11 16:24:10', '2026-04-13 15:31:20');
INSERT INTO `users` VALUES (2, '凯日夏夜', '$2a$10$9bhYmiYGm0d.sHEbYVmQuuKW1Aqc1i0GCPMI9ZD7J4OYPPCTMVFTu', '2025-11-13 19:42:35', '韦', '20220401024', '22@qq.com', '15797293819', 2, '/api/files/avatars/b1db2d90-6200-40cf-8df3-b5619c4ecec8.png', 1, '2026-03-18 13:26:32', 0, NULL, 'bc5bc6140d174a9abd28b990089e2ecc', '2026-01-30 15:01:14', '2025-11-13 19:42:35', '2026-03-18 13:26:31');
INSERT INTO `users` VALUES (3, '夏夜眠', '$2a$10$KSNsqtL3XHCgRBaZ1ZetXuhSzir04u6o1xDO4VyZCnWzFbtNwSzqW', '2025-11-13 21:50:02', '韦佳怡', '20220401006', NULL, NULL, 2, '/api/files/avatars/6481e1d0-8c00-4143-a342-6e3515128a93.jpg', 1, '2026-03-17 17:10:20', 0, NULL, NULL, NULL, '2025-11-13 21:50:02', '2026-03-17 17:10:20');
INSERT INTO `users` VALUES (4, '凯夏夜', '$2a$10$A9FqfTpyepLu8JtxXSBNz.LuKSrNvCQOwlTpsGiOboNsRG0wyUzwC', '2025-11-17 22:41:58', '韦贺', '20220401025', NULL, NULL, 0, '/api/files/avatars/5ca3bcce-0560-4641-94ef-a20f343d6040.jpeg', 1, '2026-03-17 17:10:03', 0, NULL, NULL, NULL, '2025-11-17 22:41:58', '2026-03-17 17:10:03');
INSERT INTO `users` VALUES (5, '祁浩天', '$2a$10$geFL/Bh4AdBqOC3xsne77OkHrrzA4tew/vXAAgvvMGqC0l6fB7tva', '2025-11-18 19:12:20', '祁', '20220401001', NULL, NULL, NULL, NULL, 1, '2026-03-17 17:12:38', 0, NULL, NULL, NULL, '2025-11-18 19:12:19', '2026-03-17 17:12:38');
INSERT INTO `users` VALUES (6, '天南海北', '$2a$10$MAyfsoLrvRuCBPGmlCetJOv7DoSzv21xv1L4rmXwK9svBPkzqk09W', '2026-01-28 21:50:17', '凯', NULL, NULL, NULL, NULL, NULL, 1, '2026-01-30 17:24:43', 0, NULL, NULL, NULL, '2026-01-28 21:50:16', '2026-01-30 17:24:42');
INSERT INTO `users` VALUES (7, '不参加社团', '$2a$10$s7dCNmMFfaEfbxArq0HgMuJ36gIXv1dUqFpCaxwM5pYLDGUTtAmma', '2026-01-29 16:27:10', '测试', '20220401002', '123?@qq.com', NULL, NULL, NULL, 1, '2026-03-18 12:56:52', 0, NULL, NULL, NULL, '2026-01-29 16:27:10', '2026-03-18 12:56:52');

SET FOREIGN_KEY_CHECKS = 1;
