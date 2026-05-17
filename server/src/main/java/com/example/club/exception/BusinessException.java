package com.example.club.exception;

// 业务异常类
public class BusinessException extends RuntimeException {
	//	业务错误码，表示具体的错误类型
	private final int code;

	public BusinessException(int code, String message) {
		super(message);
		this.code = code;
	}

	// 获取业务错误码
	public int getCode() {
		return code;
	}
}


