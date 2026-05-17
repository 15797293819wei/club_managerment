package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.domain.AttendanceRule;
import com.example.club.dto.PageResult;
import com.example.club.dto.attendance.AttendanceStatisticsResponse;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.UserMapper;
import com.example.club.service.AttendanceService;
import com.example.club.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 考勤管理接口
 * <p>提供考勤记录的分页查询、详情、创建等接口。</p>
 */
@Tag(name = "考勤管理", description = "考勤记录相关接口，包括考勤记录查询、创建等功能")
@RestController
@RequestMapping("/api/attendances")
@Validated
public class AttendanceController {

	private final AttendanceService attendanceService;
	private final MemberService memberService;
	private final UserMapper userMapper;

	public AttendanceController(AttendanceService attendanceService, MemberService memberService, UserMapper userMapper) {
		this.attendanceService = attendanceService;
		this.memberService = memberService;
		this.userMapper = userMapper;
	}

	/**
	 * 分页查询考勤记录列表
	 * 只有社团管理员、部长、副部长可以查看本社团考勤记录，系统管理员可以查看所有社团考勤记录
	 */
	@Operation(summary = "分页查询考勤记录列表", description = "根据条件分页查询考勤记录列表，支持按社团、活动、成员筛选，需要社团管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权查看考勤记录")
	})
	@GetMapping
	public ApiResponse<PageResult<AttendanceService.AttendanceInfo>> page(@Valid AttendancePageRequest request) {
		// 必须指定社团ID，且用户必须是该社团的管理员、部长或副部长
		if (request.getClubId() == null) {
			throw new BusinessException(40001, "必须指定社团ID");
		}
		// 系统管理员可以查看任意社团的考勤记录；普通用户需要具备该社团管理权限
		if (!isSystemAdmin()) {
			checkManagePermission(request.getClubId());
		}
		PageResult<AttendanceService.AttendanceInfo> result = attendanceService.page(
			request.getClubId(),
			request.getActivityId(),
			request.getMemberId(),
			request.getStartTime(),
			request.getEndTime(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 查询考勤记录详情
	 */
	@Operation(summary = "查询考勤记录详情", description = "根据考勤ID查询考勤记录详细信息")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "考勤记录不存在")
	})
	@GetMapping("/{id}")
	public ApiResponse<AttendanceService.AttendanceInfo> detail(
		@Parameter(description = "考勤ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(attendanceService.getDetail(id));
	}

	/**
	 * 创建考勤记录（需要社团管理员权限）
	 * 只有社团管理员、部长、副部长可以创建考勤记录，系统管理员不能创建
	 */
	@Operation(summary = "创建考勤记录", description = "创建成员考勤记录，考勤类型：1-正常，2-迟到，3-早退，4-缺勤，需要社团管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或考勤类型值非法或成员状态异常"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权创建考勤记录"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "社团或成员不存在")
	})
	@PostMapping
	public ApiResponse<Map<String, Long>> create(
		@Parameter(description = "创建考勤记录请求", required = true) @Valid @RequestBody AttendanceCreateRequest request) {
		checkManagePermission(request.getClubId());
		var command = new AttendanceService.AttendanceCommand(
			request.getClubId(),
			request.getActivityId(),
			request.getMemberId(),
			request.getAttendanceType(),
			request.getAttendanceTime(),
			request.getRemark()
		);
		Long id = attendanceService.create(command);
		return ApiResponse.success(Map.of("id", id));
	}

	@Operation(summary = "导出考勤记录", description = "导出符合条件的考勤记录 CSV 文件")
	@GetMapping("/export")
	public ResponseEntity<byte[]> export(@Valid AttendanceExportRequest request) {
		if (request.getClubId() == null) {
			throw new BusinessException(40001, "必须指定社团ID");
		}
		// 系统管理员可以导出任意社团的考勤记录；普通用户需要具备该社团管理权限
		if (!isSystemAdmin()) {
			checkManagePermission(request.getClubId());
		}
		return attendanceService.export(
			request.getClubId(),
			request.getActivityId(),
			request.getMemberId(),
			request.getStartTime(),
			request.getEndTime()
		);
	}

	@Operation(summary = "获取考勤统计", description = "按成员/活动/日期维度查看考勤统计报表")
	@GetMapping("/stats")
	public ApiResponse<AttendanceStatisticsResponse> stats(@Valid AttendanceStatsRequest request) {
		if (request.getClubId() == null) {
			throw new BusinessException(40001, "必须指定社团ID");
		}
		// 系统管理员可以查看任意社团的考勤统计；普通用户需要具备该社团的管理权限
		if (!isSystemAdmin()) {
			checkManagePermission(request.getClubId());
		}
		AttendanceStatisticsResponse response = attendanceService.statistics(
			request.getClubId(),
			request.getStartTime(),
			request.getEndTime()
		);
		return ApiResponse.success(response);
	}

	@Operation(summary = "标记考勤异常", description = "社团管理员可对考勤记录标记异常，补充原因")
	@PostMapping("/{id}/exception")
	public ApiResponse<Void> markException(
		@Parameter(description = "考勤ID", required = true) @PathVariable Long id,
		@Valid @RequestBody AttendanceExceptionRequest request) {
		var attendance = attendanceService.getDetail(id);
		checkManagePermission(attendance.clubId());
		attendanceService.markException(id, request.getReason());
		return ApiResponse.success();
	}

	@Operation(summary = "处理考勤异常", description = "社团管理员处理并记录异常说明")
	@PostMapping("/{id}/exception/resolve")
	public ApiResponse<Void> resolveException(
		@Parameter(description = "考勤ID", required = true) @PathVariable Long id,
		@Valid @RequestBody AttendanceExceptionRequest request) {
		var attendance = attendanceService.getDetail(id);
		checkManagePermission(attendance.clubId());
		attendanceService.resolveException(id, request.getReason(), getCurrentUserId());
		return ApiResponse.success();
	}

	@Operation(summary = "获取考勤规则", description = "查询社团的迟到/早退/缺勤阈值配置")
	@GetMapping("/rules/{clubId}")
	public ApiResponse<AttendanceRule> getRule(@PathVariable Long clubId) {
		checkManagePermission(clubId);
		return ApiResponse.success(attendanceService.getRule(clubId));
	}

	@Operation(summary = "保存考勤规则", description = "保存社团的考勤阈值配置")
	@PostMapping("/rules")
	public ApiResponse<Void> saveRule(@Valid @RequestBody AttendanceRuleRequest request) {
		checkManagePermission(request.getClubId());
		attendanceService.saveRule(new AttendanceService.AttendanceRuleCommand(
			request.getClubId(),
			request.getLateThreshold(),
			request.getLeaveEarlyThreshold(),
			request.getAbsenceThreshold()
		));
		return ApiResponse.success();
	}

	/**
	 * 检查管理权限
	 * 只有社团管理员、部长、副部长可以管理考勤，系统管理员不能管理考勤
	 * <p>注意：系统管理员无法创建/修改考勤记录，但在查询统计时可以查看任意社团的数据。</p>
	 */
	private void checkManagePermission(Long clubId) {
		Long userId = getCurrentUserId();
		memberService.checkManagePermission(clubId, userId, false);
	}

	private boolean isSystemAdmin() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return false;
		}
		return auth.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(authority -> "ROLE_SYSTEM_ADMIN".equals(authority));
	}

	/**
	 * 获取当前登录用户ID
	 */
	private Long getCurrentUserId() {
		String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
			.getAuthentication().getName();
		return userMapper.selectByUsername(username)
			.map(user -> user.getId())
			.orElseThrow(() -> new RuntimeException("用户不存在"));
	}

	@Schema(description = "考勤记录分页查询请求")
	@Validated
	public static class AttendancePageRequest {
		@Schema(description = "开始时间（含）", example = "2024-01-01T00:00:00")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime startTime;
		@Schema(description = "结束时间（含）", example = "2024-01-31T23:59:59")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime endTime;
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "社团ID，可选", example = "1")
		private Long clubId;
		@Schema(description = "活动ID，可选", example = "1")
		private Long activityId;
		@Schema(description = "成员ID，可选", example = "1")
		private Long memberId;

		public int getPage() {
			return page;
		}

		public void setPage(int page) {
			this.page = page;
		}

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public Long getActivityId() {
			return activityId;
		}

		public void setActivityId(Long activityId) {
			this.activityId = activityId;
		}

		public Long getMemberId() {
			return memberId;
		}

		public void setMemberId(Long memberId) {
			this.memberId = memberId;
		}

		public LocalDateTime getStartTime() {
			return startTime;
		}

		public void setStartTime(LocalDateTime startTime) {
			this.startTime = startTime;
		}

		public LocalDateTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalDateTime endTime) {
			this.endTime = endTime;
		}
	}

	@Schema(description = "创建考勤记录请求")
	public static class AttendanceCreateRequest {
		@Schema(description = "社团ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@Schema(description = "活动ID，可选（为空表示日常考勤）", example = "1")
		private Long activityId;
		@Schema(description = "成员ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "成员ID不能为空")
		private Long memberId;
		@Schema(description = "考勤类型，1-正常，2-迟到，3-早退，4-缺勤", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "考勤类型不能为空")
		@Min(value = 1, message = "考勤类型取值非法")
		@Max(value = 4, message = "考勤类型取值非法")
		private Integer attendanceType;
		@Schema(description = "考勤时间，可选（为空则使用当前时间）", example = "2024-01-01T10:00:00")
		private LocalDateTime attendanceTime;
		@Schema(description = "备注", example = "正常出勤")
		private String remark;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public Long getActivityId() {
			return activityId;
		}

		public void setActivityId(Long activityId) {
			this.activityId = activityId;
		}

		public Long getMemberId() {
			return memberId;
		}

		public void setMemberId(Long memberId) {
			this.memberId = memberId;
		}

		public Integer getAttendanceType() {
			return attendanceType;
		}

		public void setAttendanceType(Integer attendanceType) {
			this.attendanceType = attendanceType;
		}

		public LocalDateTime getAttendanceTime() {
			return attendanceTime;
		}

		public void setAttendanceTime(LocalDateTime attendanceTime) {
			this.attendanceTime = attendanceTime;
		}

		public String getRemark() {
			return remark;
		}

		public void setRemark(String remark) {
			this.remark = remark;
		}
	}

	@Schema(description = "考勤导出请求")
	public static class AttendanceExportRequest {
		@Schema(description = "社团ID", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		private Long activityId;
		private Long memberId;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime startTime;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime endTime;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public Long getActivityId() {
			return activityId;
		}

		public void setActivityId(Long activityId) {
			this.activityId = activityId;
		}

		public Long getMemberId() {
			return memberId;
		}

		public void setMemberId(Long memberId) {
			this.memberId = memberId;
		}

		public LocalDateTime getStartTime() {
			return startTime;
		}

		public void setStartTime(LocalDateTime startTime) {
			this.startTime = startTime;
		}

		public LocalDateTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalDateTime endTime) {
			this.endTime = endTime;
		}
	}

	@Schema(description = "考勤统计请求")
	public static class AttendanceStatsRequest {
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime startTime;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime endTime;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public LocalDateTime getStartTime() {
			return startTime;
		}

		public void setStartTime(LocalDateTime startTime) {
			this.startTime = startTime;
		}

		public LocalDateTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalDateTime endTime) {
			this.endTime = endTime;
		}
	}

	@Schema(description = "考勤异常请求")
	public static class AttendanceExceptionRequest {
		@NotBlank(message = "原因不能为空")
		private String reason;

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}
	}

	@Schema(description = "考勤规则配置请求")
	public static class AttendanceRuleRequest {
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@NotNull(message = "迟到阈值不能为空")
		@Min(value = 1, message = "迟到阈值必须大于0")
		private Integer lateThreshold;
		@NotNull(message = "早退阈值不能为空")
		@Min(value = 1, message = "早退阈值必须大于0")
		private Integer leaveEarlyThreshold;
		@NotNull(message = "缺勤阈值不能为空")
		@Min(value = 1, message = "缺勤阈值必须大于0")
		private Integer absenceThreshold;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public Integer getLateThreshold() {
			return lateThreshold;
		}

		public void setLateThreshold(Integer lateThreshold) {
			this.lateThreshold = lateThreshold;
		}

		public Integer getLeaveEarlyThreshold() {
			return leaveEarlyThreshold;
		}

		public void setLeaveEarlyThreshold(Integer leaveEarlyThreshold) {
			this.leaveEarlyThreshold = leaveEarlyThreshold;
		}

		public Integer getAbsenceThreshold() {
			return absenceThreshold;
		}

		public void setAbsenceThreshold(Integer absenceThreshold) {
			this.absenceThreshold = absenceThreshold;
		}
	}
}

