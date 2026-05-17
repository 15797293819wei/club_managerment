package com.example.club.exception;

import com.example.club.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * 处理业务异常
	 * 捕获BusinessException类型的异常，提取业务错误码和消息返回给客户端
	 */
	@ExceptionHandler(BusinessException.class)
	public ApiResponse<Void> handleBusiness(BusinessException ex) {
		return ApiResponse.error(ex.getCode(), ex.getMessage());
	}

	/**
	 * 处理参数验证异常
	 * 捕获请求参数验证失败的异常，包括MethodArgumentNotValidException和BindException
	 * 尝试提取具体的字段错误信息，如果无法提取则返回通用错误消息
	 */
	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ApiResponse<Void> handleValidation(Exception ex) {
		String message = "参数校验失败";
		// 处理请求体参数验证异常
		if (ex instanceof MethodArgumentNotValidException manve && manve.getBindingResult().getFieldError() != null) {
			message = manve.getBindingResult().getFieldError().getDefaultMessage();
		}
		// 处理表单参数绑定异常
		else if (ex instanceof BindException be && be.getFieldError() != null) {
			message = be.getFieldError().getDefaultMessage();
		}
		return ApiResponse.error(400, message);
	}

	/**
	 * 处理请求错误异常
	 * 捕获各类请求格式错误的异常，包括：
	 * - ConstraintViolationException：请求参数违反约束
	 * - HttpMessageNotReadableException：请求体格式错误
	 * - IllegalArgumentException：非法参数
	 */
	@ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
	public ApiResponse<Void> handleBadRequest(Exception ex) {
		return ApiResponse.error(400, ex.getMessage());
	}

	/**
	 * 处理未知异常
	 * 捕获所有其他未明确处理的异常，返回通用的服务器错误信息
	 * 避免将敏感的异常详情泄露给客户端
	 */
	@ExceptionHandler(Exception.class)
	public ApiResponse<Void> handleUnknown(Exception ex) {
		// 记录具体异常，方便排查 500 错误
		log.error("Unhandled exception caught by GlobalExceptionHandler", ex);
		return ApiResponse.error(500, "服务器开小差了，请稍后重试");
	}
}


