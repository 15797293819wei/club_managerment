package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.mapper.UserMapper;
import com.example.club.service.ProfileService;
import com.example.club.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 个人信息接口
 * 提供查询/更新个人资料、修改密码功能
 */
@Tag(name = "个人信息管理", description = "个人信息相关接口，包括查询、更新个人资料和修改密码等功能")
@RestController
@RequestMapping("/api/profile")
@Validated
public class ProfileController {

	private final UserMapper userMapper;
	private final UserService userService;
	private final PasswordEncoder passwordEncoder;
	private final ProfileService profileService;

	public ProfileController(UserMapper userMapper, UserService userService, PasswordEncoder passwordEncoder, ProfileService profileService) {
		this.userMapper = userMapper;
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
		this.profileService = profileService;
	}

	/**
	 * 获取当前用户个人信息
	 */
	@Operation(summary = "获取个人信息", description = "获取当前登录用户的详细信息")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或Token无效")
	})
	@GetMapping
	public ApiResponse<UserService.UserDetail> me() {
		return ApiResponse.success(userService.detail(getCurrentUserId()));
	}

	/**
	 * 更新当前用户个人资料（不含密码与角色）
	 */
	@Operation(summary = "更新个人信息", description = "更新当前登录用户的个人资料，不包括密码和角色")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或Token无效")
	})
	@PutMapping
	public ApiResponse<Void> updateProfile(
		@Parameter(description = "更新个人信息请求", required = true) @Valid @RequestBody UpdateProfileRequest req) {
		Long userId = getCurrentUserId();
		var command = new UserService.UserUpdateCommand(
			null,
			null,
			req.getRealName(),
			null,
			req.getEmail(),
			req.getPhone(),
			req.getGender(),
			req.getAvatar(),
			null,
			null
		);
		userService.update(userId, command);
		return ApiResponse.success();
	}

	/**
	 * 修改当前用户密码
	 */
	@Operation(summary = "修改密码", description = "修改当前登录用户的密码，需要提供原密码和新密码")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "修改成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "原密码不正确或参数错误"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或Token无效")
	})
	@PutMapping("/password")
	public ApiResponse<Void> changePassword(
		@Parameter(description = "修改密码请求", required = true) @Valid @RequestBody ChangePasswordRequest req) {
		Long userId = getCurrentUserId();
		var user = userMapper.selectById(userId).orElseThrow();
		// 校验原密码
		if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
			return ApiResponse.error(400, "原密码不正确");
		}
		
		// 验证新密码强度
		com.example.club.util.PasswordValidator.PasswordValidationResult passwordValidation = 
			com.example.club.util.PasswordValidator.validate(req.getNewPassword());
		if (!passwordValidation.isValid()) {
			return ApiResponse.error(400, passwordValidation.getMessage());
		}
		
		var command = new UserService.UserUpdateCommand(
			null,
			req.getNewPassword(),
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);
		userService.update(user.getId(), command);
		return ApiResponse.success();
	}

	/**
	 * 获取个人记录和数据统计
	 */
	@Operation(summary = "获取个人概览数据", description = "返回当前用户的社团、活动、考勤、评价记录及个人数据统计")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping("/overview")
	public ApiResponse<ProfileService.PersonalOverview> overview() {
		Long userId = getCurrentUserId();
		return ApiResponse.success(profileService.getPersonalOverview(userId));
	}

	private Long getCurrentUserId() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userMapper.selectByUsername(username)
			.map(user -> user.getId())
			.orElseThrow(() -> new RuntimeException("用户不存在"));
	}

	@Schema(description = "更新个人信息请求")
	public static class UpdateProfileRequest {
		@Schema(description = "真实姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
		@NotBlank(message = "真实姓名不能为空")
		private String realName;
		@Schema(description = "邮箱", example = "zhangsan@example.com")
		@Email(message = "邮箱格式不正确")
		private String email;
		@Schema(description = "手机号", example = "13800138000")
		private String phone;
		@Schema(description = "性别，0-未知，1-男，2-女", example = "1")
		@Min(value = 0, message = "性别取值非法")
		@Max(value = 2, message = "性别取值非法")
		private Integer gender;
		@Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
		private String avatar;

		public String getRealName() {
			return realName;
		}

		public void setRealName(String realName) {
			this.realName = realName;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}

		public Integer getGender() {
			return gender;
		}

		public void setGender(Integer gender) {
			this.gender = gender;
		}

		public String getAvatar() {
			return avatar;
		}

		public void setAvatar(String avatar) {
			this.avatar = avatar;
		}
	}

	@Schema(description = "修改密码请求")
	public static class ChangePasswordRequest {
		@Schema(description = "原密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "oldPassword123")
		@NotBlank(message = "原密码不能为空")
		private String oldPassword;
		@Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "newPassword123")
		@NotBlank(message = "新密码不能为空")
		private String newPassword;

		public String getOldPassword() {
			return oldPassword;
		}

		public void setOldPassword(String oldPassword) {
			this.oldPassword = oldPassword;
		}

		public String getNewPassword() {
			return newPassword;
		}

		public void setNewPassword(String newPassword) {
			this.newPassword = newPassword;
		}
	}
}


