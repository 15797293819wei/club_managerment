package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.mapper.UserMapper;
import com.example.club.service.ClubDissolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社团解散申请管理接口
 * 目前主要用于系统管理员审核解散申请
 */
@Tag(name = "社团解散管理", description = "社团解散申请审核等相关接口")
@RestController
@RequestMapping("/api/club-dissolutions")
@Validated
public class ClubDissolutionController {

	private final ClubDissolutionService clubDissolutionService;
	private final UserMapper userMapper;

	public ClubDissolutionController(ClubDissolutionService clubDissolutionService, UserMapper userMapper) {
		this.clubDissolutionService = clubDissolutionService;
		this.userMapper = userMapper;
	}

	/**
	 * 审核社团解散申请（需要系统管理员权限）
	 */
	@Operation(summary = "审核社团解散申请", description = "审核社团解散申请，1-通过，2-驳回，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "审核成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "审核状态值非法或该申请已审核"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "解散申请不存在")
	})
	@PutMapping("/{id}/review")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> review(
		@Parameter(description = "解散申请ID", required = true) @PathVariable Long id,
		@Parameter(description = "审核请求", required = true) @Valid @RequestBody ReviewRequest request) {
		clubDissolutionService.review(id, getCurrentUserId(), request.getStatus(), request.getReviewComment());
		return ApiResponse.success();
	}

	private Long getCurrentUserId() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userMapper.selectByUsername(username)
			.map(user -> user.getId())
			.orElseThrow(() -> new RuntimeException("用户不存在"));
	}

	@Schema(description = "审核社团解散申请请求")
	public static class ReviewRequest {
		@Schema(description = "审核状态，1-通过，2-驳回", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "审核状态不能为空")
		@Min(value = 1, message = "审核状态取值非法")
		@Max(value = 2, message = "审核状态取值非法")
		private Integer status;

		@Schema(description = "审核意见", example = "同意解散，成员已全部通知")
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
}

