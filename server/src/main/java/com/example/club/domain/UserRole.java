package com.example.club.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户与角色关联实体
 *
 * <p>对应表：user_roles（或等价的用户角色关联表）。用于维护用户和角色之间的多对多关系。</p>
 */
@Schema(description = "用户角色关联")
public class UserRole {

	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "用户ID")
	private Long userId;

	@Schema(description = "角色ID")
	private Long roleId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
}

