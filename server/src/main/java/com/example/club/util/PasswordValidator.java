package com.example.club.util;

/**
 * 密码强度校验工具类
 *
 * <p>用于在注册、修改密码、找回密码等场景统一校验密码是否符合安全要求，
 * 并返回直观的校验结果与强度等级。</p>
 *
 * <p>当前规则（可根据需要调整）：
 * <ul>
 *   <li>长度至少 8 位，最多 64 位</li>
 *   <li>必须同时包含：数字、字母（大小写任意）、特殊字符（如 !@#_ 等）中的至少两类</li>
 *   <li>不能全部是同一字符（如 11111111、aaaaaaaa）</li>
 * </ul>
 * </p>
 */
public class PasswordValidator {

	/**
	 * 校验密码强度
	 *
	 * @param password 明文密码
	 * @return 校验结果对象
	 */
	public static PasswordValidationResult validate(String password) {
		if (password == null || password.isBlank()) {
			return PasswordValidationResult.invalid("密码不能为空", PasswordStrength.WEAK);
		}

		String trimmed = password.trim();
		int length = trimmed.length();

		if (length < 8) {
			return PasswordValidationResult.invalid("密码长度不能少于 8 位", PasswordStrength.WEAK);
		}
		if (length > 64) {
			return PasswordValidationResult.invalid("密码长度不能超过 64 位", PasswordStrength.WEAK);
		}

		boolean hasDigit = false;
		boolean hasLetter = false;
		boolean hasUpper = false;
		boolean hasLower = false;
		boolean hasSpecial = false;

		char firstChar = trimmed.charAt(0);
		boolean allSame = true;

		for (int i = 0; i < length; i++) {
			char c = trimmed.charAt(i);

			if (c != firstChar) {
				allSame = false;
			}

			if (Character.isDigit(c)) {
				hasDigit = true;
			} else if (Character.isLetter(c)) {
				hasLetter = true;
				if (Character.isUpperCase(c)) {
					hasUpper = true;
				} else {
					hasLower = true;
				}
			} else {
				// 认为是特殊字符
				hasSpecial = true;
			}
		}

		if (allSame) {
			return PasswordValidationResult.invalid("密码不能为重复的同一字符", PasswordStrength.WEAK);
		}

		int categoryCount = 0;
		if (hasDigit) categoryCount++;
		if (hasLetter) categoryCount++;
		if (hasSpecial) categoryCount++;

		if (categoryCount < 2) {
			return PasswordValidationResult.invalid("密码需要至少包含数字、字母、特殊字符中的两种", PasswordStrength.WEAK);
		}

		// 根据长度与复杂度简单划分强度
		PasswordStrength strength;
		if (length >= 12 && categoryCount >= 3 && hasUpper && hasLower) {
			strength = PasswordStrength.STRONG;
		} else if (length >= 10 && categoryCount >= 2) {
			strength = PasswordStrength.MEDIUM;
		} else {
			strength = PasswordStrength.WEAK;
		}

		return PasswordValidationResult.valid("密码符合安全要求", strength);
	}

	/**
	 * 密码强度等级
	 */
	public enum PasswordStrength {
		WEAK,
		MEDIUM,
		STRONG
	}

	/**
	 * 密码校验结果
	 */
	public static class PasswordValidationResult {
		private final boolean valid;
		private final String message;
		/**
		 * 强度等级，供前端展示。
		 */
		private final PasswordStrength strength;

		private PasswordValidationResult(boolean valid, String message, PasswordStrength strength) {
			this.valid = valid;
			this.message = message;
			this.strength = strength;
		}

		public static PasswordValidationResult valid(String message, PasswordStrength strength) {
			return new PasswordValidationResult(true, message, strength);
		}

		public static PasswordValidationResult invalid(String message, PasswordStrength strength) {
			return new PasswordValidationResult(false, message, strength);
		}

		public boolean isValid() {
			return valid;
		}

		public String getMessage() {
			return message;
		}

		public PasswordStrength getStrength() {
			return strength;
		}
	}
}
