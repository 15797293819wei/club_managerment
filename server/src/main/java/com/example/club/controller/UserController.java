package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.dto.PageResult;
import com.example.club.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户管理接口
 * <p>提供用户的分页查询、详情获取、创建更新、状态变更及角色分配等接口。</p>
 */
@Tag(name = "用户管理", description = "用户管理相关接口，包括用户列表查询、详情、创建、更新、状态变更、角色分配等功能，需要系统管理员权限")
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 分页查询用户列表
	 */
	@Operation(summary = "分页查询用户列表", description = "根据条件分页查询用户列表，支持按关键词、状态筛选，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限")
	})
	@GetMapping
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<PageResult<UserService.UserSummary>> page(@Valid UserPageRequest request) {
		PageResult<UserService.UserSummary> page = userService.page(
			request.getKeyword(),
			request.getUsername(),
			request.getStudentId(),
			request.getRoleCode(),
			request.getStatus(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(page);
	}

	/**
	 * 查询用户详情
	 */
	@Operation(summary = "查询用户详情", description = "根据用户ID查询用户详细信息，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
	})
	@GetMapping("/{id}")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<UserService.UserDetail> detail(
		@Parameter(description = "用户ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(userService.detail(id));
	}

	/**
	 * 创建用户
	 */
	@Operation(summary = "创建用户", description = "创建新用户，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或用户名/学号已存在"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限")
	})
	@PostMapping
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Map<String, Long>> create(
		@Parameter(description = "创建用户请求", required = true) @Valid @RequestBody UserCreateRequest request) {
		var command = new UserService.UserCreateCommand(
			request.getUsername(),
			request.getPassword(),
			request.getRealName(),
			request.getStudentId(),
			request.getEmail(),
			request.getPhone(),
			request.getGender(),
			request.getAvatar(),
			request.getStatus(),
			normalizeRoleIds(request.getRoleIds())
		);
		Long id = userService.create(command);
		return ApiResponse.success(Map.of("id", id));
	}

	/**
	 * 更新用户信息
	 */
	@Operation(summary = "更新用户信息", description = "更新用户信息，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或用户名/学号已存在"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
	})
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> update(
		@Parameter(description = "用户ID", required = true) @PathVariable Long id,
		@Parameter(description = "更新用户请求", required = true) @Valid @RequestBody UserUpdateRequest request) {
		var command = new UserService.UserUpdateCommand(
			request.getUsername(),
			request.getPassword(),
			request.getRealName(),
			request.getStudentId(),
			request.getEmail(),
			request.getPhone(),
			request.getGender(),
			request.getAvatar(),
			request.getStatus(),
			normalizeRoleIdsNullable(request.getRoleIds())
		);
		userService.update(id, command);
		return ApiResponse.success();
	}

	/**
	 * 修改用户状态
	 */
	@Operation(summary = "修改用户状态", description = "修改用户状态，0-禁用，1-启用，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "修改成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "状态值非法"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
	})
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> changeStatus(
		@Parameter(description = "用户ID", required = true) @PathVariable Long id,
		@Parameter(description = "状态更新请求", required = true) @Valid @RequestBody UserStatusRequest request) {
		userService.changeStatus(id, request.getStatus());
		return ApiResponse.success();
	}

	/**
	 * 批量修改用户状态
	 */
	@Operation(summary = "批量修改用户状态", description = "批量启用或禁用用户，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "操作成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限")
	})
	@PostMapping("/status/batch")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> batchChangeStatus(
		@Parameter(description = "批量状态更新请求", required = true) @Valid @RequestBody UserBatchStatusRequest request) {
		userService.batchChangeStatus(request.getUserIds(), request.getStatus());
		return ApiResponse.success();
	}

	/**
	 * 系统管理员重置用户密码
	 */
	@Operation(summary = "重置用户密码", description = "系统管理员为指定用户重置密码，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "重置成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
	})
	@PostMapping("/{id}/reset-password")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> resetPassword(
		@Parameter(description = "用户ID", required = true) @PathVariable Long id,
		@Parameter(description = "重置密码请求", required = true) @Valid @RequestBody ResetPasswordRequest request) {
		userService.resetPasswordByAdmin(id, request.getNewPassword());
		return ApiResponse.success();
	}

	/**
	 * 分配用户角色
	 */
	@Operation(summary = "分配用户角色", description = "为用户分配角色，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "分配成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
	})
	@PutMapping("/{id}/roles")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> assignRoles(
		@Parameter(description = "用户ID", required = true) @PathVariable Long id,
		@Parameter(description = "角色分配请求", required = true) @Valid @RequestBody AssignRolesRequest request) {
		userService.assignRoles(id, normalizeRoleIds(request.getRoleIds()));
		return ApiResponse.success();
	}

	/**
	 * 删除用户
	 */
	@Operation(summary = "删除用户", description = "删除用户，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
	})
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> delete(
		@Parameter(description = "用户ID", required = true) @PathVariable Long id) {
		userService.delete(id);
		return ApiResponse.success();
	}

	/**
	 * 去重过滤角色ID集合
	 */
	private List<Long> normalizeRoleIds(List<Long> roleIds) {
		if (CollectionUtils.isEmpty(roleIds)) {
			return List.of();
		}
		return roleIds.stream()
			.filter(Objects::nonNull)
			.distinct()
			.toList();
	}

	/**
	 * 允许传入空值的角色ID集合处理
	 */
	private List<Long> normalizeRoleIdsNullable(List<Long> roleIds) {
		if (roleIds == null) {
			return null;
		}
		return normalizeRoleIds(roleIds);
	}

	@Schema(description = "用户分页查询请求")
	@Validated
	public static class UserPageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "关键词，支持用户名、真实姓名、学号搜索", example = "张三")
		private String keyword;
		@Schema(description = "用户名（模糊匹配）", example = "admin")
		private String username;
		@Schema(description = "学号（模糊匹配）", example = "2023")
		private String studentId;
		@Schema(description = "角色编码（精确匹配）", example = "SYSTEM_ADMIN")
		private String roleCode;
		@Schema(description = "用户状态，0-禁用，1-启用", example = "1")
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

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getStudentId() {
			return studentId;
		}

		public void setStudentId(String studentId) {
			this.studentId = studentId;
		}

		public String getRoleCode() {
			return roleCode;
		}

		public void setRoleCode(String roleCode) {
			this.roleCode = roleCode;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}

	@Schema(description = "创建用户请求")
	public static class UserCreateRequest {
		@Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
		@NotBlank(message = "用户名不能为空")
		private String username;
		@Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
		@NotBlank(message = "密码不能为空")
		private String password;
		@Schema(description = "真实姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
		@NotBlank(message = "真实姓名不能为空")
		private String realName;
		@Schema(description = "学号", example = "2024001")
		private String studentId;
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
		@Schema(description = "用户状态，0-禁用，1-启用", example = "1")
		@Min(value = 0, message = "状态取值非法")
		@Max(value = 1, message = "状态取值非法")
		private Integer status;
		@Schema(description = "角色ID列表", example = "[1, 2]")
		private List<Long> roleIds;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getRealName() {
			return realName;
		}

		public void setRealName(String realName) {
			this.realName = realName;
		}

		public String getStudentId() {
			return studentId;
		}

		public void setStudentId(String studentId) {
			this.studentId = studentId;
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

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}

		public List<Long> getRoleIds() {
			return roleIds;
		}

		public void setRoleIds(List<Long> roleIds) {
			this.roleIds = roleIds;
		}
	}

	@Schema(description = "更新用户请求")
	public static class UserUpdateRequest {
		@Schema(description = "用户名", example = "zhangsan")
		private String username;
		@Schema(description = "密码", example = "newPassword123")
		private String password;
		@Schema(description = "真实姓名", example = "张三")
		private String realName;
		@Schema(description = "学号", example = "2024001")
		private String studentId;
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
		@Schema(description = "用户状态，0-禁用，1-启用", example = "1")
		@Min(value = 0, message = "状态取值非法")
		@Max(value = 1, message = "状态取值非法")
		private Integer status;
		@Schema(description = "角色ID列表", example = "[1, 2]")
		private List<Long> roleIds;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getRealName() {
			return realName;
		}

		public void setRealName(String realName) {
			this.realName = realName;
		}

		public String getStudentId() {
			return studentId;
		}

		public void setStudentId(String studentId) {
			this.studentId = studentId;
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

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}

		public List<Long> getRoleIds() {
			return roleIds;
		}

		public void setRoleIds(List<Long> roleIds) {
			this.roleIds = roleIds;
		}
	}

	@Schema(description = "用户状态更新请求")
	public static class UserStatusRequest {
		@Schema(description = "用户状态，0-禁用，1-启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "状态不能为空")
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

	@Schema(description = "批量状态更新请求")
	public static class UserBatchStatusRequest {
		@Schema(description = "用户ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1,2,3]")
		@NotEmpty(message = "用户ID列表不能为空")
		private List<Long> userIds;
		@Schema(description = "用户状态，0-禁用，1-启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
		@NotNull(message = "状态不能为空")
		@Min(value = 0, message = "状态取值非法")
		@Max(value = 1, message = "状态取值非法")
		private Integer status;

		public List<Long> getUserIds() {
			return userIds;
		}

		public void setUserIds(List<Long> userIds) {
			this.userIds = userIds;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}

	@Schema(description = "重置密码请求")
	public static class ResetPasswordRequest {
		@Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "NewPassword123")
		@NotBlank(message = "新密码不能为空")
		@Size(min = 8, max = 64, message = "密码长度需为8-64个字符")
		private String newPassword;

		public String getNewPassword() {
			return newPassword;
		}

		public void setNewPassword(String newPassword) {
			this.newPassword = newPassword;
		}
	}

	@Schema(description = "分配用户角色请求")
	public static class AssignRolesRequest {
		@Schema(description = "角色ID列表", example = "[1, 2]")
		private List<Long> roleIds;

		public List<Long> getRoleIds() {
			return roleIds;
		}

		public void setRoleIds(List<Long> roleIds) {
			this.roleIds = roleIds;
		}
	}
}


