package com.example.club.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
	/**
	 * 业务状态码
	 * 0表示成功，非0表示失败或特定业务状态
	 */
	private int code;
	
	/**
	 * 响应消息
	 * 用于描述操作结果的文本信息
	 */
	private String message;
	
	/**
	 * 返回数据
	 * 当data为null时不包含在JSON响应中
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private T data;

	// 创建成功响应（带数据）
	public static <T> ApiResponse<T> success(T data) {
		return ApiResponse.<T>builder()
			.code(0)
			.message("success")
			.data(data)
			.build();
	}

	// 创建成功响应（无数据）
	public static <T> ApiResponse<T> success() {
		return success(null);
	}

	// 创建错误响应
	public static <T> ApiResponse<T> error(int code, String message) {
		return ApiResponse.<T>builder()
			.code(code)
			.message(message)
			.build();
	}
}


