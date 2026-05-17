package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体类
 * 用于存储活动评价信息
 */
@Data
public class Evaluation {
	// 评价ID，主键
	private Long id;
	// 活动ID
	private Long activityId;
	// 评价人ID
	private Long userId;
	// 评分（1-5分）
	private Integer rating;
	// 评价内容
	private String comment;
	// 创建时间
	private LocalDateTime createdTime;
	// 更新时间
	private LocalDateTime updatedTime;
}

