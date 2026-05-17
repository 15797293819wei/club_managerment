package com.example.club.service;

import com.example.club.mapper.ProfileMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 个人中心相关服务
 */
@Service
public class ProfileService {

	private static final int DEFAULT_LIMIT = 6;

	private final ClubService clubService;
	private final ProfileMapper profileMapper;

	public ProfileService(ClubService clubService, ProfileMapper profileMapper) {
		this.clubService = clubService;
		this.profileMapper = profileMapper;
	}

	/**
	 * 获取个人概览数据
	 */
	public PersonalOverview getPersonalOverview(Long userId) {
		var joinedClubs = clubService.getJoinedClubs(userId);
		var activities = profileMapper.selectRecentActivities(userId, DEFAULT_LIMIT).stream()
			.map(row -> new ActivityRecord(
				row.getId(),
				row.getActivityId(),
				row.getClubId(),
				row.getClubName(),
				row.getActivityName(),
				row.getStartTime(),
				row.getEndTime(),
				row.getRegistrationTime(),
				row.getActivityStatus(),
				row.getRegistrationStatus()
			))
			.toList();
		var attendance = profileMapper.selectRecentAttendance(userId, DEFAULT_LIMIT).stream()
			.map(row -> new AttendanceRecord(
				row.getId(),
				row.getClubId(),
				row.getClubName(),
				row.getActivityId(),
				row.getActivityName(),
				row.getAttendanceType(),
				row.getAttendanceTime(),
				row.getRemark()
			))
			.toList();
		var evaluations = profileMapper.selectRecentEvaluations(userId, DEFAULT_LIMIT).stream()
			.map(row -> new EvaluationRecord(
				row.getId(),
				row.getActivityId(),
				row.getActivityName(),
				row.getClubName(),
				row.getRating(),
				row.getComment(),
				row.getCreatedTime()
			))
			.toList();
		var statsRow = profileMapper.selectPersonalStats(userId);
		var stats = new PersonalStats(
			joinedClubs.size(),
			statsRow != null && statsRow.getRegistrationCount() != null ? statsRow.getRegistrationCount() : 0L,
			statsRow != null && statsRow.getAttendanceCount() != null ? statsRow.getAttendanceCount() : 0L,
			statsRow != null && statsRow.getSignInCount() != null ? statsRow.getSignInCount() : 0L,
			statsRow != null && statsRow.getEvaluationCount() != null ? statsRow.getEvaluationCount() : 0L,
			calcSignInRate(statsRow),
			statsRow != null ? statsRow.getAverageRating() : null
		);
		return new PersonalOverview(joinedClubs, activities, attendance, evaluations, stats);
	}

	private double calcSignInRate(ProfileMapper.PersonalStatsRow statsRow) {
		if (statsRow == null) {
			return 0D;
		}
		Long registrationCount = statsRow.getRegistrationCount();
		Long signInCount = statsRow.getSignInCount();
		if (registrationCount == null || registrationCount == 0) {
			return 0D;
		}
		long signIns = signInCount != null ? signInCount : 0L;
		return Math.min(1D, signIns / (double) registrationCount);
	}

	public record PersonalOverview(
		List<ClubService.JoinedClubInfo> joinedClubs,
		List<ActivityRecord> activities,
		List<AttendanceRecord> attendance,
		List<EvaluationRecord> evaluations,
		PersonalStats stats
	) {
	}

	public record ActivityRecord(Long id, Long activityId, Long clubId, String clubName, String activityName,
								 java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
								 java.time.LocalDateTime registrationTime, Integer activityStatus, Integer registrationStatus) {
	}

	public record AttendanceRecord(Long id, Long clubId, String clubName, Long activityId, String activityName,
								   Integer attendanceType, java.time.LocalDateTime attendanceTime, String remark) {
	}

	public record EvaluationRecord(Long id, Long activityId, String activityName, String clubName,
								   Integer rating, String comment, java.time.LocalDateTime createdTime) {
	}

	public record PersonalStats(int joinedClubCount, Long activityCount, Long attendanceCount, Long signInCount,
								Long evaluationCount, double signInRate, Double averageRating) {
	}
}

