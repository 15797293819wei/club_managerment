package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 用于存储系统用户信息，包括基本个人信息、账户状态等
 */
@Data
public class User {
	// 用户ID，主键
	private Long id;
	// 用户名，登录账号
	private String username;
	// 密码，加密存储
	private String password;
	// 最近一次密码修改时间
	private LocalDateTime passwordChangedTime;
	// 真实姓名
	private String realName;
	// 学号/工号
	private String studentId;
	// 电子邮箱
	private String email;
	// 手机号码
	private String phone;
	// 性别（0-未知，1-男，2-女）
	private Integer gender;
	// 头像URL
	private String avatar;
	// 账户状态（0-禁用，1-启用）
	private Integer status;
	// 最后登录时间
	private LocalDateTime lastLoginTime;
	// 连续登录失败次数
	private Integer loginFailureCount;
	// 账户锁定截止时间（锁定到期前无法登录）
	private LocalDateTime accountLockedUntil;
	// 密码重置令牌
	private String passwordResetToken;
	// 密码重置令牌过期时间
	private LocalDateTime passwordResetTokenExpiry;
	// 创建时间
	private LocalDateTime createdTime;
	// 更新时间
	private LocalDateTime updatedTime;
}


