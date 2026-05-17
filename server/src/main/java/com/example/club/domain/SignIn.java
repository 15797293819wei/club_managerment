package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签到实体类
 * 用于存储活动签到信息
 */
@Data
public class SignIn {
	// 签到ID，主键
	private Long id;
	// 活动ID
	private Long activityId;
	// 用户ID
	private Long userId;
	// 签到时间
	private LocalDateTime signInTime;
	// 签到方式（1-手动签到，2-二维码签到）
	private Integer signInType;
	// 签到地点
	private String location;
	// 备注
	private String remark;
	// 创建时间
	private LocalDateTime createdTime;
}

