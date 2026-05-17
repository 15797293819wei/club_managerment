package com.example.club.mapper;

import com.example.club.domain.ClubApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 社团申请映射器接口
 * 提供社团申请数据的增删改查操作
 */
@Mapper
public interface ClubApplicationMapper {
	/**
	 * 根据ID查询申请
	 *
	 * @param id 申请ID
	 * @return 申请信息
	 */
	Optional<ClubApplication> selectById(@Param("id") Long id);

	/**
	 * 统计符合条件的申请数量
	 *
	 * @param keyword     关键词（社团名称）
	 * @param status      状态过滤
	 * @param applicantId 申请人ID过滤（可选）
	 * @return 总数
	 */
	Long countByFilters(@Param("keyword") String keyword, @Param("status") Integer status, @Param("applicantId") Long applicantId);

	/**
	 * 分页查询申请列表
	 *
	 * @param keyword     关键词
	 * @param status      状态过滤
	 * @param applicantId 申请人ID过滤（可选）
	 * @param offset      偏移量
	 * @param size        页大小
	 * @return 申请列表
	 */
	List<ClubApplication> selectPage(@Param("keyword") String keyword, @Param("status") Integer status, @Param("applicantId") Long applicantId, @Param("offset") Long offset, @Param("size") Integer size);

	/**
	 * 插入新申请
	 *
	 * @param application 申请信息
	 */
	void insert(ClubApplication application);

	/**
	 * 更新申请信息
	 *
	 * @param application 申请信息
	 */
	void updateById(ClubApplication application);

	/**
	 * 删除申请
	 *
	 * @param id 申请ID
	 */
	void deleteById(@Param("id") Long id);

	Long countPendingByApplicant(@Param("applicantId") Long applicantId);
}

