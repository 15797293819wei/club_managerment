package com.example.club.mapper;

import com.example.club.domain.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 评价映射器接口
 * 提供评价数据的增删改查操作
 */
@Mapper
public interface EvaluationMapper {
	/**
	 * 根据ID查询评价
	 *
	 * @param id 评价ID
	 * @return 评价信息
	 */
	Optional<Evaluation> selectById(@Param("id") Long id);

	/**
	 * 根据活动ID和用户ID查询评价
	 *
	 * @param activityId 活动ID
	 * @param userId 用户ID
	 * @return 评价信息
	 */
	Optional<Evaluation> selectByActivityIdAndUserId(@Param("activityId") Long activityId, @Param("userId") Long userId);

	/**
	 * 统计符合条件的评价数量
	 *
	 * @param activityId 活动ID（可选）
	 * @param userId 用户ID（可选）
	 * @return 总数
	 */
	Long countByFilters(@Param("activityId") Long activityId, @Param("userId") Long userId);

	/**
	 * 分页查询评价列表
	 *
	 * @param activityId 活动ID（可选）
	 * @param userId 用户ID（可选）
	 * @param offset 偏移量
	 * @param size   页大小
	 * @return 评价列表
	 */
	List<Evaluation> selectPage(@Param("activityId") Long activityId, @Param("userId") Long userId,
								 @Param("offset") Long offset, @Param("size") Integer size);

	/**
	 * 插入新评价
	 *
	 * @param evaluation 评价信息
	 */
	void insert(Evaluation evaluation);

	/**
	 * 更新评价信息
	 *
	 * @param evaluation 评价信息
	 */
	void updateById(Evaluation evaluation);

	/**
	 * 删除评价
	 *
	 * @param id 评价ID
	 */
	void deleteById(@Param("id") Long id);

	/**
	 * 统计活动的平均评分
	 *
	 * @param activityId 活动ID
	 * @return 平均评分
	 */
	Double selectAverageRating(@Param("activityId") Long activityId);

	/**
	 * 统计活动的评价数量
	 *
	 * @param activityId 活动ID
	 * @return 评价数量
	 */
	Long countByActivityId(@Param("activityId") Long activityId);
}

