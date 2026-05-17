package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社团考勤规则配置
 */
@Data
public class AttendanceRule {
	private Long id;
	private Long clubId;
	private Integer lateThreshold;
	private Integer leaveEarlyThreshold;
	private Integer absenceThreshold;
	private LocalDateTime createdTime;
	private LocalDateTime updatedTime;
}


