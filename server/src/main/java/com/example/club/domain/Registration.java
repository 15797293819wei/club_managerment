package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名实体类
 * 用于存储活动报名信息
 */
@Data
public class Registration {
	// 报名ID，主键
	private Long id;
	// 活动ID
	private Long activityId;
	// 用户ID
	private Long userId;
	// 报名时间
	private LocalDateTime registrationTime;
	// 状态（0-已取消，1-已报名）
	private Integer status;
	// 创建时间
	private LocalDateTime createdTime;
	// 更新时间
	private LocalDateTime updatedTime;
}

