package com.example.club.service;

import com.example.club.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * 认证用户详情服务
 * 实现Spring Security的UserDetailsService接口，为安全框架提供用户认证所需的用户详情
 * 负责从数据库加载用户信息，并将其转换为Spring Security可识别的UserDetails对象
 */
@Service
public class AuthUserDetailsService implements UserDetailsService {

	// 用户映射器，用于从数据库查询用户信息和角色
	private final UserMapper userMapper;
	
	/**
	 * 构造函数
	 * @param userMapper 用户映射器对象
	 */
	 public AuthUserDetailsService(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	/**
	 * 根据用户名加载用户详情
	 * @param username 用户名
	 * @return UserDetails对象，包含用户的认证信息和权限列表
	 * @throws UsernameNotFoundException 当用户名不存在时抛出异常
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// 根据用户名查询用户信息，如果不存在则抛出异常
		var dbUser = userMapper.selectByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
		// 查询用户的角色编码列表
		var roles = userMapper.selectRoleCodesByUserId(dbUser.getId());
		// 将角色编码转换为Spring Security的授权对象，添加ROLE_前缀
		var authorities = roles.stream()
			.map(rc -> new SimpleGrantedAuthority("ROLE_" + rc))
			.collect(Collectors.toSet());
		// 根据用户状态判断账号是否可用（status=1表示可用）
		boolean enabled = dbUser.getStatus() != null && dbUser.getStatus() == 1;
		// 检查账户是否被锁定
		boolean accountNonLocked = true;
		if (dbUser.getAccountLockedUntil() != null && 
			dbUser.getAccountLockedUntil().isAfter(java.time.LocalDateTime.now())) {
			accountNonLocked = false;
		}
		// 创建并返回UserDetails对象
		// 参数说明：用户名、密码、是否可用、是否过期、凭证是否过期、是否锁定、权限列表
		return new User(dbUser.getUsername(), dbUser.getPassword(), enabled, true, true, accountNonLocked, authorities);
	}
}


