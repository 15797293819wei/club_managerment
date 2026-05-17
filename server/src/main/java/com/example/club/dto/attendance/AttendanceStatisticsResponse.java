package com.example.club.dto.attendance;

import java.util.List;

/**
 * 考勤统计响应
 */
public record AttendanceStatisticsResponse(
	Summary summary,
	List<DimensionStat> memberStats,
	List<DimensionStat> activityStats,
	List<DimensionStat> dateStats
) {
	public record Summary(
		long totalCount,
		long normalCount,
		long lateCount,
		long leaveEarlyCount,
		long absentCount,
		long exceptionCount
	) {}

	public record DimensionStat(
		Long refId,
		String refName,
		long totalCount,
		long normalCount,
		long lateCount,
		long leaveEarlyCount,
		long absentCount,
		long exceptionCount
	) {}
}


