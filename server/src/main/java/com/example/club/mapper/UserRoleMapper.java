package com.example.club.mapper;

import com.example.club.domain.UserRole;
import com.example.club.domain.UserRoleRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 用户角色关联数据访问层
 * <p>负责维护用户与角色之间的多对多关系。</p>
 */
@Mapper
public interface UserRoleMapper {
	/**
	 * 新增单条关联记录
	 *
	 * @param userRole 关联实体
	 * @return 影响行数
	 */
	int insert(UserRole userRole);

	/**
	 * 批量插入用户角色关联
	 *
	 * @param userId  用户ID
	 * @param roleIds 角色ID集合
	 * @return 影响行数
	 */
	int insertBatch(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

	/**
	 * 根据用户ID删除关联
	 *
	 * @param userId 用户ID
	 * @return 影响行数
	 */
	int deleteByUserId(@Param("userId") Long userId);

	/**
	 * 统计某角色被分配的数量
	 *
	 * @param roleId 角色ID
	 * @return 绑定数量
	 */
	long countByRoleId(@Param("roleId") Long roleId);

	/**
	 * 查询用户绑定的角色ID集合
	 *
	 * @param userId 用户ID
	 * @return 角色ID集合
	 */
	Set<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

	/**
	 * 查询用户与角色的详细信息集合
	 *
	 * @param userIds 用户ID集合
	 * @return 用户角色详情列表
	 */
	List<UserRoleRelation> selectRoleInfoByUserIds(@Param("userIds") List<Long> userIds);

	/**
	 * 删除用户与指定角色的关联
	 *
	 * @param userId 用户ID
	 * @param roleId 角色ID
	 * @return 影响行数
	 */
	int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}


