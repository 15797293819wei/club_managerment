package com.example.club.mapper;

import com.example.club.domain.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 成员映射器接口
 * 提供成员数据的增删改查操作
 */
@Mapper
public interface MemberMapper {
	/**
	 * 根据ID查询成员
	 *
	 * @param id 成员ID
	 * @return 成员信息
	 */
	Optional<Member> selectById(@Param("id") Long id);

	/**
	 * 根据社团ID和用户ID查询成员
	 *
	 * @param clubId 社团ID
	 * @param userId 用户ID
	 * @return 成员信息
	 */
	Optional<Member> selectByClubIdAndUserId(@Param("clubId") Long clubId, @Param("userId") Long userId);

	/**
	 * 统计符合条件的成员数量
	 *
	 * @param clubId 社团ID（可选）
	 * @param userId 用户ID（可选）
	 * @param role   角色过滤（可选）
	 * @param status 状态过滤（可选）
	 * @return 总数
	 */
	Long countByFilters(@Param("clubId") Long clubId, @Param("userId") Long userId,
						@Param("role") String role, @Param("status") Integer status);

	/**
	 * 分页查询成员列表
	 *
	 * @param clubId 社团ID（可选）
	 * @param userId 用户ID（可选）
	 * @param role   角色过滤（可选）
	 * @param status 状态过滤（可选）
	 * @param offset 偏移量
	 * @param size   页大小
	 * @return 成员列表
	 */
	List<Member> selectPage(@Param("clubId") Long clubId, @Param("userId") Long userId,
							@Param("role") String role, @Param("status") Integer status,
							@Param("offset") Long offset, @Param("size") Integer size);

	/**
	 * 插入新成员
	 *
	 * @param member 成员信息
	 */
	void insert(Member member);

	/**
	 * 更新成员信息
	 *
	 * @param member 成员信息
	 */
	void updateById(Member member);

	/**
	 * 更新成员状态
	 *
	 * @param id     成员ID
	 * @param status 状态值
	 */
	void updateStatus(@Param("id") Long id, @Param("status") Integer status);

	/**
	 * 更新成员角色
	 *
	 * @param id   成员ID
	 * @param role 角色
	 */
	void updateRole(@Param("id") Long id, @Param("role") String role);

	/**
	 * 删除成员
	 *
	 * @param id 成员ID
	 */
	void deleteById(@Param("id") Long id);

	/**
	 * 批量更新指定社团成员状态
	 *
	 * @param clubId 社团ID
	 * @param status 状态值
	 */
	void updateStatusByClubId(@Param("clubId") Long clubId, @Param("status") Integer status);

	List<Member> selectByIds(@Param("ids") List<Long> ids);

	List<Member> selectList(@Param("clubId") Long clubId,
							@Param("userId") Long userId,
							@Param("role") String role,
							@Param("status") Integer status);

	List<Map<String, Object>> countRoleDistribution(@Param("clubId") Long clubId);

	Long countNewMembers(@Param("clubId") Long clubId, @Param("since") LocalDateTime since);

	Long countActiveMembers(@Param("clubId") Long clubId, @Param("since") LocalDateTime since);

	void updateRoleBatch(@Param("ids") List<Long> ids, @Param("role") String role);

	void updateStatusBatch(@Param("ids") List<Long> ids, @Param("status") Integer status);

	void deleteByIds(@Param("ids") List<Long> ids);

	List<Long> selectActiveClubIdsByUserId(@Param("userId") Long userId);

	List<Long> selectActiveUserIdsByClubId(@Param("clubId") Long clubId);
}

