package com.example.club.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * JWT服务
 * 主要功能：
 * 1. 从配置文件读取JWT密钥和过期时间
 * 2. 生成包含用户信息和自定义声明的JWT令牌
 * 3. 解析并验证JWT令牌，返回令牌中的声明信息
 */
@Service
public class JwtService {
	// JWT签名密钥，从配置文件读取Base64编码的256位密钥
	@Value("${app.jwt.secret}")
	private String secret;

	// JWT令牌过期时间（分钟），默认为120分钟
	@Value("${app.jwt.expireMinutes:120}")
	private long expireMinutes;

	/**
	 * 获取JWT签名密钥
	 * 将Base64编码的密钥解码为字节数组，然后创建HMAC-SHA密钥对象
	 * @return 用于JWT签名和验证的Key对象
	 */
	private Key getSigningKey() {
		// 从Base64编码的配置解码得到密钥字节数组
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		// 创建HS256算法使用的密钥对象
		return Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * 生成JWT令牌
	 * @param username 用户名，作为JWT的subject
	 * @param claims 自定义声明，可包含用户角色等信息
	 * @return 生成的JWT令牌字符串
	 */
	public String generateToken(String username, Map<String, Object> claims) {
		// 获取当前时间戳
		long now = System.currentTimeMillis();
		// 创建签发时间
		Date issuedAt = new Date(now);
		// 计算过期时间
		Date expiration = new Date(now + expireMinutes * 60 * 1000);
		// 构建JWT令牌
		return Jwts.builder()
			.setClaims(claims)
			.setSubject(username)
			.setIssuedAt(issuedAt)
			.setExpiration(expiration)
			// 使用HS256算法签名
			.signWith(getSigningKey(), SignatureAlgorithm.HS256)
			.compact();
	}

	/**
	 * 解析并验证JWT令牌
	 * 验证令牌签名和过期时间，并返回令牌中的声明信息
	 * 如果令牌无效或已过期，将抛出异常
	 * @param token JWT令牌字符串
	 * @return 令牌中的声明信息
	 */
	public Claims parse(String token) {
		// 构建JWT解析器，设置签名密钥
		return Jwts.parserBuilder()
			// 设置用于验证的签名密钥
			.setSigningKey(getSigningKey())
			// 构建解析器
			.build()
			// 解析令牌并验证签名和过期时间
			.parseClaimsJws(token)
			// 获取令牌中的声明主体
			.getBody();
	}
}


