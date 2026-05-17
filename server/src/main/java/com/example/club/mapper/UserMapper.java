package com.example.club.mapper;

import com.example.club.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 用户映射器接口
 */
@Mapper
public interface UserMapper {
	// 根据用户ID查询用户信息
	Optional<User> selectById(@Param("id") Long id);
	
	// 查询所有用户总数
	Long countAll();

	/**
	 * 分页查询用户集合
	 *
	 * @param keyword 关键词（用户名/真实姓名/邮箱）
	 * @param status  用户状态
	 * @param offset  偏移量
	 * @param size    页面大小
	 * @return 用户列表
	 */
	// 查询用户分页数据
	List<User> selectPage(@Param("keyword") String keyword,
		@Param("username") String username,
		@Param("studentId") String studentId,
		@Param("roleCode") String roleCode,
		@Param("status") Integer status,
		@Param("offset") long offset,
		@Param("size") int size);

	/**
	 * 统计指定筛选条件下的用户数量
	 *
	 * @param keyword 关键词
	 * @param status  用户状态
	 * @return 符合条件的用户数量
	 */
	// 根据筛选条件统计用户数量
	long countByFilters(@Param("keyword") String keyword,
		@Param("username") String username,
		@Param("studentId") String studentId,
		@Param("roleCode") String roleCode,
		@Param("status") Integer status);

	/**
	 * 新增用户
	 *
	 * @param user 用户实体
	 * @return 影响行数
	 */
	// 插入用户
	int insert(User user);

	/**
	 * 根据ID更新用户信息
	 *
	 * @param user 用户实体
	 * @return 影响行数
	 */
	// 更新用户
	int updateById(User user);

	/**
	 * 根据ID删除用户
	 *
	 * @param id 用户ID
	 * @return 影响行数
	 */
	// 删除用户
	int deleteById(@Param("id") Long id);

	/**
	 * 根据ID修改用户状态
	 *
	 * @param id     用户ID
	 * @param status 状态值
	 * @return 影响行数
	 */
	// 更新用户状态
	int updateStatus(@Param("id") Long id, @Param("status") Integer status);

	// 批量更新用户状态
	int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

	// 更新最后登录时间
	int updateLastLoginTime(@Param("id") Long id, @Param("time") java.time.LocalDateTime time);
	
	// 更新密码修改时间
	int updatePasswordChangedTime(@Param("id") Long id, @Param("time") java.time.LocalDateTime time);
	
	// 增加登录失败次数
	int incrementLoginFailureCount(@Param("id") Long id);
	
	// 重置登录失败次数
	int resetLoginFailureCount(@Param("id") Long id);
	
	// 锁定账户
	int lockAccount(@Param("id") Long id, @Param("lockedUntil") java.time.LocalDateTime lockedUntil);
	
	// 设置密码重置令牌
	int setPasswordResetToken(@Param("id") Long id, @Param("token") String token, @Param("expiry") java.time.LocalDateTime expiry);
	
	// 清除密码重置令牌
	int clearPasswordResetToken(@Param("id") Long id);
	
	// 根据密码重置令牌查询用户
	Optional<User> selectByPasswordResetToken(@Param("token") String token);
	
	// 根据邮箱查询用户
	Optional<User> selectByEmail(@Param("email") String email);
	
	// 根据手机号查询用户
	Optional<User> selectByPhone(@Param("phone") String phone);

	/**
	 * 根据用户名判断是否已存在
	 *
	 * @param username 用户名
	 * @return true 表示存在
	 */
	// 查询用户名是否存在
	boolean existsByUsername(@Param("username") String username);

	/**
	 * 根据学号查询用户
	 *
	 * @param studentId 学号
	 * @return 用户信息
	 */
	// 查询学号是否存在
	Optional<User> selectByStudentId(@Param("studentId") String studentId);

	/**
	 * 根据用户名查询用户信息
	 * 主要用于用户登录验证和用户信息查询
	 */
	Optional<User> selectByUsername(@Param("username") String username);

	/**
	 * 根据ID集合批量查询用户
	 *
	 * @param ids 用户ID集合
	 * @return 用户列表
	 */
	List<User> selectByIds(@Param("ids") List<Long> ids);
	
	/**
	 * 根据用户ID查询用户角色编码集合
	 * 用于获取用户的权限信息，为JWT生成和权限校验提供支持
	 */
	Set<String> selectRoleCodesByUserId(@Param("userId") Long userId);

	/**
	 * 根据用户ID集合批量查询用户名
	 * 返回List<Map>，每个Map包含id和username字段
	 *
	 * @param ids 用户ID集合
	 * @return 用户信息列表，每个Map包含id和username
	 */
	List<java.util.Map<String, Object>> selectUsernamesByIds(@Param("ids") List<Long> ids);

	/**
	 * 查询所有启用状态的用户ID
	 */
	List<Long> selectActiveUserIds();
}


