package com.example.club.config;

import com.example.club.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security安全配置类
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	// JWT认证过滤器
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	/** 
	 * @param jwtAuthenticationFilter JWT认证过滤器实例
	 */
	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	/**
	 * 配置安全过滤链
	 * <p>
	 * 定义系统的安全规则，包括CSRF配置、会话管理、请求授权和过滤器链
	 * </p>
	 * 
	 * @param http HttpSecurity配置对象
	 * @return 配置完成的SecurityFilterChain
	 * @throws Exception 配置过程中可能出现的异常
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// 禁用CSRF，API使用Token鉴权
		http.csrf(csrf -> csrf.disable());
		// 采用无状态会话，不使用HttpSession
		http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		// 路由权限策略配置
		http.authorizeHttpRequests(auth -> auth
			// 允许访问健康检查和认证接口，无需认证
			.requestMatchers("/api/health/**", "/api/auth/**").permitAll()
			// 允许访问文件（读取文件无需认证，上传需要认证）
			.requestMatchers("/api/files/**").permitAll()
			// 允许访问Swagger UI和API文档
			.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
			// 其他所有请求都需要认证
			.anyRequest().authenticated()
		);
		// 在UsernamePasswordAuthenticationFilter之前添加JWT认证过滤器
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * 配置密码加密器
	 * 使用BCrypt算法进行密码加密，提供安全的密码存储
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * 配置认证管理器
	 * 用于处理认证请求，验证用户名和密码
	 * 
	 * @param configuration 认证配置对象
	 * @return AuthenticationManager实例
	 * @throws Exception 获取认证管理器过程中可能出现的异常
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}


