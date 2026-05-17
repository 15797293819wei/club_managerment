package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.UserMapper;
import com.example.club.security.JwtService;
import com.example.club.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供的接口：
 * - POST /api/auth/login：用户名密码登录，返回 JWT 令牌
 * - GET /api/auth/me：获取当前登录用户的用户名与权限信息
 */
@Tag(name = "认证管理", description = "用户认证相关接口，包括登录、获取当前用户信息等功能")
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

	// 认证管理器，用于验证用户凭据
	private final AuthenticationManager authenticationManager;

	// JWT服务，用于生成和验证JWT令牌
	private final JwtService jwtService;

	// 用户映射器，用于从数据库查询用户信息
	private final UserMapper userMapper;
	// 用户服务，用于处理注册等业务逻辑
	private final UserService userService;
	// 密码重置服务
	private final com.example.club.service.PasswordResetService passwordResetService;

	public AuthController(AuthenticationManager authenticationManager,
						  JwtService jwtService,
						  UserMapper userMapper,
						  UserService userService,
						  com.example.club.service.PasswordResetService passwordResetService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userMapper = userMapper;
		this.userService = userService;
		this.passwordResetService = passwordResetService;
	}

	/**
	 * 登录请求记录类
	 * 包含用户名和密码字段，并进行非空验证
	 */
	@Schema(description = "登录请求")
	public record LoginRequest(
		@Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
		@NotBlank String username,
		@Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
		@NotBlank String password) {}

	/**
	 * 注册请求记录类
	 * 用于接收用户注册时的基础信息
	 */
	@Schema(description = "注册请求")
	public record RegisterRequest(
		@Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "student001")
		@NotBlank(message = "用户名不能为空")
		@Size(min = 3, max = 32, message = "用户名长度需在3-32个字符之间")
		String username,

		@Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
		@NotBlank(message = "密码不能为空")
		@Size(min = 6, max = 64, message = "密码长度需在6-64个字符之间")
		String password,

		@Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
		@NotBlank(message = "确认密码不能为空")
		String confirmPassword,

		@Schema(description = "真实姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
		@NotBlank(message = "真实姓名不能为空")
		String realName,

		@Schema(description = "学号", example = "2024001")
		String studentId,

		@Schema(description = "邮箱", example = "student@example.com")
		String email,

		@Schema(description = "手机号", example = "13800138000")
		String phone
	) {}

	/**
	 * 用户登录接口
	 * 接收用户名和密码，验证成功后生成JWT令牌返回
	 */
	@Operation(summary = "用户登录", description = "使用用户名和密码登录，验证成功后返回JWT令牌")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户名或密码错误")
	})
	@PostMapping("/login")
	public ApiResponse<Map<String, Object>> login(
		@Parameter(description = "登录请求", required = true) @RequestBody LoginRequest req) {
		// 先查询用户，检查账户状态
		var dbUserOpt = userMapper.selectByUsername(req.username());
		if (dbUserOpt.isEmpty()) {
			throw new BusinessException(401, "用户名或密码错误");
		}
		
		var dbUser = dbUserOpt.get();
		
		// 检查账户是否被锁定
		LocalDateTime lockedUntil = passwordResetService.checkAccountLocked(dbUser);
		if (lockedUntil != null) {
			throw new BusinessException(40301, 
				String.format("账户已被锁定，请于 %s 后重试", 
					lockedUntil.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
		}
		
		// 检查账户状态
		if (dbUser.getStatus() == null || dbUser.getStatus() != 1) {
			throw new BusinessException(40302, "账户已被禁用");
		}
		
		try {
			// 委托 AuthenticationManager 做账号密码校验
			Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(req.username(), req.password())
			);
			// 设置认证信息到安全上下文
			SecurityContextHolder.getContext().setAuthentication(authentication);
			
			// 登录成功，重置登录失败次数
			passwordResetService.handleLoginSuccess(dbUser.getId());
			
			// 更新最后登录时间
			userMapper.updateLastLoginTime(dbUser.getId(), LocalDateTime.now());
			// 获取用户角色信息
			var roles = userMapper.selectRoleCodesByUserId(dbUser.getId());
			
			// 构建JWT声明，包含用户角色信息
			Map<String, Object> claims = new HashMap<>();
			claims.put("roles", roles);
			// 生成JWT令牌
			String token = jwtService.generateToken(dbUser.getUsername(), claims);
			
			// 构建响应数据
			Map<String, Object> resp = new HashMap<>();
			resp.put("token", token);
			
			return ApiResponse.success(resp);
		}
		catch (AuthenticationException ex) {
			// 登录失败，增加失败次数
			passwordResetService.handleLoginFailure(dbUser.getId());
			throw new BusinessException(401, "用户名或密码错误");
		}
	}

	/**
	 * 用户注册接口
	 * 注册成功后返回新用户ID，默认赋予学生角色
	 */
	@Operation(summary = "用户注册", description = "注册普通学生用户，注册成功后默认赋予学生角色")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "用户名或学号已存在")
	})
	@PostMapping("/register")
	public ApiResponse<Map<String, Object>> register(
		@Parameter(description = "注册请求", required = true) @Valid @RequestBody RegisterRequest req) {
		if (!req.password().equals(req.confirmPassword())) {
			throw new BusinessException(40020, "两次输入的密码不一致");
		}
		String username = req.username().trim();
		String realName = req.realName().trim();
		String studentId = normalizeOptional(req.studentId());
		String email = normalizeOptional(req.email());
		String phone = normalizeOptional(req.phone());

		Long userId = userService.register(username, req.password(), realName, studentId, email, phone);
		return ApiResponse.success(Map.of("id", userId));
	}

	/**
	 * 获取当前登录用户信息接口
	 * 从SecurityContext中获取当前认证用户的用户名和权限信息
	 */
	@Operation(summary = "获取当前用户信息", description = "获取当前登录用户的用户名和权限信息")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或Token无效")
	})
	@GetMapping("/me")
	public ApiResponse<Map<String, Object>> me() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new BusinessException(401, "未登录或Token无效");
		}
		String username = authentication.getName();
		var user = userMapper.selectByUsername(username)
			.orElseThrow(() -> new BusinessException(40400, "用户不存在"));
		var detail = userService.detail(user.getId());

		Map<String, Object> resp = new HashMap<>();
		resp.put("id", detail.id());
		resp.put("username", detail.username());
		resp.put("realName", detail.realName());
		resp.put("roles", detail.roles().stream()
			.map(UserService.RoleSimple::roleCode)
			.map(code -> code != null ? code.toUpperCase() : null)
			.filter(code -> code != null && !code.isEmpty())
			.toList());
		return ApiResponse.success(resp);
	}

	/**
	 * 请求密码重置
	 */
	@Operation(summary = "请求密码重置", description = "通过邮箱或手机号请求密码重置，系统会发送重置链接（实际应用中）")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "请求成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
	})
	@PostMapping("/forgot-password")
	public ApiResponse<Map<String, Object>> forgotPassword(
		@Parameter(description = "忘记密码请求", required = true) @RequestBody ForgotPasswordRequest req) {
		String token = passwordResetService.requestPasswordReset(req.username(), req.email(), req.phone());
		// 实际应用中不应该返回token，应该通过邮件/短信发送
		// 这里仅用于测试
		return ApiResponse.success(Map.of("message", "如果该邮箱/手机号已注册，重置链接已发送", "token", token));
	}
	
	/**
	 * 重置密码
	 */
	@Operation(summary = "重置密码", description = "通过重置令牌设置新密码")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "重置成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "令牌无效或密码不符合要求")
	})
	@PostMapping("/reset-password")
	public ApiResponse<Void> resetPassword(
		@Parameter(description = "重置密码请求", required = true) @RequestBody ResetPasswordRequest req) {
		passwordResetService.resetPassword(req.token(), req.newPassword());
		return ApiResponse.success();
	}
	
	/**
	 * 验证密码强度
	 */
	@Operation(summary = "验证密码强度", description = "验证密码是否符合强度要求")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "验证成功")
	})
	@PostMapping("/validate-password")
	public ApiResponse<Map<String, Object>> validatePassword(
		@Parameter(description = "密码", required = true) @RequestBody Map<String, String> request) {
		String password = request.get("password");
		if (password == null) {
			throw new BusinessException(40001, "密码不能为空");
		}
		
		com.example.club.util.PasswordValidator.PasswordValidationResult result = 
			com.example.club.util.PasswordValidator.validate(password);
		
		Map<String, Object> resp = new HashMap<>();
		resp.put("valid", result.isValid());
		resp.put("message", result.getMessage());
		resp.put("strength", result.getStrength());
		
		return ApiResponse.success(resp);
	}
	
	@Schema(description = "忘记密码请求")
	public record ForgotPasswordRequest(
		@Schema(description = "用户名", example = "student001")
		String username,
		@Schema(description = "邮箱", example = "user@example.com")
		String email,
		@Schema(description = "手机号", example = "13800138000")
		String phone
	) {}
	
	@Schema(description = "重置密码请求")
	public record ResetPasswordRequest(
		@Schema(description = "重置令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "abc123...")
		@NotBlank String token,
		@Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "NewPassword123")
		@NotBlank @Size(min = 8, max = 64) String newPassword
	) {}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
