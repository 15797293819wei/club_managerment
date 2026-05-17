package com.example.club.security;

import com.example.club.service.AuthUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 继承自OncePerRequestFilter，确保每个请求只经过一次该过滤器处理
 * 负责从请求头中提取JWT令牌，验证并设置认证信息到Spring Security上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	// JWT服务组件，提供JWT令牌解析功能
	private final JwtService jwtService;

	// 用户详情服务，用于根据用户名加载用户信息
	private final AuthUserDetailsService userDetailsService;

	/**
	 * 构造函数
	 * @param jwtService JWT服务组件
	 * @param userDetailsService 用户详情服务
	 */
	public JwtAuthenticationFilter(JwtService jwtService, AuthUserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	// 执行过滤逻辑
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		// 从请求头获取Authorization字段
		String header = request.getHeader("Authorization");
		// 检查Authorization头是否存在且以Bearer开头
		if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
			// 提取JWT令牌（去掉"Bearer "前缀）
			String token = header.substring(7);
			try {
				// 解析JWT令牌获取声明
				Claims claims = jwtService.parse(token);
				// 从声明中获取用户名
				String username = claims.getSubject();
				// 如果用户名不为空且当前用户未认证
				if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
					// 加载用户详情
					UserDetails userDetails = userDetailsService.loadUserByUsername(username);
					// 创建认证令牌
					UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
					// 设置认证详情
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					// 将认证信息设置到SecurityContext
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			} catch (Exception ignored) {
				// 忽略解析失败，后续鉴权会因无认证信息而拒绝访问受保护接口
			}
		}
		// 继续过滤器链处理
		filterChain.doFilter(request, response);
	}
}


