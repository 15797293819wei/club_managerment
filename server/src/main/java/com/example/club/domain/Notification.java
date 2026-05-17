package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息 / 通知实体
 */
@Data
public class Notification {
	private Long id;
	// 接收人
	private Long userId;
	// 类型：APPLICATION_RESULT、ACTIVITY_REGISTRATION、ACTIVITY_REMINDER、EVALUATION_NOTICE、ROLE_CHANGED 等
	private String type;
	// 标题
	private String title;
	// 内容
	private String content;
	// 关联业务ID（如申请ID、活动ID、社团ID）
	private Long relatedId;
	// 是否已读：0-未读，1-已读
	private Integer readFlag;
	// 创建时间
	private LocalDateTime createdTime;
	// 阅读时间
	private LocalDateTime readTime;
}


