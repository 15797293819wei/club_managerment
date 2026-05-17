package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.dto.PageResult;
import com.example.club.mapper.UserMapper;
import com.example.club.service.ClubApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 社团申请管理接口
 * <p>提供社团申请的提交、查询、审核等接口。</p>
 */
@Tag(name = "社团申请管理", description = "社团申请相关接口，包括申请提交、查询、审核等功能")
@RestController
@RequestMapping("/api/club-applications")
@Validated
public class ClubApplicationController {

	private final ClubApplicationService applicationService;
	private final UserMapper userMapper;

	public ClubApplicationController(ClubApplicationService applicationService, UserMapper userMapper) {
		this.applicationService = applicationService;
		this.userMapper = userMapper;
	}

	/**
	 * 分页查询申请列表
	 */
	@Operation(summary = "分页查询社团申请列表", description = "根据条件分页查询社团创建申请列表，支持按关键词、状态筛选，非系统管理员只能查看自己提交的申请")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping
	public ApiResponse<PageResult<ClubApplicationService.ApplicationInfo>> page(@Valid ApplicationPageRequest request) {
		// 非系统管理员只能查看自己提交的申请
		Long applicantId = isSystemAdmin() ? null : getCurrentUserId();
		PageResult<ClubApplicationService.ApplicationInfo> result = applicationService.page(
			request.getKeyword(),
			request.getStatus(),
			applicantId,
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 查询申请详情
	 */
	@Operation(summary = "查询社团申请详情", description = "根据申请ID查询社团创建申请详细信息")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "申请不存在")
	})
	@GetMapping("/{id}")
	public ApiResponse<ClubApplicationService.ApplicationInfo> detail(
		@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(applicationService.getDetail(id));
	}

	/**
	 * 提交社团创建申请
	 */
	@Operation(summary = "提交社团创建申请", description = "学生提交新社团创建申请，包含社团名称、简介、宗旨、章程等信息")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "提交成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或社团名称/代码已存在")
	})
	@PostMapping
	public ApiResponse<Map<String, Long>> submit(
		@Parameter(description = "社团申请请求", required = true) @Valid @RequestBody ApplicationSubmitRequest request) {
		var command = new ClubApplicationService.ApplicationCommand(
			getCurrentUserId(), // applicantId
			request.getClubName(),
			request.getClubCode(),
			request.getDescription(),
			request.getPurpose(),
			request.getConstitution(),
			request.getLogo(),
			request.getAttachment()
		);
		Long id = applicationService.submit(command);
		return ApiResponse.success(Map.of("id", id));
	}

	@Operation(summary = "检测是否可以提交新的社团申请", description = "若已创建社团或存在待审核申请，将返回false")
	@GetMapping("/can-submit")
	public ApiResponse<Boolean> canSubmit() {
		return ApiResponse.success(applicationService.canSubmit(getCurrentUserId()));
	}

	/**
	 * 审核社团申请（需要系统管理员权限）
	 */
	@Operation(summary = "审核社团申请", description = "审核社团创建申请，1-通过，2-驳回，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "审核成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "审核状态值非法或该申请已审核"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "申请不存在")
	})
	@PutMapping("/{id}/review")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> review(
		@Parameter(description = "申请ID", required = true) @PathVariable Long id,
		@Parameter(description = "审核请求", required = true) @Valid @RequestBody ApplicationReviewRequest request) {
		var command = new ClubApplicationService.ReviewCommand(
			getCurrentUserId(), // reviewerId
			request.getStatus(),
			request.getReviewComment()
		);
		applicationService.review(id, command);
		return ApiResponse.success();
	}

	/**
	 * 批量审核社团申请
	 */
	@Operation(summary = "批量审核社团申请", description = "系统管理员可一次性通过/驳回多个申请")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "审核成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限")
	})
	@PostMapping("/batch-review")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> batchReview(
		@Parameter(description = "批量审核请求", required = true) @Valid @RequestBody BatchReviewRequest request) {
		var command = new ClubApplicationService.ReviewCommand(
			getCurrentUserId(),
			request.getStatus(),
			request.getReviewComment()
		);
		applicationService.batchReview(request.getApplicationIds(), command);
		return ApiResponse.success();
	}

	/**
	 * 查询社团申请审核历史
	 */
	@Operation(summary = "查询社团申请审核历史", description = "查看指定申请的审核记录")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "申请不存在")
	})
	@GetMapping("/{id}/history")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<List<ClubApplicationService.HistoryInfo>> history(
		@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(applicationService.history(id));
	}

	/**
	 * 删除申请（仅允许删除待审核状态的申请）
	 */
	@Operation(summary = "删除社团申请", description = "删除待审核状态的社团申请，只能删除自己提交的申请")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "只能删除待审核状态的申请"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "申请不存在")
	})
	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(
		@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
		applicationService.delete(id);
		return ApiResponse.success();
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

	@Schema(description = "社团申请分页查询请求")
	@Validated
	public static class ApplicationPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "关键词，支持社团名称搜索", example = "篮球")
		private String keyword;
		@Schema(description = "申请状态，0-待审核，1-已通过，2-已驳回", example = "0")
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
	}

	@Schema(description = "提交社团申请请求")
	public static class ApplicationSubmitRequest {
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
		@Schema(description = "证明材料URL", example = "https://example.com/attachment.pdf")
		private String attachment;

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

		public String getAttachment() {
			return attachment;
		}

		public void setAttachment(String attachment) {
			this.attachment = attachment;
		}
	}

	@Schema(description = "审核社团申请请求")
	public static class ApplicationReviewRequest {
		@Schema(description = "审核状态，1-通过，2-驳回", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "审核状态不能为空")
		@Min(value = 1, message = "审核状态取值非法")
		@Max(value = 2, message = "审核状态取值非法")
		private Integer status;
		@Schema(description = "审核意见", example = "申请通过，符合创建条件")
		private String reviewComment;

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}

		public String getReviewComment() {
			return reviewComment;
		}

		public void setReviewComment(String reviewComment) {
			this.reviewComment = reviewComment;
		}
	}

	@Schema(description = "批量审核社团申请请求")
	public static class BatchReviewRequest {
		@Schema(description = "申请ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1,2,3]")
		@NotEmpty(message = "申请ID列表不能为空")
		private List<Long> applicationIds;
		@Schema(description = "审核状态，1-通过，2-驳回", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "审核状态不能为空")
		@Min(value = 1, message = "审核状态取值非法")
		@Max(value = 2, message = "审核状态取值非法")
		private Integer status;
		@Schema(description = "审核意见", example = "资料不完整")
		private String reviewComment;

		public List<Long> getApplicationIds() {
			return applicationIds;
		}

		public void setApplicationIds(List<Long> applicationIds) {
			this.applicationIds = applicationIds;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}

		public String getReviewComment() {
			return reviewComment;
		}

		public void setReviewComment(String reviewComment) {
			this.reviewComment = reviewComment;
		}
	}
}

