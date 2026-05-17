package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成员实体类
 * 用于存储社团成员信息，包括成员角色、加入时间、状态等
 */
@Data
public class Member {
	// 成员ID，主键
	private Long id;
	// 社团ID
	private Long clubId;
	// 用户ID
	private Long userId;
	// 成员角色（MEMBER-普通成员，STAFF-干事，VICE_MINISTER-副部长，MINISTER-部长）
	private String role;
	// 加入时间
	private LocalDateTime joinTime;
	// 状态（0-已退出，1-正常）
	private Integer status;
	// 创建时间
	private LocalDateTime createdTime;
	// 更新时间
	private LocalDateTime updatedTime;
}

