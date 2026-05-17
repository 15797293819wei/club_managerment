package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.domain.Member;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.MemberApplicationMapper;
import com.example.club.mapper.MemberMapper;
import com.example.club.mapper.UserMapper;
import com.example.club.mapper.ClubMapper;
import com.example.club.service.ClubOwnershipService;
import com.example.club.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

/**
 * 成员管理接口
 * <p>提供成员的分页查询、详情、添加、角色分配、状态变更等接口。</p>
 */
@Tag(name = "成员管理", description = "成员管理相关接口，包括成员列表查询、添加、角色分配、状态变更等功能")
@RestController
@RequestMapping("/api/members")
@Validated
public class MemberController {

	private final MemberService memberService;
	private final UserMapper userMapper;
	private final MemberMapper memberMapper;
	private final MemberApplicationMapper memberApplicationMapper;
	private final ClubMapper clubMapper;
	private final ClubOwnershipService clubOwnershipService;

	public MemberController(MemberService memberService,
							UserMapper userMapper,
							MemberMapper memberMapper,
							MemberApplicationMapper memberApplicationMapper,
							ClubMapper clubMapper,
							ClubOwnershipService clubOwnershipService) {
		this.memberService = memberService;
		this.userMapper = userMapper;
		this.memberMapper = memberMapper;
		this.memberApplicationMapper = memberApplicationMapper;
		this.clubMapper = clubMapper;
		this.clubOwnershipService = clubOwnershipService;
	}

	/**
	 * 获取社团成员统计
	 */
	@Operation(summary = "获取成员统计概览", description = "查看指定社团的成员数量、活跃度以及参与排行榜")
	@GetMapping("/club/{clubId}/stats")
	public ApiResponse<MemberService.ClubMemberStats> stats(
		@Parameter(description = "社团ID", required = true) @PathVariable Long clubId,
		@Parameter(description = "统计区间（天）", example = "30") @RequestParam(value = "recentDays", defaultValue = "30") int recentDays) {
		checkStatisticsPermission(clubId);
		return ApiResponse.success(memberService.getClubMemberStats(clubId, recentDays));
	}

	/**
	 * 分页查询成员列表
	 */
	@Operation(summary = "分页查询成员列表", description = "根据条件分页查询成员列表，支持按社团、用户、角色、状态筛选")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
	})
	@GetMapping
	public ApiResponse<PageResult<MemberService.MemberInfo>> page(@Valid MemberPageRequest request) {
		// 普通成员也可以查看本社团的成员列表，但只能查看，不能操作
		Long clubId = request.getClubId();
		if (clubId == null) {
			throw new BusinessException(40001, "必须指定社团ID");
		}
		ensureViewPermission(clubId);
		// 成员管理列表只展示“正常”状态的成员，已退出成员不再出现在列表中
		PageResult<MemberService.MemberInfo> result = memberService.page(
			request.getClubId(),
			request.getUserId(),
			request.getRole(),
			1,
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 查询当前用户在指定社团的成员状态（是否成员、是否有待审核申请）
	 */
	@Operation(summary = "查询个人社团状态", description = "返回当前用户在指定社团是否已加入、成员ID以及是否存在待审核申请")
	@GetMapping("/self/state")
	public ApiResponse<MemberSelfState> selfState(
		@Parameter(description = "社团ID", required = true) @RequestParam Long clubId) {
		if (clubId == null) {
			throw new BusinessException(40001, "必须指定社团ID");
		}
		Long userId = getCurrentUserId();
		var club = clubMapper.selectById(clubId)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		Optional<Member> memberOpt = memberMapper.selectByClubIdAndUserId(clubId, userId);
		boolean joined = memberOpt.filter(m -> m.getStatus() != null && m.getStatus() == 1).isPresent();
		Long memberId = joined ? memberOpt.get().getId() : null;
		boolean pending = memberApplicationMapper
			.selectPendingByClubIdAndApplicantId(clubId, userId)
			.isPresent();
		String role = joined ? memberOpt.get().getRole() : null;
		if (Objects.equals(club.getPresidentId(), userId)) {
			role = "PRESIDENT";
		}
		return ApiResponse.success(new MemberSelfState(joined, memberId, role, pending));
	}

	/**
	 * 查询成员详情
	 */
	@Operation(summary = "查询成员详情", description = "根据成员ID查询成员详细信息")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "成员不存在")
	})
	@GetMapping("/{id}")
	public ApiResponse<MemberService.MemberInfo> detail(
		@Parameter(description = "成员ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(memberService.getDetail(id));
	}

	/**
	 * 查询成员概览，包含基础信息和参与统计
	 */
	@Operation(summary = "查询成员概览", description = "获取成员的基础信息、用户档案和参与统计")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "成员不存在")
	})
	@GetMapping("/{id}/overview")
	public ApiResponse<MemberService.MemberDetail> overview(
		@Parameter(description = "成员ID", required = true) @PathVariable Long id) {
		MemberService.MemberInfo member = memberService.getDetail(id);
		ensureViewPermission(member.clubId());
		return ApiResponse.success(memberService.getMemberDetailOverview(id));
	}

	/**
	 * 添加成员（需要社团管理员权限）
	 */
	@Operation(summary = "添加成员", description = "向指定社团添加新成员，需要社团管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "添加成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或该用户已是该社团成员"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的成员")
	})
	@PostMapping
	public ApiResponse<Map<String, Long>> add(
		@Parameter(description = "添加成员请求", required = true) @Valid @RequestBody MemberAddRequest request) {
		checkManagePermission(request.getClubId());
		var command = new MemberService.MemberCommand(
			request.getClubId(),
			request.getUserId(),
			request.getRole()
		);
		Long id = memberService.add(command);
		return ApiResponse.success(Map.of("id", id));
	}

	/**
	 * 更新成员角色（需要社团管理员权限）
	 */
	@Operation(summary = "更新成员角色", description = "更新成员在社团中的角色，可选值：MEMBER（普通成员）、STAFF（干事）、VICE_MINISTER（副部长）、MINISTER（部长）、PRESIDENT（社长）")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "角色值非法"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的成员"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "成员不存在")
	})
	@PutMapping("/{id}/role")
	public ApiResponse<Void> updateRole(
		@Parameter(description = "成员ID", required = true) @PathVariable Long id,
		@Parameter(description = "角色更新请求", required = true) @Valid @RequestBody MemberRoleRequest request) {
		// 先获取成员信息以获取clubId
		MemberService.MemberInfo member = memberService.getDetail(id);
		checkManagePermission(member.clubId());
		checkRoleAssignPermission(member.clubId());

		String role = request.getRole();
		if ("PRESIDENT".equalsIgnoreCase(role)) {
			// 将该成员设为社长，同时完成社团管理员转交逻辑
			Long operatorId = getCurrentUserId();
			boolean isSystemAdmin = isSystemAdmin();
			clubOwnershipService.transferAdmin(member.clubId(), member.userId(), operatorId, isSystemAdmin);
		} else {
			memberService.updateRole(id, role);
		}
		return ApiResponse.success();
	}

	/**
	 * 批量调整成员角色
	 */
	@Operation(summary = "批量分配角色", description = "一次性为多个成员设置新角色")
	@PostMapping("/batch/role")
	public ApiResponse<Void> batchUpdateRole(@Valid @RequestBody MemberBatchRoleRequest request) {
		checkManagePermission(request.getClubId());
		checkRoleAssignPermission(request.getClubId());
		memberService.batchUpdateRole(request.getClubId(), request.getMemberIds(), request.getRole());
		return ApiResponse.success();
	}

	/**
	 * 修改成员状态（需要社团管理员权限）
	 */
	@Operation(summary = "修改成员状态", description = "修改成员状态，0-已退出，1-正常")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "修改成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "状态值非法"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的成员"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "成员不存在")
	})
	@PatchMapping("/{id}/status")
	public ApiResponse<Void> changeStatus(
		@Parameter(description = "成员ID", required = true) @PathVariable Long id,
		@Parameter(description = "状态更新请求", required = true) @Valid @RequestBody MemberStatusRequest request) {
		// 先获取成员信息以获取clubId
		MemberService.MemberInfo member = memberService.getDetail(id);
		checkManagePermission(member.clubId());
		memberService.changeStatus(id, request.getStatus());
		return ApiResponse.success();
	}

	/**
	 * 删除成员（需要社团管理员权限）
	 */
	@Operation(summary = "删除成员", description = "从社团中删除成员，需要社团管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的成员"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "成员不存在")
	})
	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(
		@Parameter(description = "成员ID", required = true) @PathVariable Long id) {
		// 先获取成员信息以获取clubId
		MemberService.MemberInfo member = memberService.getDetail(id);
		checkManagePermission(member.clubId());
		memberService.delete(id);
		return ApiResponse.success();
	}

	/**
	 * 批量移除成员
	 */
	@Operation(summary = "批量移除成员", description = "一次性移除多个成员，需要社团管理员权限")
	@PostMapping("/batch/remove")
	public ApiResponse<Void> batchRemove(@Valid @RequestBody MemberBatchRemoveRequest request) {
		checkManagePermission(request.getClubId());
		memberService.batchRemoveMembers(request.getClubId(), request.getMemberIds());
		return ApiResponse.success();
	}

	/**
	 * 成员自己退出社团
	 */
	@Operation(summary = "退出社团", description = "成员自己退出社团")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "退出成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "只能退出自己所在的社团"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "成员不存在")
	})
	@PostMapping("/{id}/quit")
	public ApiResponse<Void> quit(
		@Parameter(description = "成员ID", required = true) @PathVariable Long id) {
		Long userId = getCurrentUserId();
		MemberService.MemberInfo member = memberService.getDetail(id);
		// 只能退出自己的成员身份
		if (!member.userId().equals(userId)) {
			throw new BusinessException(40301, "只能退出自己所在的社团");
		}
		memberService.quit(id);
		return ApiResponse.success();
	}

	/**
	 * 导出成员列表
	 */
	@Operation(summary = "导出成员列表", description = "导出指定社团成员为CSV文件")
	@GetMapping("/export")
	public ResponseEntity<byte[]> export(@Valid MemberExportRequest request) {
		checkManagePermission(request.getClubId());
		byte[] data = memberService.exportMembers(request.getClubId(), request.getRole(), request.getStatus());
		String filename = URLEncoder.encode("club-members-" + request.getClubId() + ".csv", StandardCharsets.UTF_8);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
			.contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
			.body(data);
	}

	/**
	 * 检查管理权限
	 * 只有社团管理员、部长、副部长可以管理成员，系统管理员不能管理成员
	 */
	private void checkManagePermission(Long clubId) {
		Long userId = getCurrentUserId();
		memberService.checkManagePermission(clubId, userId, false);
	}

	private void checkRoleAssignPermission(Long clubId) {
		Long userId = getCurrentUserId();
		memberService.checkRoleAssignmentPermission(clubId, userId, isSystemAdmin());
	}

	private void checkStatisticsPermission(Long clubId) {
		Long userId = getCurrentUserId();
		memberService.checkStatisticsPermission(clubId, userId, isSystemAdmin());
	}

	private void ensureViewPermission(Long clubId) {
		if (clubId == null) {
			throw new BusinessException(40001, "必须指定社团ID");
		}
		if (isSystemAdmin()) {
			return;
		}
		Long userId = getCurrentUserId();
		Optional<Member> member = memberMapper.selectByClubIdAndUserId(clubId, userId);
		if (member.isPresent() && member.get().getStatus() == 1) {
			return;
		}
		try {
			checkManagePermission(clubId);
		} catch (BusinessException e) {
			throw new BusinessException(40302, "无权查看该社团的成员列表");
		}
	}

	/**
	 * 判断当前用户是否为系统管理员
	 */
	private boolean isSystemAdmin() {
		org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return false;
		}
		return auth.getAuthorities().stream()
			.map(authority -> authority.getAuthority())
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

	@Schema(description = "成员分页查询请求")
	@Validated
	public static class MemberPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "社团ID，可选", example = "1")
		private Long clubId;
		@Schema(description = "用户ID，可选", example = "1")
		private Long userId;
		@Schema(description = "成员角色，可选值：MEMBER、STAFF、VICE_MINISTER、MINISTER、PRESIDENT", example = "MEMBER")
		private String role;
		@Schema(description = "成员状态，0-已退出，1-正常", example = "1")
		private Integer status;

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

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}

	@Schema(description = "添加成员请求")
	public static class MemberAddRequest {
		@Schema(description = "社团ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "用户ID不能为空")
		private Long userId;
		@Schema(description = "成员角色，可选值：MEMBER、STAFF、VICE_MINISTER、MINISTER，默认为MEMBER", example = "MEMBER")
		private String role;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}
	}

	@Schema(description = "成员角色更新请求")
	public static class MemberRoleRequest {
		@Schema(description = "成员角色，可选值：MEMBER、STAFF、VICE_MINISTER、MINISTER、PRESIDENT", requiredMode = Schema.RequiredMode.REQUIRED, example = "STAFF")
		@NotBlank(message = "角色不能为空")
		private String role;

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}
	}

	@Schema(description = "成员状态更新请求")
	public static class MemberStatusRequest {
		@Schema(description = "成员状态，0-已退出，1-正常", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@Min(value = 0, message = "状态取值非法")
		@Max(value = 1, message = "状态取值非法")
		private Integer status;

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}

	@Schema(description = "批量角色分配请求")
	public static class MemberBatchRoleRequest {
		@Schema(description = "社团ID", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@Schema(description = "成员ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotEmpty(message = "成员ID列表不能为空")
		private List<Long> memberIds;
		@Schema(description = "目标角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "STAFF")
		@NotBlank(message = "角色不能为空")
		private String role;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public List<Long> getMemberIds() {
			return memberIds;
		}

		public void setMemberIds(List<Long> memberIds) {
			this.memberIds = memberIds;
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}
	}

	@Schema(description = "批量移除成员请求")
	public static class MemberBatchRemoveRequest {
		@Schema(description = "社团ID", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@Schema(description = "成员ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotEmpty(message = "成员ID列表不能为空")
		private List<Long> memberIds;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public List<Long> getMemberIds() {
			return memberIds;
		}

		public void setMemberIds(List<Long> memberIds) {
			this.memberIds = memberIds;
		}
	}

	@Schema(description = "成员导出请求")
	public static class MemberExportRequest {
		@Schema(description = "社团ID", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@Schema(description = "按角色筛选", example = "STAFF")
		private String role;
		@Schema(description = "按状态筛选，1=正常，0=已退出", example = "1")
		private Integer status;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}

	public record MemberSelfState(boolean joined, Long memberId, String role, boolean pendingApplication) {
	}
}

