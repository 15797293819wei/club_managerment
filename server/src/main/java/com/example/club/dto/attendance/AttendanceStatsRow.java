package com.example.club.dto.attendance;

import lombok.Data;

/**
 * 通用考勤统计行
 */
@Data
public class AttendanceStatsRow {
	private Long refId;
	private String refName;
	private Long totalCount;
	private Long normalCount;
	private Long lateCount;
	private Long leaveEarlyCount;
	private Long absentCount;
	private Long exceptionCount;
}


