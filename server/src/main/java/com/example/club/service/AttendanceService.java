package com.example.club.service;

import com.example.club.domain.Activity;
import com.example.club.domain.Attendance;
import com.example.club.domain.AttendanceRule;
import com.example.club.dto.PageResult;
import com.example.club.dto.attendance.AttendanceStatisticsResponse;
import com.example.club.dto.attendance.AttendanceStatsRow;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ActivityMapper;
import com.example.club.mapper.AttendanceMapper;
import com.example.club.mapper.AttendanceRuleMapper;
import com.example.club.mapper.ClubMapper;
import com.example.club.mapper.MemberMapper;
import com.example.club.mapper.UserMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 考勤领域服务
 * <p>处理考勤记录的增删改查等核心业务逻辑。</p>
 */
@Service
public class AttendanceService {

	private final AttendanceMapper attendanceMapper;
	private final ActivityMapper activityMapper;
	private final AttendanceRuleMapper attendanceRuleMapper;
	private final ClubMapper clubMapper;
	private final MemberMapper memberMapper;
	private final UserMapper userMapper;

	public AttendanceService(AttendanceMapper attendanceMapper,
							 ActivityMapper activityMapper,
							 AttendanceRuleMapper attendanceRuleMapper,
							 ClubMapper clubMapper,
							 MemberMapper memberMapper,
							 UserMapper userMapper) {
		this.attendanceMapper = attendanceMapper;
		this.activityMapper = activityMapper;
		this.attendanceRuleMapper = attendanceRuleMapper;
		this.clubMapper = clubMapper;
		this.memberMapper = memberMapper;
		this.userMapper = userMapper;
	}

	/**
	 * 分页查询考勤记录列表
	 *
	 * @param clubId     社团ID（可选）
	 * @param activityId 活动ID（可选）
	 * @param memberId   成员ID（可选）
	 * @param page        页码
	 * @param size        页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<AttendanceInfo> page(Long clubId, Long activityId, Long memberId,
										   LocalDateTime startTime, LocalDateTime endTime,
										   int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = attendanceMapper.countByFilters(clubId, activityId, memberId, startTime, endTime);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Attendance> attendances = attendanceMapper.selectPage(clubId, activityId, memberId, startTime, endTime, offset, size);
		// 批量查询社团名称
		Map<Long, String> clubNames = attendances.stream()
			.map(Attendance::getClubId)
			.distinct()
			.collect(Collectors.toMap(
				id -> id,
				id -> clubMapper.selectById(id).map(c -> c.getClubName()).orElse("未知社团")
			));
		Map<Long, String> activityNames = attendances.stream()
			.map(Attendance::getActivityId)
			.filter(Objects::nonNull)
			.distinct()
			.collect(Collectors.toMap(
				id -> id,
				id -> activityMapper.selectById(id).map(Activity::getActivityName).orElse("活动-" + id)
			));
		// 批量查询成员信息（memberId -> userId）
		Map<Long, Long> memberToUser = attendances.stream()
			.map(Attendance::getMemberId)
			.distinct()
			.collect(Collectors.toMap(
				mId -> mId,
				mId -> memberMapper.selectById(mId).map(m -> m.getUserId()).orElse(-1L)
			));
		// 批量查询用户名
		List<Long> userIds = memberToUser.values().stream().filter(id -> id > 0).distinct().collect(Collectors.toList());
		Map<Long, String> usernames = userIds.isEmpty() ? Map.of() :
			userMapper.selectUsernamesByIds(userIds).stream()
				.collect(Collectors.toMap(
					m -> ((Number) m.get("id")).longValue(),
					m -> (String) m.get("username"),
					(v1, v2) -> v1
				));
		// 构建memberId到username的映射
		Map<Long, String> memberUsernames = memberToUser.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				e -> usernames.getOrDefault(e.getValue(), "未知用户")
			));
		List<AttendanceInfo> records = attendances.stream()
			.map(a -> toAttendanceInfo(
				a,
				clubNames.get(a.getClubId()),
				memberUsernames.get(a.getMemberId()),
				a.getActivityId() != null ? activityNames.getOrDefault(a.getActivityId(), "活动-" + a.getActivityId()) : "日常考勤"
			))
			.toList();
		return new PageResult<>(total, records);
	}

	/**
	 * 查询考勤记录详情
	 *
	 * @param id 考勤ID
	 * @return 考勤记录详情
	 */
	@Transactional(readOnly = true)
	public AttendanceInfo getDetail(Long id) {
		Attendance attendance = attendanceMapper.selectById(id);
		if (attendance == null) {
			throw new BusinessException(40406, "考勤记录不存在");
		}
		String clubName = clubMapper.selectById(attendance.getClubId())
			.map(c -> c.getClubName())
			.orElse("未知社团");
		Long userId = memberMapper.selectById(attendance.getMemberId())
			.map(m -> m.getUserId())
			.orElse(-1L);
		String memberUsername = userId > 0 ? 
			userMapper.selectUsernamesByIds(List.of(userId)).stream()
				.filter(m -> ((Number) m.get("id")).longValue() == userId)
				.findFirst()
				.map(m -> (String) m.get("username"))
				.orElse("未知用户") : "未知用户";
		String activityName = attendance.getActivityId() != null
			? activityMapper.selectById(attendance.getActivityId()).map(Activity::getActivityName).orElse("活动-" + attendance.getActivityId())
			: "日常考勤";
		return toAttendanceInfo(attendance, clubName, memberUsername, activityName);
	}

	/**
	 * 创建考勤记录
	 *
	 * @param command 创建命令
	 * @return 新考勤记录ID
	 */
	@Transactional
	public Long create(AttendanceCommand command) {
		// 校验社团是否存在
		clubMapper.selectById(command.clubId())
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));

		// 校验成员是否存在且属于该社团
		var member = memberMapper.selectById(command.memberId())
			.orElseThrow(() -> new BusinessException(40404, "成员不存在"));
		if (!member.getClubId().equals(command.clubId())) {
			throw new BusinessException(40036, "成员不属于该社团");
		}
		if (member.getStatus() != 1) {
			throw new BusinessException(40037, "成员状态异常，无法记录考勤");
		}

		// 验证考勤类型
		if (command.attendanceType() == null || command.attendanceType() < 1 || command.attendanceType() > 4) {
			throw new BusinessException(40038, "考勤类型值非法，可选值：1-正常，2-迟到，3-早退，4-缺勤");
		}

		// 创建考勤记录
		Attendance attendance = new Attendance();
		attendance.setClubId(command.clubId());
		attendance.setActivityId(command.activityId());
		attendance.setMemberId(command.memberId());
		attendance.setAttendanceType(command.attendanceType());
		attendance.setAttendanceTime(command.attendanceTime() != null ? command.attendanceTime() : LocalDateTime.now());
		attendance.setRemark(command.remark());
		attendanceMapper.insert(attendance);
		return attendance.getId();
	}

	@Transactional(readOnly = true)
	public AttendanceStatisticsResponse statistics(Long clubId, LocalDateTime startTime, LocalDateTime endTime) {
		AttendanceStatsRow summaryRow = attendanceMapper.selectSummary(clubId, null, null, startTime, endTime);
		AttendanceStatisticsResponse.Summary summary = new AttendanceStatisticsResponse.Summary(
			summaryRow == null ? 0 : defaultLong(summaryRow.getTotalCount()),
			summaryRow == null ? 0 : defaultLong(summaryRow.getNormalCount()),
			summaryRow == null ? 0 : defaultLong(summaryRow.getLateCount()),
			summaryRow == null ? 0 : defaultLong(summaryRow.getLeaveEarlyCount()),
			summaryRow == null ? 0 : defaultLong(summaryRow.getAbsentCount()),
			summaryRow == null ? 0 : defaultLong(summaryRow.getExceptionCount())
		);
		List<AttendanceStatisticsResponse.DimensionStat> memberStats = mapStats(attendanceMapper.selectStatsByMember(clubId, startTime, endTime));
		List<AttendanceStatisticsResponse.DimensionStat> activityStats = mapStats(attendanceMapper.selectStatsByActivity(clubId, startTime, endTime));
		List<AttendanceStatisticsResponse.DimensionStat> dateStats = mapStats(attendanceMapper.selectStatsByDate(clubId, startTime, endTime));
		return new AttendanceStatisticsResponse(summary, memberStats, activityStats, dateStats);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<byte[]> export(Long clubId, Long activityId, Long memberId,
										 LocalDateTime startTime, LocalDateTime endTime) {
		List<Attendance> attendances = attendanceMapper.selectForExport(clubId, activityId, memberId, startTime, endTime);
		if (attendances.isEmpty()) {
			throw new BusinessException(40406, "没有可导出的考勤数据");
		}
		Map<Long, String> clubNames = attendances.stream()
			.map(Attendance::getClubId)
			.distinct()
			.collect(Collectors.toMap(
				id -> id,
				id -> clubMapper.selectById(id).map(c -> c.getClubName()).orElse("未知社团")
			));
		Map<Long, Long> memberToUser = attendances.stream()
			.map(Attendance::getMemberId)
			.distinct()
			.collect(Collectors.toMap(
				memberId1 -> memberId1,
				memberId1 -> memberMapper.selectById(memberId1).map(m -> m.getUserId()).orElse(-1L)
			));
		List<Long> userIds = memberToUser.values().stream().filter(id -> id > 0).distinct().toList();
		Map<Long, String> usernames = userIds.isEmpty() ? Map.of() :
			userMapper.selectUsernamesByIds(userIds).stream()
				.collect(Collectors.toMap(
					m -> ((Number) m.get("id")).longValue(),
					m -> (String) m.get("username"),
					(v1, v2) -> v1
				));
		StringBuilder sb = new StringBuilder();
		sb.append("社团,成员,考勤类型,考勤时间,备注,是否异常,异常状态,异常说明").append("\n");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		attendances.forEach(att -> {
			String clubName = clubNames.getOrDefault(att.getClubId(), "未知社团");
			Long userId = memberToUser.getOrDefault(att.getMemberId(), -1L);
			String username = usernames.getOrDefault(userId, "未知用户");
			sb.append(escapeCsv(clubName)).append(",")
				.append(escapeCsv(username)).append(",")
				.append(attendanceTypeText(att.getAttendanceType())).append(",")
				.append(att.getAttendanceTime() != null ? att.getAttendanceTime().format(formatter) : "")
				.append(",")
				.append(escapeCsv(att.getRemark()))
				.append(",")
				.append(Boolean.TRUE.equals(att.getExceptionFlag()) ? "是" : "否")
				.append(",")
				.append(att.getExceptionStatus() != null && att.getExceptionStatus() == 1 ? "已处理" : "待处理")
				.append(",")
				.append(escapeCsv(att.getExceptionReason()))
				.append("\n");
		});
		byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
		String filename = "attendance-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
			.contentType(MediaType.parseMediaType("text/csv"))
			.body(bytes);
	}

	@Transactional
	public void markException(Long attendanceId, String reason) {
		Attendance attendance = attendanceMapper.selectById(attendanceId);
		if (attendance == null) {
			throw new BusinessException(40406, "考勤记录不存在");
		}
		attendanceMapper.markException(attendanceId, reason);
	}

	@Transactional
	public void resolveException(Long attendanceId, String reason, Long handlerId) {
		Attendance attendance = attendanceMapper.selectById(attendanceId);
		if (attendance == null) {
			throw new BusinessException(40406, "考勤记录不存在");
		}
		attendanceMapper.resolveException(attendanceId, reason, handlerId, LocalDateTime.now());
	}

	@Transactional(readOnly = true)
	public AttendanceRule getRule(Long clubId) {
		return attendanceRuleMapper.selectByClubId(clubId)
			.orElseGet(() -> {
				AttendanceRule rule = new AttendanceRule();
				rule.setClubId(clubId);
				rule.setLateThreshold(10);
				rule.setLeaveEarlyThreshold(10);
				rule.setAbsenceThreshold(30);
				return rule;
			});
	}

	@Transactional
	public void saveRule(AttendanceRuleCommand command) {
		AttendanceRule rule = attendanceRuleMapper.selectByClubId(command.clubId())
			.orElseGet(AttendanceRule::new);
		rule.setClubId(command.clubId());
		rule.setLateThreshold(command.lateThreshold());
		rule.setLeaveEarlyThreshold(command.leaveEarlyThreshold());
		rule.setAbsenceThreshold(command.absenceThreshold());
		if (rule.getId() == null) {
			attendanceRuleMapper.insert(rule);
		} else {
			attendanceRuleMapper.update(rule);
		}
	}

	/**
	 * 为活动签到自动记录考勤
	 */
	@Transactional
	public Long recordActivityAttendance(Long clubId, Long activityId, Long memberId,
										 LocalDateTime attendanceTime, String remark) {
		if (activityId == null || memberId == null) {
			throw new BusinessException(40041, "缺少活动或成员信息，无法记录考勤");
		}
		Attendance existing = attendanceMapper.selectByActivityIdAndMemberId(activityId, memberId);
		if (existing != null) {
			return existing.getId();
		}
		return create(new AttendanceCommand(
			clubId,
			activityId,
			memberId,
			1,
			attendanceTime != null ? attendanceTime : LocalDateTime.now(),
			remark != null ? remark : "活动签到自动记录"
		));
	}

	private List<AttendanceStatisticsResponse.DimensionStat> mapStats(List<AttendanceStatsRow> rows) {
		return rows == null ? List.of() : rows.stream()
			.map(r -> new AttendanceStatisticsResponse.DimensionStat(
				r.getRefId(),
				r.getRefName(),
				defaultLong(r.getTotalCount()),
				defaultLong(r.getNormalCount()),
				defaultLong(r.getLateCount()),
				defaultLong(r.getLeaveEarlyCount()),
				defaultLong(r.getAbsentCount()),
				defaultLong(r.getExceptionCount())
			))
			.toList();
	}

	private long defaultLong(Long value) {
		return value == null ? 0 : value;
	}

	private String attendanceTypeText(Integer type) {
		return switch (type == null ? 0 : type) {
			case 1 -> "正常";
			case 2 -> "迟到";
			case 3 -> "早退";
			case 4 -> "缺勤";
			default -> "未知";
		};
	}

	private String escapeCsv(String value) {
		if (value == null) {
			return "";
		}
		String escaped = value.replace("\"", "\"\"");
		if (escaped.contains(",") || escaped.contains("\n")) {
			return "\"" + escaped + "\"";
		}
		return escaped;
	}

	private AttendanceInfo toAttendanceInfo(Attendance attendance, String clubName, String memberUsername, String activityName) {
		return new AttendanceInfo(
			attendance.getId(),
			attendance.getClubId(),
			clubName,
			attendance.getActivityId(),
			activityName,
			attendance.getMemberId(),
			memberUsername,
			attendance.getAttendanceType(),
			attendance.getAttendanceTime(),
			attendance.getRemark(),
			Boolean.TRUE.equals(attendance.getExceptionFlag()),
			attendance.getExceptionStatus(),
			attendance.getExceptionReason(),
			attendance.getHandledBy(),
			attendance.getHandledTime(),
			attendance.getCreatedTime()
		);
	}

	public record AttendanceCommand(Long clubId, Long activityId, Long memberId, Integer attendanceType,
									 LocalDateTime attendanceTime, String remark) {
	}

	public record AttendanceInfo(Long id, Long clubId, String clubName, Long activityId, String activityName, Long memberId,
								 String memberUsername, Integer attendanceType, LocalDateTime attendanceTime,
								 String remark, Boolean exceptionFlag, Integer exceptionStatus, String exceptionReason,
								 Long handledBy, LocalDateTime handledTime, LocalDateTime createdTime) {
	}

	public record AttendanceRuleCommand(Long clubId, Integer lateThreshold, Integer leaveEarlyThreshold, Integer absenceThreshold) {}
}

