package com.example.club.mapper;

import com.example.club.domain.SignIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 签到映射器接口
 * 提供签到数据的增删改查操作
 */
@Mapper
public interface SignInMapper {
	/**
	 * 根据ID查询签到
	 *
	 * @param id 签到ID
	 * @return 签到信息
	 */
	Optional<SignIn> selectById(@Param("id") Long id);

	/**
	 * 根据活动ID和用户ID查询签到
	 *
	 * @param activityId 活动ID
	 * @param userId 用户ID
	 * @return 签到信息
	 */
	Optional<SignIn> selectByActivityIdAndUserId(@Param("activityId") Long activityId, @Param("userId") Long userId);

	/**
	 * 统计符合条件的签到数量
	 *
	 * @param activityId 活动ID（可选）
	 * @param userId 用户ID（可选）
	 * @return 总数
	 */
	Long countByFilters(@Param("activityId") Long activityId, @Param("userId") Long userId);

	/**
	 * 分页查询签到列表
	 *
	 * @param activityId 活动ID（可选）
	 * @param userId 用户ID（可选）
	 * @param offset 偏移量
	 * @param size   页大小
	 * @return 签到列表
	 */
	List<SignIn> selectPage(@Param("activityId") Long activityId, @Param("userId") Long userId,
							@Param("offset") Long offset, @Param("size") Integer size);

	/**
	 * 插入新签到
	 *
	 * @param signIn 签到信息
	 */
	void insert(SignIn signIn);

	/**
	 * 删除签到
	 *
	 * @param id 签到ID
	 */
	void deleteById(@Param("id") Long id);
}

