package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.dto.PageResult;
import com.example.club.mapper.UserMapper;
import com.example.club.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "公告管理", description = "系统公告与社团公告接口")
@Validated
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

	private final AnnouncementService announcementService;
	private final UserMapper userMapper;

	public AnnouncementController(AnnouncementService announcementService, UserMapper userMapper) {
		this.announcementService = announcementService;
		this.userMapper = userMapper;
	}

	@Operation(summary = "分页查询可见公告", description = "返回当前用户可以查看的公告，包含系统公告与所属社团公告")
	@GetMapping
	public ApiResponse<PageResult<AnnouncementService.AnnouncementView>> page(
		@Schema(description = "页码，最小为1", example = "1") @RequestParam(defaultValue = "1") @Min(1) int page,
		@Schema(description = "页大小，1-100", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
		Long userId = getCurrentUserId();
		return ApiResponse.success(announcementService.pageForAudience(userId, page, size));
	}

	@Operation(summary = "分页查询我发布的公告", description = "系统管理员可查看全部公告，社团管理角色（社长/部长/副部长）仅能查看自己所在社团的公告")
	@GetMapping("/manage")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<PageResult<AnnouncementService.AnnouncementView>> managePage(
		@Valid ManagePageRequest request) {
		boolean systemAdmin = isSystemAdmin();
		return ApiResponse.success(announcementService.pageForManage(
			getCurrentUserId(),
			systemAdmin,
			request.getScopeType(),
			request.getClubId(),
			request.getPage(),
			request.getSize()
		));
	}

	@Operation(summary = "发布公告", description = "系统管理员发布全局公告，社团负责人（社长/部长/副部长）发布本社团公告")
	@PostMapping
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<Void> publish(@Valid @RequestBody PublishRequest request) {
		boolean systemAdmin = isSystemAdmin();
		Long userId = getCurrentUserId();
		announcementService.publish(
			new AnnouncementService.AnnouncementPublishCommand(
				userId,
				request.getTitle(),
				request.getContent(),
				request.getClubId()
			),
			systemAdmin
		);
		return ApiResponse.success();
	}

	private Long getCurrentUserId() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userMapper.selectByUsername(username)
			.map(user -> user.getId())
			.orElseThrow(() -> new RuntimeException("用户不存在"));
	}

	private boolean isSystemAdmin() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return false;
		}
		return auth.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(authority -> authority.equals("ROLE_SYSTEM_ADMIN"));
	}

	public static class PublishRequest {
		@NotBlank(message = "标题不能为空")
		private String title;
		@NotBlank(message = "内容不能为空")
		private String content;
		private Long clubId;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}
	}

	public static class ManagePageRequest {
		@Min(1)
		private int page = 1;
		@Min(1)
		@Max(100)
		private int size = 10;
		@Schema(description = "范围：GLOBAL / CLUB")
		private String scopeType;
		@Schema(description = "社团ID，仅在scopeType=CLUB时可选")
		private Long clubId;

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

		public String getScopeType() {
			return scopeType;
		}

		public void setScopeType(String scopeType) {
			this.scopeType = scopeType;
		}

		public Long getClubId() {
			return clubId;
		}

		public void setClubId(Long clubId) {
			this.clubId = clubId;
		}
	}
}

