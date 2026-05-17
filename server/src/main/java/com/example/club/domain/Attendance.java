package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考勤实体类
 * 用于存储成员考勤记录
 */
@Data
public class Attendance {
	// 考勤ID，主键
	private Long id;
	// 社团ID
	private Long clubId;
	// 活动ID（可为空，表示日常考勤）
	private Long activityId;
	// 成员ID
	private Long memberId;
	// 考勤类型（1-正常，2-迟到，3-早退，4-缺勤）
	private Integer attendanceType;
	// 考勤时间
	private LocalDateTime attendanceTime;
	// 备注
	private String remark;
	// 是否异常
	private Boolean exceptionFlag;
	// 异常处理状态：0-待处理，1-已处理
	private Integer exceptionStatus;
	// 异常原因/说明
	private String exceptionReason;
	// 处理人
	private Long handledBy;
	// 处理时间
	private LocalDateTime handledTime;
	// 创建时间
	private LocalDateTime createdTime;
}

