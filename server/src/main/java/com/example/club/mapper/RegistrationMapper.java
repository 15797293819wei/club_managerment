package com.example.club.mapper;

import com.example.club.domain.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 报名映射器接口
 * 提供报名数据的增删改查操作
 */
@Mapper
public interface RegistrationMapper {
	/**
	 * 根据ID查询报名
	 *
	 * @param id 报名ID
	 * @return 报名信息
	 */
	Optional<Registration> selectById(@Param("id") Long id);

	/**
	 * 根据活动ID和用户ID查询报名
	 *
	 * @param activityId 活动ID
	 * @param userId 用户ID
	 * @return 报名信息
	 */
	Optional<Registration> selectByActivityIdAndUserId(@Param("activityId") Long activityId, @Param("userId") Long userId);

	/**
	 * 统计符合条件的报名数量
	 *
	 * @param activityId 活动ID（可选）
	 * @param userId 用户ID（可选）
	 * @param status 状态过滤（可选）
	 * @return 总数
	 */
	Long countByFilters(@Param("activityId") Long activityId, @Param("userId") Long userId, @Param("status") Integer status);

	/**
	 * 分页查询报名列表
	 *
	 * @param activityId 活动ID（可选）
	 * @param userId 用户ID（可选）
	 * @param status 状态过滤（可选）
	 * @param offset 偏移量
	 * @param size   页大小
	 * @return 报名列表
	 */
	List<Registration> selectPage(@Param("activityId") Long activityId, @Param("userId") Long userId,
								  @Param("status") Integer status, @Param("offset") Long offset, @Param("size") Integer size);

	/**
	 * 插入新报名
	 *
	 * @param registration 报名信息
	 */
	void insert(Registration registration);

	/**
	 * 更新报名信息
	 *
	 * @param registration 报名信息
	 */
	void updateById(Registration registration);

	/**
	 * 更新报名状态
	 *
	 * @param id     报名ID
	 * @param status 状态值
	 */
	void updateStatus(@Param("id") Long id, @Param("status") Integer status);

	/**
	 * 删除报名
	 *
	 * @param id 报名ID
	 */
	void deleteById(@Param("id") Long id);
}

