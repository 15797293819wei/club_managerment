package com.example.club.mapper;

import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人中心数据查询 Mapper
 */
@Mapper
public interface ProfileMapper {

	List<ActivityRecordRow> selectRecentActivities(@Param("userId") Long userId, @Param("limit") int limit);

	List<AttendanceRecordRow> selectRecentAttendance(@Param("userId") Long userId, @Param("limit") int limit);

	List<EvaluationRecordRow> selectRecentEvaluations(@Param("userId") Long userId, @Param("limit") int limit);

	PersonalStatsRow selectPersonalStats(@Param("userId") Long userId);

	@Data
	class ActivityRecordRow {
		private Long id;
		private Long activityId;
		private Long clubId;
		private String clubName;
		private String activityName;
		private LocalDateTime startTime;
		private LocalDateTime endTime;
		private LocalDateTime registrationTime;
		private Integer activityStatus;
		private Integer registrationStatus;
	}

	@Data
	class AttendanceRecordRow {
		private Long id;
		private Long clubId;
		private String clubName;
		private Long activityId;
		private String activityName;
		private Integer attendanceType;
		private LocalDateTime attendanceTime;
		private String remark;
	}

	@Data
	class EvaluationRecordRow {
		private Long id;
		private Long activityId;
		private String activityName;
		private String clubName;
		private Integer rating;
		private String comment;
		private LocalDateTime createdTime;
	}

	@Data
	class PersonalStatsRow {
		private Long joinedClubCount;
		private Long registrationCount;
		private Long signInCount;
		private Long attendanceCount;
		private Long evaluationCount;
		private Double averageRating;
	}
}

