package com.example.club.mapper;

import com.example.club.domain.MemberApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 入社申请映射器接口
 * 提供入社申请数据的增删改查操作
 */
@Mapper
public interface MemberApplicationMapper {
	/**
	 * 根据ID查询申请
	 *
	 * @param id 申请ID
	 * @return 申请信息
	 */
	Optional<MemberApplication> selectById(@Param("id") Long id);

	/**
	 * 根据社团ID和申请人ID查询待审核的申请
	 *
	 * @param clubId     社团ID
	 * @param applicantId 申请人ID
	 * @return 申请信息
	 */
	Optional<MemberApplication> selectPendingByClubIdAndApplicantId(@Param("clubId") Long clubId, @Param("applicantId") Long applicantId);

	/**
	 * 统计符合条件的申请数量
	 *
	 * @param clubId     社团ID（可选）
	 * @param applicantId 申请人ID（可选）
	 * @param status     状态过滤（可选）
	 * @return 总数
	 */
	Long countByFilters(@Param("clubId") Long clubId, @Param("applicantId") Long applicantId, @Param("status") Integer status);

	/**
	 * 分页查询申请列表
	 *
	 * @param clubId     社团ID（可选）
	 * @param applicantId 申请人ID（可选）
	 * @param status     状态过滤（可选）
	 * @param offset     偏移量
	 * @param size       页大小
	 * @return 申请列表
	 */
	List<MemberApplication> selectPage(@Param("clubId") Long clubId, @Param("applicantId") Long applicantId,
									   @Param("status") Integer status, @Param("offset") Long offset, @Param("size") Integer size);

	/**
	 * 插入新申请
	 *
	 * @param application 申请信息
	 */
	void insert(MemberApplication application);

	/**
	 * 更新申请信息
	 *
	 * @param application 申请信息
	 */
	void updateById(MemberApplication application);

	/**
	 * 删除申请
	 *
	 * @param id 申请ID
	 */
	void deleteById(@Param("id") Long id);

	List<MemberApplication> selectRecentByApplicant(@Param("applicantId") Long applicantId,
													@Param("limit") Integer limit);
}

