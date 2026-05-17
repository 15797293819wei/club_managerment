package com.example.club.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户与角色的关联视图对象
 *
 * <p>通常用于联合查询用户与角色信息时的结果映射。</p>
 */
@Schema(description = "用户角色关联视图")
public class UserRoleRelation {

	@Schema(description = "用户ID")
	private Long userId;

	@Schema(description = "用户名")
	private String username;

	@Schema(description = "角色ID")
	private Long roleId;

	@Schema(description = "角色编码")
	private String roleCode;

	@Schema(description = "角色名称")
	private String roleName;

	@Schema(description = "角色状态，0-禁用，1-启用")
	private Integer roleStatus;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

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

	public Integer getRoleStatus() {
		return roleStatus;
	}

	public void setRoleStatus(Integer roleStatus) {
		this.roleStatus = roleStatus;
	}
}

