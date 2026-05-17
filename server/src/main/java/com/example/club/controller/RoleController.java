package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.dto.PageResult;
import com.example.club.service.RoleService;
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

/**
 * 角色管理接口
 * <p>提供角色的分页查询、详情、启用列表以及增删改接口。</p>
 */
@Tag(name = "角色管理", description = "角色管理相关接口，包括角色列表查询、详情、创建、更新、状态变更等功能，需要系统管理员权限")
@RestController
@RequestMapping("/api/roles")
@Validated
public class RoleController {

	private final RoleService roleService;

	public RoleController(RoleService roleService) {
		this.roleService = roleService;
	}

	/**
	 * 分页查询角色
	 */
	@Operation(summary = "分页查询角色列表", description = "根据条件分页查询角色列表，支持按关键词、状态筛选，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限")
	})
	@GetMapping
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<PageResult<RoleService.RoleInfo>> page(@Valid RolePageRequest request) {
		PageResult<RoleService.RoleInfo> result = roleService.page(
			request.getKeyword(),
			request.getStatus(),
			request.getPage(),
			request.getSize()
		);
		return ApiResponse.success(result);
	}

	/**
	 * 查看角色详情
	 */
	@Operation(summary = "查询角色详情", description = "根据角色ID查询角色详细信息，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "角色不存在")
	})
	@GetMapping("/{id}")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<RoleService.RoleInfo> detail(
		@Parameter(description = "角色ID", required = true) @PathVariable Long id) {
		return ApiResponse.success(roleService.getDetail(id));
	}

	/**
	 * 查询所有启用中的角色
	 */
	@Operation(summary = "查询所有启用中的角色", description = "查询所有状态为启用的角色列表，用于下拉选择等场景")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
	})
	@GetMapping("/active")
	public ApiResponse<List<RoleService.RoleSimple>> activeList() {
		return ApiResponse.success(roleService.listAllActive());
	}

	/**
	 * 新增角色
	 */
	@Operation(summary = "创建角色", description = "创建新角色，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或角色代码已存在"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限")
	})
	@PostMapping
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Map<String, Long>> create(
		@Parameter(description = "创建角色请求", required = true) @Valid @RequestBody RoleCreateRequest request) {
		var command = new RoleService.RoleCommand(
			request.getRoleCode(),
			request.getRoleName(),
			request.getDescription(),
			request.getStatus()
		);
		Long id = roleService.create(command);
		return ApiResponse.success(Map.of("id", id));
	}

	/**
	 * 更新角色信息
	 */
	@Operation(summary = "更新角色信息", description = "更新角色信息，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误或角色代码已存在"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "角色不存在")
	})
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> update(
		@Parameter(description = "角色ID", required = true) @PathVariable Long id,
		@Parameter(description = "更新角色请求", required = true) @Valid @RequestBody RoleUpdateRequest request) {
		var command = new RoleService.RoleCommand(
			request.getRoleCode(),
			request.getRoleName(),
			request.getDescription(),
			request.getStatus()
		);
		roleService.update(id, command);
		return ApiResponse.success();
	}

	/**
	 * 修改角色状态
	 */
	@Operation(summary = "修改角色状态", description = "修改角色状态，0-禁用，1-启用，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "修改成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "状态值非法"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "角色不存在")
	})
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> changeStatus(
		@Parameter(description = "角色ID", required = true) @PathVariable Long id,
		@Parameter(description = "状态更新请求", required = true) @Valid @RequestBody RoleStatusRequest request) {
		var command = new RoleService.RoleCommand(null, null, null, request.getStatus());
		roleService.update(id, command);
		return ApiResponse.success();
	}

	/**
	 * 删除角色
	 */
	@Operation(summary = "删除角色", description = "删除角色，需要系统管理员权限")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "需要系统管理员权限"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "角色不存在")
	})
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ApiResponse<Void> delete(
		@Parameter(description = "角色ID", required = true) @PathVariable Long id) {
		roleService.delete(id);
		return ApiResponse.success();
	}

	@Schema(description = "角色分页查询请求")
	@Validated
	public static class RolePageRequest {
		@Schema(description = "页码，最小为1", example = "1")
		@Min(value = 1, message = "page最小为1")
		private int page = 1;
		@Schema(description = "每页大小，最小为1，最大为100", example = "10")
		@Min(value = 1, message = "size最小为1")
		@Max(value = 100, message = "size最大为100")
		private int size = 10;
		@Schema(description = "关键词，支持角色代码、名称搜索", example = "管理员")
		private String keyword;
		@Schema(description = "角色状态，0-禁用，1-启用", example = "1")
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

	@Schema(description = "创建角色请求")
	public static class RoleCreateRequest {
		@Schema(description = "角色代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "CLUB_ADMIN")
		@NotBlank(message = "角色编码不能为空")
		private String roleCode;
		@Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "社团管理员")
		@NotBlank(message = "角色名称不能为空")
		private String roleName;
		@Schema(description = "角色描述", example = "社团管理员，可管理本社团")
		private String description;
		@Schema(description = "角色状态，0-禁用，1-启用", example = "1")
		@Min(value = 0, message = "状态取值非法")
		@Max(value = 1, message = "状态取值非法")
		private Integer status;

		public String getRoleCode() {
			return roleCode;
		}

		public void setRoleCode(String roleCode) {
			this.roleCode = roleCode;
		}

		public String getRoleName() {
			return roleName;
		}

		public void setRoleName(String roleName) {
			this.roleName = roleName;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}

	@Schema(description = "更新角色请求")
	public static class RoleUpdateRequest {
		@Schema(description = "角色代码", example = "CLUB_ADMIN")
		private String roleCode;
		@Schema(description = "角色名称", example = "社团管理员")
		private String roleName;
		@Schema(description = "角色描述", example = "社团管理员，可管理本社团")
		private String description;
		@Schema(description = "角色状态，0-禁用，1-启用", example = "1")
		@Min(value = 0, message = "状态取值非法")
		@Max(value = 1, message = "状态取值非法")
		private Integer status;

		public String getRoleCode() {
			return roleCode;
		}

		public void setRoleCode(String roleCode) {
			this.roleCode = roleCode;
		}

		public String getRoleName() {
			return roleName;
		}

		public void setRoleName(String roleName) {
			this.roleName = roleName;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}

	@Schema(description = "角色状态更新请求")
	public static class RoleStatusRequest {
		@Schema(description = "角色状态，0-禁用，1-启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
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
}


