package com.example.club.service;

import com.example.club.domain.User;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.UserMapper;
import com.example.club.util.PasswordValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 密码重置服务
 * 处理忘记密码、密码重置等功能
 */
@Service
public class PasswordResetService {
	
	private static final int MAX_LOGIN_FAILURES = 5; // 最大登录失败次数
	private static final int LOCK_DURATION_MINUTES = 30; // 账户锁定时长（分钟）
	private static final int PASSWORD_RESET_TOKEN_EXPIRY_HOURS = 24; // 密码重置令牌有效期（小时）
	
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	
	public PasswordResetService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}
	
	/**
	 * 请求密码重置（通过邮箱或手机号）
	 * 
	 * @param email 邮箱（可选）
	 * @param phone 手机号（可选）
	 * @return 密码重置令牌（实际应用中应该通过邮件/短信发送，这里仅返回用于测试）
	 */
	@Transactional
	public String requestPasswordReset(String username, String email, String phone) {
		if (username == null || username.trim().isEmpty()) {
			throw new BusinessException(40001, "用户名不能为空");
		}
		if ((email == null || email.trim().isEmpty()) &&
			(phone == null || phone.trim().isEmpty())) {
			throw new BusinessException(40001, "邮箱或手机号至少提供一个");
		}
		
		String normalizedUsername = username.trim();
		User user = userMapper.selectByUsername(normalizedUsername)
			.orElseThrow(() -> new BusinessException(40404, "该账号未注册"));
		
		if (email != null && !email.trim().isEmpty()) {
			var emailUserOpt = userMapper.selectByEmail(email.trim());
			if (emailUserOpt.isEmpty()) {
				throw new BusinessException(40405, "该邮箱未注册");
			}
			if (!emailUserOpt.get().getId().equals(user.getId())) {
				throw new BusinessException(40305, "账号与申述邮箱不匹配");
			}
		}
		
		if (phone != null && !phone.trim().isEmpty()) {
			var phoneUserOpt = userMapper.selectByPhone(phone.trim());
			if (phoneUserOpt.isEmpty()) {
				throw new BusinessException(40406, "该手机号未注册");
			}
			if (!phoneUserOpt.get().getId().equals(user.getId())) {
				throw new BusinessException(40306, "账号与申述手机号不匹配");
			}
		}
		
		// 生成密码重置令牌
		String token = UUID.randomUUID().toString().replace("-", "");
		LocalDateTime expiry = LocalDateTime.now().plusHours(PASSWORD_RESET_TOKEN_EXPIRY_HOURS);
		
		// 保存令牌
		userMapper.setPasswordResetToken(user.getId(), token, expiry);
		
		// 实际应用中这里应该发送邮件或短信
		// emailService.sendPasswordResetEmail(user.getEmail(), token);
		// smsService.sendPasswordResetSMS(user.getPhone(), token);
		
		return token; // 仅用于测试，实际应用中不应该返回
	}
	
	/**
	 * 通过令牌重置密码
	 * 
	 * @param token 密码重置令牌
	 * @param newPassword 新密码
	 */
	@Transactional
	public void resetPassword(String token, String newPassword) {
		if (token == null || token.trim().isEmpty()) {
			throw new BusinessException(40001, "重置令牌不能为空");
		}
		
		// 验证密码强度
		PasswordValidator.PasswordValidationResult validation = PasswordValidator.validate(newPassword);
		if (!validation.isValid()) {
			throw new BusinessException(40002, validation.getMessage());
		}
		
		// 查询用户
		Optional<User> userOpt = userMapper.selectByPasswordResetToken(token.trim());
		if (userOpt.isEmpty()) {
			throw new BusinessException(40003, "重置令牌无效或已过期");
		}
		
		User user = userOpt.get();
		
		// 加密新密码
		String encodedPassword = passwordEncoder.encode(newPassword);
		
		// 更新密码
		user.setPassword(encodedPassword);
		user.setPasswordChangedTime(LocalDateTime.now());
		userMapper.updateById(user);
		userMapper.updatePasswordChangedTime(user.getId(), LocalDateTime.now());
		
		// 清除重置令牌
		userMapper.clearPasswordResetToken(user.getId());
		
		// 重置登录失败次数
		userMapper.resetLoginFailureCount(user.getId());
	}
	
	/**
	 * 检查账户是否被锁定
	 * 
	 * @param user 用户
	 * @return 如果被锁定，返回锁定截止时间；否则返回null
	 */
	public LocalDateTime checkAccountLocked(User user) {
		if (user.getAccountLockedUntil() != null && 
			user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
			return user.getAccountLockedUntil();
		}
		return null;
	}
	
	/**
	 * 处理登录失败
	 * 
	 * @param userId 用户ID
	 */
	@Transactional
	public void handleLoginFailure(Long userId) {
		userMapper.incrementLoginFailureCount(userId);
		
		Optional<User> userOpt = userMapper.selectById(userId);
		if (userOpt.isPresent()) {
			User user = userOpt.get();
			int failureCount = user.getLoginFailureCount() != null ? user.getLoginFailureCount() : 0;
			
			// 如果失败次数达到阈值，锁定账户
			if (failureCount >= MAX_LOGIN_FAILURES) {
				LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
				userMapper.lockAccount(userId, lockedUntil);
			}
		}
	}
	
	/**
	 * 处理登录成功
	 * 
	 * @param userId 用户ID
	 */
	@Transactional
	public void handleLoginSuccess(Long userId) {
		// 重置登录失败次数
		userMapper.resetLoginFailureCount(userId);
	}
	
}

