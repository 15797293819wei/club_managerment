package com.example.club.controller;

import com.example.club.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口：
 * - GET /api/health：应用可用性
 * - GET /api/health/db：数据库连通性（SELECT 1）
 */
@Tag(name = "健康检查", description = "系统健康检查接口，包括应用可用性和数据库连通性检查")
@RestController
@RequestMapping("/api/health")
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Operation(summary = "应用健康检查", description = "检查应用是否正常运行")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "应用正常")
	})
	@GetMapping
	public ApiResponse<Map<String, Object>> health() {
		Map<String, Object> result = new HashMap<>();
		result.put("app", "ok");
		return ApiResponse.success(result);
	}

	@Operation(summary = "数据库健康检查", description = "检查数据库连接是否正常")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "数据库连接正常"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "数据库连接异常")
	})
	@GetMapping("/db")
	public ApiResponse<Map<String, Object>> db() {
		Map<String, Object> result = new HashMap<>();
		Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		result.put("db", one != null && one == 1 ? "ok" : "fail");
		return ApiResponse.success(result);
	}
}


