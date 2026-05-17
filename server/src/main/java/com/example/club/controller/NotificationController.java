package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.domain.Notification;
import com.example.club.dto.PageResult;
import com.example.club.mapper.UserMapper;
import com.example.club.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "消息通知", description = "站内消息通知相关接口")
@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationController {

	private final NotificationService notificationService;
	private final UserMapper userMapper;

	public NotificationController(NotificationService notificationService, UserMapper userMapper) {
		this.notificationService = notificationService;
		this.userMapper = userMapper;
	}

	@Operation(summary = "分页查询当前用户消息", description = "获取当前登录用户的站内消息列表，可选择仅查看未读")
	@GetMapping
	public ApiResponse<PageResult<Notification>> page(
		@Parameter(description = "是否仅查看未读，默认为false") Boolean onlyUnread,
		@Schema(description = "页码，最小为1", example = "1") @Min(1) int page,
		@Schema(description = "每页大小，最小为1，最大为100", example = "10") @Min(1) @Max(100) int size) {
		Long userId = getCurrentUserId();
		PageResult<Notification> result = notificationService.page(userId, Boolean.TRUE.equals(onlyUnread), page, size);
		return ApiResponse.success(result);
	}

	@Operation(summary = "获取未读消息数量")
	@GetMapping("/unread-count")
	public ApiResponse<Long> unreadCount() {
		Long userId = getCurrentUserId();
		return ApiResponse.success(notificationService.countUnread(userId));
	}

	@Operation(summary = "标记单条消息为已读")
	@PostMapping("/{id}/read")
	public ApiResponse<Void> markRead(@PathVariable Long id) {
		notificationService.markRead(id);
		return ApiResponse.success();
	}

	@Operation(summary = "标记当前用户所有消息为已读")
	@PostMapping("/read-all")
	public ApiResponse<Void> markAllRead() {
		Long userId = getCurrentUserId();
		notificationService.markAllRead(userId);
		return ApiResponse.success();
	}

	private Long getCurrentUserId() {
		String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
			.getAuthentication().getName();
		return userMapper.selectByUsername(username)
			.map(user -> user.getId())
			.orElseThrow(() -> new RuntimeException("用户不存在"));
	}
}


