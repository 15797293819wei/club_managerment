package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.dto.PageResult;
import com.example.club.mapper.UserMapper;
import com.example.club.service.ClubOwnershipService;
import com.example.club.service.ClubDissolutionService;
import com.example.club.service.ClubService;
import com.example.club.service.MemberService;
import com.example.club.service.UserService;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 社团管理接口
 * <p>提供社团的分页查询、详情、创建、更新、状态变更等接口。</p>
 */
@Tag(name = "社团管理", description = "社团管理相关接口，包括社团列表查询、详情、创建、更新、状态变更等功能")
@RestController
@RequestMapping("/api/clubs")
@Validated
public class ClubController {

	private final ClubService clubService;
	private final UserMapper userMapper;
	private final MemberService memberService;
	private final UserService userService;
	private final ClubOwnershipService clubOwnershipService;
	private final ClubDissolutionService clubDissolutionService;

	public ClubController(ClubService clubService,
						  UserMapper userMapper,
						  MemberService memberService,
						  UserService userService,
						  ClubOwnershipService clubOwnershipService,
						  ClubDissolutionService clubDissolutionService) {
		this.clubService = clubService;
		this.userMapper = userMapper;
		this.memberService = memberService;
		this.userService = userService;
		this.clubOwnershipService = clubOwnershipService;
		this.clubDissolutionService = clubDissolutionService;
	}

	/**
	 * 分页查询社团列表
	 */
	@Operation(summary = "分页查询社团列表", description = "根据条件分页查询社团列表，支持按关键词、状态筛选，非系统管理员只能查看自己创建的社团")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping
	public ApiResponse<PageResult<ClubService.ClubInfo>> page(@Valid ClubPageRequest request) {
		// 非系统管理员只能查看自己创建的社团
		Long founderId = isSystemAdmin() ? null : getCurrentUserId();
		PageResult<ClubService.ClubInfo> result = clubService.page(
			request.getKeyword(),
			request.getStatus(),
			founderId,
			request.getMinMembers(),
			request.getMaxMembers(),
			request.getSortBy(),
			request.getSortOrder(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 获取当前用户管理的社团列表
	 * 包括：作为创始人、社长、部长、副部长的社团
	 */
	@Operation(summary = "获取当前用户管理的社团列表", description = "返回当前用户有管理权限的社团列表（创始人、社长、部长、副部长）")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping("/managed")
	public ApiResponse<List<ClubService.ClubInfo>> getManagedClubs() {
		boolean isSystemAdmin = isSystemAdmin();
		Long userId = getCurrentUserId();
		
		if (isSystemAdmin) {
			// 系统管理员返回所有已通过的社团
			PageResult<ClubService.ClubInfo> result = clubService.page(null, 1, null, 1, 1000);
			return ApiResponse.success(result.getRecords());
		}
		
		// 社团管理员：获取作为创始人、社长、部长、副部长的社团
		List<ClubService.ClubInfo> managedClubs = clubService.getManagedClubs(userId);
		return ApiResponse.success(managedClubs);
	}

	/**
	 * 获取当前用户加入的社团列表（包括角色信息）
	 * 包括：作为成员、干事、副部长、部长的所有社团
	 */
	@Operation(summary = "获取当前用户加入的社团列表", description = "返回当前用户加入的所有社团列表（包括角色信息）")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping("/joined")
	public ApiResponse<List<ClubService.JoinedClubInfo>> getJoinedClubs() {
		Long userId = getCurrentUserId();
		List<ClubService.JoinedClubInfo> joinedClubs = clubService.getJoinedClubs(userId);
		return ApiResponse.success(joinedClubs);
	}

	/**
	 * 面向学生的社团广场查询
	 */
	@Operation(summary = "社团广场列表", description = "查询所有已通过审核的社团列表，支持关键词搜索")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping("/public")
	public ApiResponse<PageResult<ClubService.ClubInfo>> publicPage(@Valid PublicClubPageRequest request) {
		PageResult<ClubService.ClubInfo> result = clubService.pageForPublic(
			request.getKeyword(),
			request.getMinMembers(),
			request.getMaxMembers(),
			request.getSortBy(),
			request.getSortOrder(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 查询社团详情
	 */
	@Operation(summary = "查询社团详情", description = "根据社团ID查询社团详细信息")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "社团不存在")
	})
	@GetMapping("/{id}")
	public ApiResponse<ClubService.ClubInfo> detail(
		@Parameter(description = "社团ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(clubService.getDetail(id));
	}

	/**
	 * 创建社团（需要系统管理员权限或通过申请流程）
	 */
	@Operation(summary = "创建社团", description = "创建新社团，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或社团名称/代码已存在"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限")
	})
	@PostMapping
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Map<String, Long>> create(
		@Parameter(description = "创建社团请求", required = true) @Valid @RequestBody ClubCreateRequest request) {
		Long operatorId = getCurrentUserId();
		Long founderId = request.getFounderId() != null ? request.getFounderId() : operatorId;
		var command = new ClubService.ClubCommand(
			request.getClubName(),
			request.getClubCode(),
			request.getDescription(),
			request.getPurpose(),
			request.getConstitution(),
			request.getLogo(),
			request.getContactPerson(),
			request.getContactPhone(),
			request.getContactEmail(),
			founderId,
			request.getPresidentId() != null ? request.getPresidentId() : founderId,
			null, // memberCount
			request.getStatus(),
			request.getEstablishedTime()
		);
		Long id = clubService.create(command);

		// 保障创始人具备社团管理员身份及基础成员角色
		memberService.add(new MemberService.MemberCommand(id, founderId, "MINISTER"));
		userService.grantRoleIfAbsent(founderId, UserService.ROLE_CODE_CLUB_ADMIN);
		return ApiResponse.success(Map.of("id", id));
	}

	/**
	 * 更新社团信息（需要系统管理员权限或社团创始人权限）
	 */
	@Operation(summary = "更新社团信息", description = "更新社团信息，需要系统管理员权限或社团创始人权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或社团名称/代码已存在"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "社团不存在")
	})
	@PutMapping("/{id}")
	public ApiResponse<Void> update(
		@Parameter(description = "社团ID", required = true) @PathVariable Long id,
		@Parameter(description = "更新社团请求", required = true) @Valid @RequestBody ClubUpdateRequest request) {
		// 权限校验
		checkManagePermission(id);
		var command = new ClubService.ClubCommand(
			request.getClubName(),
			request.getClubCode(),
			request.getDescription(),
			request.getPurpose(),
			request.getConstitution(),
			request.getLogo(),
			request.getContactPerson(),
			request.getContactPhone(),
			request.getContactEmail(),
			null, // founderId 不允许修改
			request.getPresidentId(),
			request.getMemberCount(),
			request.getStatus(),
			request.getEstablishedTime()
		);
		clubService.update(id, command);
		return ApiResponse.success();
	}

	@Operation(summary = "转交社团管理员", description = "仅系统管理员或当前社团管理员可操作")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "转交成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或成员状态异常"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权进行转交"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "社团或成员不存在")
	})
	@PutMapping("/{id}/transfer-admin")
	public ApiResponse<Void> transferAdmin(
		@Parameter(description = "社团ID", required = true) @PathVariable Long id,
		@Parameter(description = "转交请求", required = true) @Valid @RequestBody TransferAdminRequest request) {
		clubOwnershipService.transferAdmin(id, request.getTargetUserId(), getCurrentUserId(), isSystemAdmin());
		return ApiResponse.success();
	}

	/**
	 * 修改社团状态（需要系统管理员权限）
	 */
	@Operation(summary = "修改社团状态", description = "修改社团状态，0-待审核，1-已通过，2-已驳回，3-已解散，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "修改成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "状态值非法"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "社团不存在")
	})
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> changeStatus(
		@Parameter(description = "社团ID", required = true) @PathVariable Long id,
		@Parameter(description = "状态更新请求", required = true) @Valid @RequestBody ClubStatusRequest request) {
		clubService.changeStatus(id, request.getStatus());
		return ApiResponse.success();
	}

	/**
	 * 解散社团
	 */
	@Operation(summary = "解散社团", description = "社长/社团管理员可发起解散申请，系统管理员审核通过后真正解散社团；系统管理员也可以直接解散社团")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "操作成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或存在待审核的解散申请"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限解散"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "社团不存在")
	})
	@PostMapping("/{id}/dissolve")
	@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CLUB_ADMIN')")
	public ApiResponse<Void> dissolve(
		@Parameter(description = "社团ID", required = true) @PathVariable Long id,
		@Parameter(description = "解散申请请求", required = true) @Valid @RequestBody DissolveRequest request) {
		Long operatorId = getCurrentUserId();
		boolean isSystemAdmin = isSystemAdmin();

		if (isSystemAdmin) {
			// 系统管理员可直接解散社团
			clubService.dissolveClub(id, operatorId, true);
		} else {
			// 非系统管理员：创建解散申请，等待系统管理员审核
			clubDissolutionService.submit(id, operatorId, request.getReason());
		}
		return ApiResponse.success();
	}

	/**
	 * 社团排行榜
	 */
	@Operation(summary = "社团排行榜", description = "按成员数、活动数等维度返回排行榜数据")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping("/leaderboard")
	public ApiResponse<List<ClubService.ClubLeaderboardItem>> leaderboard(@Valid ClubLeaderboardRequest request) {
		return ApiResponse.success(clubService.getLeaderboard(request.getType(), request.getLimit()));
	}

	/**
	 * 删除社团（需要系统管理员权限）
	 */
	@Operation(summary = "删除社团", description = "删除社团，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "社团不存在")
	})
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> delete(
		@Parameter(description = "社团ID", required = true) @PathVariable Long id) {
		clubService.delete(id);
		return ApiResponse.success();
	}

	/**
	 * 检查管理权限
	 */
	private void checkManagePermission(Long clubId) {
		boolean isSystemAdmin = isSystemAdmin();
		Long userId = getCurrentUserId();
		clubService.checkManagePermission(clubId, userId, isSystemAdmin);
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

	@Schema(description = "社团分页查询请求")
	@Validated
	public static class ClubPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "关键词，支持社团名称、代码搜索", example = "篮球")
		private String keyword;
		@Schema(description = "社团状态，0-待审核，1-已通过，2-已驳回，3-已解散", example = "1")
		private Integer status;
		@Schema(description = "最小成员数量", example = "10")
		private Integer minMembers;
		@Schema(description = "最大成员数量", example = "200")
		private Integer maxMembers;
		@Schema(description = "排序字段：memberCount / establishedTime / createdTime", example = "memberCount")
		private String sortBy;
		@Schema(description = "排序方向：asc / desc", example = "desc")
		private String sortOrder;

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

		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(String keyword) {
			this.keyword = keyword;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
		public Integer getMinMembers() {
			return minMembers;
		}

		public void setMinMembers(Integer minMembers) {
			this.minMembers = minMembers;
		}

		public Integer getMaxMembers() {
			return maxMembers;
		}

		public void setMaxMembers(Integer maxMembers) {
			this.maxMembers = maxMembers;
		}

		public String getSortBy() {
			return sortBy;
		}

		public void setSortBy(String sortBy) {
			this.sortBy = sortBy;
		}

		public String getSortOrder() {
			return sortOrder;
		}

		public void setSortOrder(String sortOrder) {
			this.sortOrder = sortOrder;
		}
	}

	@Schema(description = "创建社团请求")
	public static class ClubCreateRequest {
		@Schema(description = "社团名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "篮球社")
		@NotBlank(message = "社团名称不能为空")
		private String clubName;
		@Schema(description = "社团代码", example = "BASKETBALL")
		private String clubCode;
		@Schema(description = "社团简介", example = "热爱篮球，享受运动")
		private String description;
		@Schema(description = "社团宗旨", example = "推广篮球运动，提高学生身体素质")
		private String purpose;
		@Schema(description = "社团章程", example = "遵守学校规定，定期组织活动")
		private String constitution;
		@Schema(description = "Logo URL", example = "https://example.com/logo.jpg")
		private String logo;
		@Schema(description = "联系人", example = "张三")
		private String contactPerson;
		@Schema(description = "联系电话", example = "13800138000")
		private String contactPhone;
		@Schema(description = "联系邮箱", example = "club@example.com")
		private String contactEmail;
		@Schema(description = "创始人ID，可选（为空则使用当前用户）", example = "1")
		private Long founderId;
		@Schema(description = "社长ID", example = "1")
		private Long presidentId;
		@Schema(description = "社团状态，0-待审核，1-已通过，2-已驳回，3-已解散", example = "1")
		@Min(value = 0, message = "状态取值非法")
		@Max(value = 3, message = "状态取值非法")
		private Integer status;
		@Schema(description = "成立时间", example = "2024-01-01T10:00:00")
		private LocalDateTime establishedTime;

		// Getters and Setters
		public String getClubName() {
			return clubName;
		}

		public void setClubName(String clubName) {
			this.clubName = clubName;
		}

		public String getClubCode() {
			return clubCode;
		}

		public void setClubCode(String clubCode) {
			this.clubCode = clubCode;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getPurpose() {
			return purpose;
		}

		public void setPurpose(String purpose) {
			this.purpose = purpose;
		}

		public String getConstitution() {
			return constitution;
		}

		public void setConstitution(String constitution) {
			this.constitution = constitution;
		}

		public String getLogo() {
			return logo;
		}

		public void setLogo(String logo) {
			this.logo = logo;
		}

		public String getContactPerson() {
			return contactPerson;
		}

		public void setContactPerson(String contactPerson) {
			this.contactPerson = contactPerson;
		}

		public String getContactPhone() {
			return contactPhone;
		}

		public void setContactPhone(String contactPhone) {
			this.contactPhone = contactPhone;
		}

		public String getContactEmail() {
			return contactEmail;
		}

		public void setContactEmail(String contactEmail) {
			this.contactEmail = contactEmail;
		}

		public Long getFounderId() {
			return founderId;
		}

		public void setFounderId(Long founderId) {
			this.founderId = founderId;
		}

		public Long getPresidentId() {
			return presidentId;
		}

		public void setPresidentId(Long presidentId) {
			this.presidentId = presidentId;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}

		public LocalDateTime getEstablishedTime() {
			return establishedTime;
		}

		public void setEstablishedTime(LocalDateTime establishedTime) {
			this.establishedTime = establishedTime;
		}
	}

	@Schema(description = "更新社团请求")
	public static class ClubUpdateRequest {
		@Schema(description = "社团名称", example = "篮球社")
		private String clubName;
		@Schema(description = "社团代码", example = "BASKETBALL")
		private String clubCode;
		@Schema(description = "社团简介", example = "热爱篮球，享受运动")
		private String description;
		@Schema(description = "社团宗旨", example = "推广篮球运动，提高学生身体素质")
		private String purpose;
		@Schema(description = "社团章程", example = "遵守学校规定，定期组织活动")
		private String constitution;
		@Schema(description = "Logo URL", example = "https://example.com/logo.jpg")
		private String logo;
		@Schema(description = "联系人", example = "张三")
		private String contactPerson;
		@Schema(description = "联系电话", example = "13800138000")
		private String contactPhone;
		@Schema(description = "联系邮箱", example = "club@example.com")
		private String contactEmail;
		@Schema(description = "社长ID", example = "1")
		private Long presidentId;
		@Schema(description = "成员数量", example = "50")
		private Integer memberCount;
		@Schema(description = "社团状态，0-待审核，1-已通过，2-已驳回，3-已解散", example = "1")
		@Min(value = 0, message = "状态取值非法")
		@Max(value = 3, message = "状态取值非法")
		private Integer status;
		@Schema(description = "成立时间", example = "2024-01-01T10:00:00")
		private LocalDateTime establishedTime;

		// Getters and Setters
		public String getClubName() {
			return clubName;
		}

		public void setClubName(String clubName) {
			this.clubName = clubName;
		}

		public String getClubCode() {
			return clubCode;
		}

		public void setClubCode(String clubCode) {
			this.clubCode = clubCode;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getPurpose() {
			return purpose;
		}

		public void setPurpose(String purpose) {
			this.purpose = purpose;
		}

		public String getConstitution() {
			return constitution;
		}

		public void setConstitution(String constitution) {
			this.constitution = constitution;
		}

		public String getLogo() {
			return logo;
		}

		public void setLogo(String logo) {
			this.logo = logo;
		}

		public String getContactPerson() {
			return contactPerson;
		}

		public void setContactPerson(String contactPerson) {
			this.contactPerson = contactPerson;
		}

		public String getContactPhone() {
			return contactPhone;
		}

		public void setContactPhone(String contactPhone) {
			this.contactPhone = contactPhone;
		}

		public String getContactEmail() {
			return contactEmail;
		}

		public void setContactEmail(String contactEmail) {
			this.contactEmail = contactEmail;
		}

		public Long getPresidentId() {
			return presidentId;
		}

		public void setPresidentId(Long presidentId) {
			this.presidentId = presidentId;
		}

		public Integer getMemberCount() {
			return memberCount;
		}

		public void setMemberCount(Integer memberCount) {
			this.memberCount = memberCount;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}

		public LocalDateTime getEstablishedTime() {
			return establishedTime;
		}

		public void setEstablishedTime(LocalDateTime establishedTime) {
			this.establishedTime = establishedTime;
		}
	}

	@Schema(description = "社团状态更新请求")
	public static class ClubStatusRequest {
		@Schema(description = "社团状态，0-待审核，1-已通过，2-已驳回，3-已解散", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@Min(value = 0, message = "状态取值非法")
		@Max(value = 3, message = "状态取值非法")
		private Integer status;

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}

	@Schema(description = "社团管理员转交请求")
	public static class TransferAdminRequest {
		@Schema(description = "新的社团管理员用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
		@NotNull(message = "目标用户ID不能为空")
		private Long targetUserId;

		public Long getTargetUserId() {
			return targetUserId;
		}

		public void setTargetUserId(Long targetUserId) {
			this.targetUserId = targetUserId;
		}
	}

	@Schema(description = "社团广场分页请求")
	@Validated
	public static class PublicClubPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "9")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 9;
		@Schema(description = "关键词，支持社团名称、代码搜索", example = "篮球")
		private String keyword;
		@Schema(description = "最少成员数量", example = "20")
		private Integer minMembers;
		@Schema(description = "最多成员数量", example = "200")
		private Integer maxMembers;
		@Schema(description = "排序字段：memberCount / establishedTime / createdTime", example = "memberCount")
		private String sortBy;
		@Schema(description = "排序方向：asc / desc", example = "desc")
		private String sortOrder;

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

		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(String keyword) {
			this.keyword = keyword;
		}

		public Integer getMinMembers() {
			return minMembers;
		}

		public void setMinMembers(Integer minMembers) {
			this.minMembers = minMembers;
		}

		public Integer getMaxMembers() {
			return maxMembers;
		}

		public void setMaxMembers(Integer maxMembers) {
			this.maxMembers = maxMembers;
		}

		public String getSortBy() {
			return sortBy;
		}

		public void setSortBy(String sortBy) {
			this.sortBy = sortBy;
		}

		public String getSortOrder() {
			return sortOrder;
		}

		public void setSortOrder(String sortOrder) {
			this.sortOrder = sortOrder;
		}
	}

	@Schema(description = "社团排行榜请求")
	public static class ClubLeaderboardRequest {
		@Schema(description = "排行榜类型：member_count 或 activity_count", example = "member_count")
		@NotBlank(message = "排行榜类型不能为空")
		private String type = "member_count";
		@Schema(description = "返回条数，默认10，最大20", example = "10")
		@Min(value = 1, message = "limit至少为1")
		@Max(value = 20, message = "limit最大为20")
		private int limit = 10;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public int getLimit() {
			return limit;
		}

		public void setLimit(int limit) {
			this.limit = limit;
		}
	}

	@Schema(description = "社团解散申请请求")
	public static class DissolveRequest {
		@Schema(description = "解散原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "成员较少，长期无活动，申请解散")
		@NotBlank(message = "解散原因不能为空")
		private String reason;

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}
	}
}

