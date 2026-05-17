package com.example.club.mapper;

import com.example.club.domain.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 活动映射器接口
 * 提供活动数据的增删改查操作
 */
@Mapper
public interface ActivityMapper {
	/**
	 * 根据ID查询活动
	 *
	 * @param id 活动ID
	 * @return 活动信息
	 */
	Optional<Activity> selectById(@Param("id") Long id);

	/**
	 * 统计符合条件的活动数量
	 *
	 * @param clubId 社团ID（可选）
	 * @param clubName 社团名称关键词（可选，用于搜索社团名称）
	 * @param keyword 关键词（可选，用于搜索活动名称）
	 * @param status 状态过滤（可选）
	 * @return 总数
	 */
	Long countByFilters(@Param("clubId") Long clubId,
						@Param("clubIds") List<Long> clubIds,
						@Param("clubName") String clubName,
						@Param("keyword") String keyword,
						@Param("status") Integer status);

	default Long countByFilters(Long clubId,
								String clubName,
								String keyword,
								Integer status) {
		return countByFilters(clubId, null, clubName, keyword, status);
	}

	/**
	 * 分页查询活动列表
	 *
	 * @param clubId 社团ID（可选）
	 * @param clubName 社团名称关键词（可选）
	 * @param keyword 关键词（可选）
	 * @param status 状态过滤（可选）
	 * @param offset 偏移量
	 * @param size   页大小
	 * @return 活动列表
	 */
	List<Activity> selectPage(@Param("clubId") Long clubId,
							  @Param("clubIds") List<Long> clubIds,
							  @Param("clubName") String clubName,
							  @Param("keyword") String keyword,
							  @Param("status") Integer status,
							  @Param("offset") Long offset,
							  @Param("size") Integer size);

	default List<Activity> selectPage(Long clubId,
									  String clubName,
									  String keyword,
									  Integer status,
									  Long offset,
									  Integer size) {
		return selectPage(clubId, null, clubName, keyword, status, offset, size);
	}

	/**
	 * 插入新活动
	 *
	 * @param activity 活动信息
	 */
	void insert(Activity activity);

	/**
	 * 更新活动信息
	 *
	 * @param activity 活动信息
	 */
	void updateById(Activity activity);

	/**
	 * 更新活动状态
	 *
	 * @param id     活动ID
	 * @param status 状态值
	 */
	void updateStatus(@Param("id") Long id, @Param("status") Integer status);

	/**
	 * 更新当前参与人数
	 *
	 * @param id 活动ID
	 * @param delta 变化量（正数增加，负数减少）
	 */
	void updateCurrentParticipants(@Param("id") Long id, @Param("delta") Integer delta);

	/**
	 * 删除活动
	 *
	 * @param id 活动ID
	 */
	void deleteById(@Param("id") Long id);
}

