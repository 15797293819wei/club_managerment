package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.dto.PageResult;
import com.example.club.mapper.UserMapper;
import com.example.club.service.ActivityService;
import com.example.club.service.EvaluationService;
import com.example.club.service.RegistrationService;
import com.example.club.service.SignInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 活动管理接口
 * <p>提供活动的发布、修改、取消、报名、签到、评价等接口。</p>
 */
@Tag(name = "活动管理", description = "活动管理相关接口，包括活动列表查询、发布、修改、取消、报名、签到、评价等功能")
@RestController
@RequestMapping("/api/activities")
@Validated
public class ActivityController {

	private final ActivityService activityService;
	private final RegistrationService registrationService;
	private final SignInService signInService;
	private final EvaluationService evaluationService;
	private final UserMapper userMapper;

	public ActivityController(ActivityService activityService, RegistrationService registrationService,
							  SignInService signInService, EvaluationService evaluationService, UserMapper userMapper) {
		this.activityService = activityService;
		this.registrationService = registrationService;
		this.signInService = signInService;
		this.evaluationService = evaluationService;
		this.userMapper = userMapper;
	}

	// ==================== 活动管理接口 ====================

	/**
	 * 分页查询活动列表
	 */
	@Operation(summary = "分页查询活动列表", description = "根据条件分页查询活动列表，支持按社团、关键词、状态筛选")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping
	public ApiResponse<PageResult<ActivityService.ActivityInfo>> page(@Valid ActivityPageRequest request) {
		boolean isSystemAdmin = isSystemAdmin();
		Long userId = getCurrentUserId();
		
		Long clubId = request.getClubId();
		List<Long> accessibleClubIds = null;
		if (!isSystemAdmin) {
			accessibleClubIds = activityService.getJoinedClubIds(userId);
			if (accessibleClubIds == null || accessibleClubIds.isEmpty()) {
				return ApiResponse.success(PageResult.empty());
			}
			if (clubId != null && !accessibleClubIds.contains(clubId)) {
				throw new com.example.club.exception.BusinessException(40303, "无权查看该社团的活动");
			}
		}
		
		PageResult<ActivityService.ActivityInfo> result = activityService.page(
			clubId,
			accessibleClubIds,
			request.getClubName(),
			request.getKeyword(),
			request.getStatus(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 查询活动详情
	 */
	@Operation(summary = "查询活动详情", description = "根据活动ID查询活动详细信息")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "活动不存在")
	})
	@GetMapping("/{id}")
	public ApiResponse<ActivityService.ActivityInfo> detail(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(activityService.getDetail(id));
	}

	/**
	 * 发布活动（需要社团管理员权限）
	 */
	@Operation(summary = "发布活动", description = "发布新活动，需要社团管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "发布成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或时间设置不合理"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的活动")
	})
	@PostMapping
	public ApiResponse<Map<String, Long>> create(@Valid @RequestBody ActivityCreateRequest request) {
		// 权限校验
		checkManagePermission(request.getClubId());
		var command = new ActivityService.ActivityCommand(
			request.getClubId(),
			request.getActivityName(),
			request.getActivityType(),
			request.getDescription(),
			request.getStartTime(),
			request.getEndTime(),
			request.getLocation(),
			request.getMaxParticipants(),
			request.getRegistrationDeadline(),
			getCurrentUserId()
		);
		Long id = activityService.create(command);
		return ApiResponse.success(Map.of("id", id));
	}

	/**
	 * 更新活动信息（需要社团管理员权限）
	 * 所有已发布的活动都可以编辑
	 */
	@Operation(summary = "更新活动信息", description = "更新活动信息，需要社团管理员权限。所有已发布的活动都可以编辑，不受状态限制。")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或时间设置不合理"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的活动"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "活动不存在")
	})
	@PutMapping("/{id}")
	public ApiResponse<Void> update(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id,
		@Valid @RequestBody ActivityUpdateRequest request) {
		// 获取活动信息以校验权限
		var activity = activityService.getDetail(id);
		checkManagePermission(activity.clubId());
		var command = new ActivityService.ActivityCommand(
			null, // clubId 不允许修改
			request.getActivityName(),
			request.getActivityType(),
			request.getDescription(),
			request.getStartTime(),
			request.getEndTime(),
			request.getLocation(),
			request.getMaxParticipants(),
			request.getRegistrationDeadline(),
			null // creatorId 不允许修改
		);
		activityService.update(id, command);
		return ApiResponse.success();
	}

	/**
	 * 取消活动（需要社团管理员权限）
	 */
	@Operation(summary = "取消活动", description = "取消活动，需要社团管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "活动已结束或已取消"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的活动"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "活动不存在")
	})
	@PatchMapping("/{id}/cancel")
	public ApiResponse<Void> cancel(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id) {
		var activity = activityService.getDetail(id);
		checkManagePermission(activity.clubId());
		activityService.cancel(id);
		return ApiResponse.success();
	}

	/**
	 * 删除活动（需要社团管理员权限）
	 * 所有已发布的活动都可以删除
	 */
	@Operation(summary = "删除活动", description = "删除活动，所有已发布的活动都可以删除，需要社团管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的活动"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "活动不存在")
	})
	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id) {
		var activity = activityService.getDetail(id);
		checkManagePermission(activity.clubId());
		activityService.delete(id);
		return ApiResponse.success();
	}

	// ==================== 报名管理接口 ====================

	/**
	 * 分页查询报名列表
	 */
	@Operation(summary = "分页查询报名列表", description = "根据条件分页查询活动报名列表，支持按活动、用户、状态筛选")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping("/registrations")
	public ApiResponse<PageResult<RegistrationService.RegistrationInfo>> pageRegistrations(@Valid RegistrationPageRequest request) {
		PageResult<RegistrationService.RegistrationInfo> result = registrationService.page(
			request.getActivityId(),
			request.getUserId(),
			request.getStatus(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 报名活动
	 */
	@Operation(summary = "报名活动", description = "报名参加活动，需要先报名才能签到")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "报名成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "活动已结束/已取消、报名已截止、已报名或人数已满")
	})
	@PostMapping("/{id}/register")
	public ApiResponse<Map<String, Long>> register(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id) {
		Long registrationId = registrationService.register(id, getCurrentUserId());
		return ApiResponse.success(Map.of("id", registrationId));
	}

	/**
	 * 取消报名
	 */
	@Operation(summary = "取消报名", description = "取消活动报名，活动已开始或已结束则无法取消")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "报名已取消或活动已开始/已结束"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "报名不存在")
	})
	@PatchMapping("/{id}/cancel-registration")
	public ApiResponse<Void> cancelRegistration(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id) {
		registrationService.cancel(id, getCurrentUserId());
		return ApiResponse.success();
	}

	// ==================== 签到管理接口 ====================

	/**
	 * 分页查询签到列表
	 */
	@Operation(summary = "分页查询签到列表", description = "根据条件分页查询活动签到列表，支持按活动、用户筛选")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping("/sign-ins")
	public ApiResponse<PageResult<SignInService.SignInInfo>> pageSignIns(@Valid SignInPageRequest request) {
		PageResult<SignInService.SignInInfo> result = signInService.page(
			request.getActivityId(),
			request.getUserId(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 签到打卡
	 */
	@Operation(summary = "签到打卡", description = "活动签到打卡，支持手动签到和二维码签到（预留位置逻辑扩展）")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "签到成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "活动未进行中、未报名或已签到")
	})
	@PostMapping("/{id}/sign-in")
	public ApiResponse<Map<String, Long>> signIn(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id,
		@Valid @RequestBody SignInRequest request) {
		Long signInId = signInService.signIn(
			id,
			getCurrentUserId(),
			request.getSignInType(),
			request.getLocation(),
			request.getRemark()
		);
		return ApiResponse.success(Map.of("id", signInId));
	}

	/**
	 * 管理员代签（需要社团管理员权限）
	 */
	@Operation(summary = "管理员代签", description = "社团管理员可为指定成员进行签到，用于补签等场景")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "签到成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "活动未进行中、未报名或已签到"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的活动"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "活动不存在")
	})
	@PostMapping("/{id}/sign-in/manager")
	public ApiResponse<Map<String, Long>> managerSignIn(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id,
		@Valid @RequestBody ManagerSignInRequest request) {
		var activity = activityService.getDetail(id);
		checkManagePermission(activity.clubId());
		Long signInId = signInService.signIn(
			id,
			request.getUserId(),
			request.getSignInType(),
			request.getLocation(),
			request.getRemark()
		);
		return ApiResponse.success(Map.of("id", signInId));
	}

	// ==================== 评价管理接口 ====================

	/**
	 * 分页查询评价列表
	 */
	@Operation(summary = "分页查询评价列表", description = "根据条件分页查询活动评价列表，支持按活动、用户筛选")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping("/evaluations")
	public ApiResponse<PageResult<EvaluationService.EvaluationInfo>> pageEvaluations(@Valid EvaluationPageRequest request) {
		PageResult<EvaluationService.EvaluationInfo> result = evaluationService.page(
			request.getActivityId(),
			request.getUserId(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 创建评价
	 */
	@Operation(summary = "创建评价", description = "对已结束的活动进行评价，评分1-5分")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "评价成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "活动未结束、未报名或已评价")
	})
	@PostMapping("/{id}/evaluate")
	public ApiResponse<Map<String, Long>> evaluate(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id,
		@Valid @RequestBody EvaluationRequest request) {
		Long evaluationId = evaluationService.create(
			id,
			getCurrentUserId(),
			request.getRating(),
			request.getComment()
		);
		return ApiResponse.success(Map.of("id", evaluationId));
	}

	/**
	 * 获取活动评分统计
	 */
	@Operation(summary = "获取活动评分统计", description = "获取活动的平均评分和评价数量")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "活动不存在")
	})
	@GetMapping("/{id}/rating-statistics")
	public ApiResponse<EvaluationService.RatingStatistics> getRatingStatistics(
		@Parameter(description = "活动ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(evaluationService.getRatingStatistics(id));
	}

	// ==================== 辅助方法 ====================

	/**
	 * 检查管理权限
	 */
	private void checkManagePermission(Long clubId) {
		boolean isSystemAdmin = isSystemAdmin();
		Long userId = getCurrentUserId();
		activityService.checkManagePermission(clubId, userId, isSystemAdmin);
	}

	/**
	 * 判断当前用户是否为系统管理员
	 */
	private boolean isSystemAdmin() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return false;
		}
		return auth.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(authority -> authority.equals("ROLE_SYSTEM_ADMIN"));
	}

	/**
	 * 获取当前登录用户ID
	 */
	private Long getCurrentUserId() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userMapper.selectByUsername(username)
			.map(user -> user.getId())
			.orElseThrow(() -> new RuntimeException("用户不存在"));
	}

	// ==================== 请求类 ====================

	@Schema(description = "活动分页查询请求")
	@Validated
	public static class ActivityPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "社团ID", example = "1")
		private Long clubId;
		@Schema(description = "社团名称（支持关键词搜索）", example = "计算机")
		private String clubName;
		@Schema(description = "关键词，支持活动名称搜索", example = "篮球")
		private String keyword;
		@Schema(description = "活动状态，0-待开始，1-进行中，2-已结束，3-已取消", example = "0")
		private Integer status;

		public int getPage() { return page; }
		public void setPage(int page) { this.page = page; }
		public int getSize() { return size; }
		public void setSize(int size) { this.size = size; }
		public Long getClubId() { return clubId; }
		public void setClubId(Long clubId) { this.clubId = clubId; }
		public String getClubName() { return clubName; }
		public void setClubName(String clubName) { this.clubName = clubName; }
		public String getKeyword() { return keyword; }
		public void setKeyword(String keyword) { this.keyword = keyword; }
		public Integer getStatus() { return status; }
		public void setStatus(Integer status) { this.status = status; }
	}

	@Schema(description = "创建活动请求")
	public static class ActivityCreateRequest {
		@Schema(description = "社团ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@Schema(description = "活动名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "篮球比赛")
		@NotBlank(message = "活动名称不能为空")
		private String activityName;
		@Schema(description = "活动类型", example = "体育")
		private String activityType;
		@Schema(description = "活动描述", example = "组织一场篮球比赛")
		private String description;
		@Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01T10:00:00")
		@NotNull(message = "开始时间不能为空")
		private LocalDateTime startTime;
		@Schema(description = "结束时间", example = "2024-01-01T12:00:00")
		private LocalDateTime endTime;
		@Schema(description = "活动地点", example = "体育馆")
		private String location;
		@Schema(description = "最大参与人数", example = "50")
		private Integer maxParticipants;
		@Schema(description = "报名截止时间", example = "2023-12-31T23:59:59")
		private LocalDateTime registrationDeadline;

		// Getters and Setters
		public Long getClubId() { return clubId; }
		public void setClubId(Long clubId) { this.clubId = clubId; }
		public String getActivityName() { return activityName; }
		public void setActivityName(String activityName) { this.activityName = activityName; }
		public String getActivityType() { return activityType; }
		public void setActivityType(String activityType) { this.activityType = activityType; }
		public String getDescription() { return description; }
		public void setDescription(String description) { this.description = description; }
		public LocalDateTime getStartTime() { return startTime; }
		public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
		public LocalDateTime getEndTime() { return endTime; }
		public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
		public String getLocation() { return location; }
		public void setLocation(String location) { this.location = location; }
		public Integer getMaxParticipants() { return maxParticipants; }
		public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
		public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
		public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }
	}

	@Schema(description = "更新活动请求")
	public static class ActivityUpdateRequest {
		@Schema(description = "活动名称", example = "篮球比赛")
		private String activityName;
		@Schema(description = "活动类型", example = "体育")
		private String activityType;
		@Schema(description = "活动描述", example = "组织一场篮球比赛")
		private String description;
		@Schema(description = "开始时间", example = "2024-01-01T10:00:00")
		private LocalDateTime startTime;
		@Schema(description = "结束时间", example = "2024-01-01T12:00:00")
		private LocalDateTime endTime;
		@Schema(description = "活动地点", example = "体育馆")
		private String location;
		@Schema(description = "最大参与人数", example = "50")
		private Integer maxParticipants;
		@Schema(description = "报名截止时间", example = "2023-12-31T23:59:59")
		private LocalDateTime registrationDeadline;

		// Getters and Setters
		public String getActivityName() { return activityName; }
		public void setActivityName(String activityName) { this.activityName = activityName; }
		public String getActivityType() { return activityType; }
		public void setActivityType(String activityType) { this.activityType = activityType; }
		public String getDescription() { return description; }
		public void setDescription(String description) { this.description = description; }
		public LocalDateTime getStartTime() { return startTime; }
		public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
		public LocalDateTime getEndTime() { return endTime; }
		public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
		public String getLocation() { return location; }
		public void setLocation(String location) { this.location = location; }
		public Integer getMaxParticipants() { return maxParticipants; }
		public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
		public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
		public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }
	}

	@Schema(description = "报名分页查询请求")
	@Validated
	public static class RegistrationPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "活动ID", example = "1")
		private Long activityId;
		@Schema(description = "用户ID", example = "1")
		private Long userId;
		@Schema(description = "报名状态，0-已取消，1-已报名", example = "1")
		private Integer status;

		public int getPage() { return page; }
		public void setPage(int page) { this.page = page; }
		public int getSize() { return size; }
		public void setSize(int size) { this.size = size; }
		public Long getActivityId() { return activityId; }
		public void setActivityId(Long activityId) { this.activityId = activityId; }
		public Long getUserId() { return userId; }
		public void setUserId(Long userId) { this.userId = userId; }
		public Integer getStatus() { return status; }
		public void setStatus(Integer status) { this.status = status; }
	}

	@Schema(description = "签到请求")
	public static class SignInRequest {
		@Schema(description = "签到方式，1-手动签到，2-二维码签到", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "签到方式不能为空")
		@Min(value = 1, message = "签到方式值非法")
		@Max(value = 2, message = "签到方式值非法")
		private Integer signInType;
		@Schema(description = "签到地点（二维码签到时可选）", example = "体育馆")
		private String location;
		@Schema(description = "备注", example = "准时到达")
		private String remark;

		public Integer getSignInType() { return signInType; }
		public void setSignInType(Integer signInType) { this.signInType = signInType; }
		public String getLocation() { return location; }
		public void setLocation(String location) { this.location = location; }
		public String getRemark() { return remark; }
		public void setRemark(String remark) { this.remark = remark; }
	}

	@Schema(description = "管理员代签请求")
	public static class ManagerSignInRequest {
		@Schema(description = "签到用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "用户ID不能为空")
		private Long userId;
		@Schema(description = "签到方式，1-手动签到，2-二维码签到", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "签到方式不能为空")
		@Min(value = 1, message = "签到方式值非法")
		@Max(value = 2, message = "签到方式值非法")
		private Integer signInType;
		@Schema(description = "签到地点（二维码签到时可选）", example = "体育馆")
		private String location;
		@Schema(description = "备注", example = "管理员代签")
		private String remark;

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Integer getSignInType() {
			return signInType;
		}

		public void setSignInType(Integer signInType) {
			this.signInType = signInType;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public String getRemark() {
			return remark;
		}

		public void setRemark(String remark) {
			this.remark = remark;
		}
	}

	@Schema(description = "签到分页查询请求")
	@Validated
	public static class SignInPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "活动ID", example = "1")
		private Long activityId;
		@Schema(description = "用户ID", example = "1")
		private Long userId;

		public int getPage() { return page; }
		public void setPage(int page) { this.page = page; }
		public int getSize() { return size; }
		public void setSize(int size) { this.size = size; }
		public Long getActivityId() { return activityId; }
		public void setActivityId(Long activityId) { this.activityId = activityId; }
		public Long getUserId() { return userId; }
		public void setUserId(Long userId) { this.userId = userId; }
	}

	@Schema(description = "评价请求")
	public static class EvaluationRequest {
		@Schema(description = "评分，1-5分", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
		@NotNull(message = "评分不能为空")
		@Min(value = 1, message = "评分必须在1-5分之间")
		@Max(value = 5, message = "评分必须在1-5分之间")
		private Integer rating;
		@Schema(description = "评价内容", example = "活动很棒，收获很多")
		private String comment;

		public Integer getRating() { return rating; }
		public void setRating(Integer rating) { this.rating = rating; }
		public String getComment() { return comment; }
		public void setComment(String comment) { this.comment = comment; }
	}

	@Schema(description = "评价分页查询请求")
	@Validated
	public static class EvaluationPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "活动ID", example = "1")
		private Long activityId;
		@Schema(description = "用户ID", example = "1")
		private Long userId;

		public int getPage() { return page; }
		public void setPage(int page) { this.page = page; }
		public int getSize() { return size; }
		public void setSize(int size) { this.size = size; }
		public Long getActivityId() { return activityId; }
		public void setActivityId(Long activityId) { this.activityId = activityId; }
		public Long getUserId() { return userId; }
		public void setUserId(Long userId) { this.userId = userId; }
	}
}

