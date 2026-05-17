package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体类
 * 用于存储系统角色信息，包括编码、名称、描述及状态等数据
 */
@Data
public class Role {
	// 角色ID，主键
	private Long id;
	// 角色编码（唯一）
	private String roleCode;
	// 角色名称
	private String roleName;
	// 角色描述
	private String description;
	// 角色状态（0-禁用，1-启用）
	private Integer status;
	// 创建时间
	private LocalDateTime createdTime;
	// 更新时间
	private LocalDateTime updatedTime;
}


