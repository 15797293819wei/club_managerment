package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.UserMapper;
import com.example.club.service.MemberApplicationService;
import com.example.club.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
 * 入社申请管理接口
 * <p>提供入社申请的提交、查询、审核等接口。</p>
 */
@Tag(name = "入社申请管理", description = "入社申请相关接口，包括申请提交、查询、审核等功能")
@RestController
@RequestMapping("/api/member-applications")
@Validated
public class MemberApplicationController {

	private final MemberApplicationService applicationService;
	private final UserMapper userMapper;
	private final MemberService memberService;

	public MemberApplicationController(MemberApplicationService applicationService,
									   UserMapper userMapper,
									   MemberService memberService) {
		this.applicationService = applicationService;
		this.userMapper = userMapper;
		this.memberService = memberService;
	}

	/**
	 * 分页查询申请列表
	 */
	@Operation(summary = "分页查询入社申请列表", description = "根据条件分页查询入社申请列表，支持按社团、状态筛选")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping
	public ApiResponse<PageResult<MemberApplicationService.ApplicationInfo>> page(@Valid ApplicationPageRequest request) {
		// 只有社团管理员、部长、副部长可以查看所有申请，普通用户只能查看自己的申请
		boolean canManage = hasClubManagePermission(request.getClubId());
		Long applicantId = canManage ? null : getCurrentUserId();
		PageResult<MemberApplicationService.ApplicationInfo> result = applicationService.page(
			request.getClubId(),
			applicantId,
			request.getStatus(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 查询申请详情
	 */
	@Operation(summary = "查询入社申请详情", description = "根据申请ID查询入社申请详细信息")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "申请不存在")
	})
	@GetMapping("/{id}")
	public ApiResponse<MemberApplicationService.ApplicationInfo> detail(
		@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(applicationService.getDetail(id));
	}

	/**
	 * 查看申请人扩展信息
	 */
	@Operation(summary = "查看入社申请扩展信息", description = "获取申请人的基本资料、历史申请及参与统计")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "申请不存在")
	})
	@GetMapping("/{id}/insight")
	public ApiResponse<MemberApplicationService.ApplicationInspection> inspection(
		@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
		MemberApplicationService.ApplicationInfo application = applicationService.getDetail(id);
		checkManagePermission(application.clubId());
		return ApiResponse.success(applicationService.getInspectionDetail(id));
	}

	/**
	 * 提交入社申请
	 */
	@Operation(summary = "提交入社申请", description = "学生提交加入指定社团的申请")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "提交成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或该社团尚未通过审核或您已是该社团成员或已有待审核的申请")
	})
	@PostMapping
	public ApiResponse<Map<String, Long>> submit(
		@Parameter(description = "入社申请请求", required = true) @Valid @RequestBody ApplicationSubmitRequest request) {
		var command = new MemberApplicationService.ApplicationCommand(
			request.getClubId(),
			getCurrentUserId(), // applicantId
			request.getApplicationReason()
		);
		Long id = applicationService.submit(command);
		return ApiResponse.success(Map.of("id", id));
	}

	/**
	 * 审核入社申请（需要社团管理员、部长或副部长权限）
	 */
	@Operation(summary = "审核入社申请", description = "审核入社申请，1-通过，2-驳回，需要社团管理员、部长或副部长权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "审核成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "审核状态值非法或该申请已审核"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权管理该社团的申请"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "申请不存在")
	})
	@PutMapping("/{id}/review")
	public ApiResponse<Void> review(
		@Parameter(description = "申请ID", required = true) @PathVariable Long id,
		@Parameter(description = "审核请求", required = true) @Valid @RequestBody ApplicationReviewRequest request) {
		// 获取申请信息以获取clubId
		MemberApplicationService.ApplicationInfo application = applicationService.getDetail(id);
		checkManagePermission(application.clubId());
		var command = new MemberApplicationService.ReviewCommand(
			getCurrentUserId(), // reviewerId
			request.getStatus(),
			request.getReviewComment()
		);
		applicationService.review(id, command);
		return ApiResponse.success();
	}

	/**
	 * 批量审核入社申请
	 */
	@Operation(summary = "批量审核入社申请", description = "一次性通过或驳回多个入社申请")
	@PostMapping("/batch-review")
	public ApiResponse<Void> batchReview(
		@Parameter(description = "批量审核请求", required = true) @Valid @RequestBody BatchReviewRequest request) {
		checkManagePermission(request.getClubId());
		var command = new MemberApplicationService.ReviewCommand(
			getCurrentUserId(),
			request.getStatus(),
			request.getReviewComment()
		);
		applicationService.batchReview(request.getClubId(), request.getApplicationIds(), command);
		return ApiResponse.success();
	}

	/**
	 * 删除申请（仅允许删除待审核状态的申请）
	 */
	@Operation(summary = "删除入社申请", description = "删除待审核状态的入社申请，只能删除自己提交的申请")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "只能删除待审核状态的申请"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "只能删除自己提交的申请"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "申请不存在")
	})
	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(
		@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
		// 只能删除自己提交的申请
		MemberApplicationService.ApplicationInfo application = applicationService.getDetail(id);
		if (!application.applicantId().equals(getCurrentUserId())) {
			throw new RuntimeException("只能删除自己提交的申请");
		}
		applicationService.delete(id);
		return ApiResponse.success();
	}

	/**
	 * 检查管理权限
	 * 只有社团管理员（CLUB_ADMIN）、部长（MINISTER）、副部长（VICE_MINISTER）可以审核入社申请
	 * 系统管理员不能审核入社申请
	 */
	private void checkManagePermission(Long clubId) {
		memberService.checkManagePermission(clubId, getCurrentUserId(), false);
	}

	private boolean hasClubManagePermission(Long clubId) {
		if (clubId == null) {
			return false;
		}
		try {
			memberService.checkManagePermission(clubId, getCurrentUserId(), false);
			return true;
		} catch (BusinessException ignored) {
			return false;
		}
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

	@Schema(description = "入社申请分页查询请求")
	@Validated
	public static class ApplicationPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "社团ID，可选", example = "1")
		private Long clubId;
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

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}

	@Schema(description = "提交入社申请请求")
	public static class ApplicationSubmitRequest {
		@Schema(description = "社团ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@Schema(description = "申请理由", example = "我对该社团的活动很感兴趣，希望能加入")
		private String applicationReason;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

		public String getApplicationReason() {
			return applicationReason;
		}

		public void setApplicationReason(String applicationReason) {
			this.applicationReason = applicationReason;
		}
	}

	@Schema(description = "审核入社申请请求")
	public static class ApplicationReviewRequest {
		@Schema(description = "审核状态，1-通过，2-驳回", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "审核状态不能为空")
		@Min(value = 1, message = "审核状态取值非法")
		@Max(value = 2, message = "审核状态取值非法")
		private Integer status;
		@Schema(description = "审核意见", example = "申请通过")
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

	@Schema(description = "批量审核入社申请请求")
	public static class BatchReviewRequest {
		@Schema(description = "社团ID", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "社团ID不能为空")
		private Long clubId;
		@Schema(description = "申请ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1,2,3]")
		@NotEmpty(message = "申请ID列表不能为空")
		private List<Long> applicationIds;
		@Schema(description = "审核状态，1-通过，2-驳回", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "审核状态不能为空")
		@Min(value = 1, message = "审核状态取值非法")
		@Max(value = 2, message = "审核状态取值非法")
		private Integer status;
		@Schema(description = "审核意见")
		private String reviewComment;

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}

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

