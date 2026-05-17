package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动实体类
 * 用于存储社团活动信息，包括活动名称、类型、时间、地点、参与人数等
 */
@Data
public class Activity {
	// 活动ID，主键
	private Long id;
	// 社团ID
	private Long clubId;
	// 活动名称
	private String activityName;
	// 活动类型
	private String activityType;
	// 活动描述
	private String description;
	// 开始时间
	private LocalDateTime startTime;
	// 结束时间
	private LocalDateTime endTime;
	// 活动地点
	private String location;
	// 最大参与人数
	private Integer maxParticipants;
	// 当前参与人数
	private Integer currentParticipants;
	// 报名截止时间
	private LocalDateTime registrationDeadline;
	// 状态（0-待开始，1-进行中，2-已结束，3-已取消）
	private Integer status;
	// 创建人ID
	private Long creatorId;
	// 创建时间
	private LocalDateTime createdTime;
	// 更新时间
	private LocalDateTime updatedTime;
}

