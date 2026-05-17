package com.example.club.mapper;

import com.example.club.domain.Club;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 社团映射器接口
 * 提供社团数据的增删改查操作
 */
@Mapper
public interface ClubMapper {
	/**
	 * 根据ID查询社团
	 *
	 * @param id 社团ID
	 * @return 社团信息
	 */
	Optional<Club> selectById(@Param("id") Long id);

	/**
	 * 根据社团名称查询社团
	 *
	 * @param clubName 社团名称
	 * @return 社团信息
	 */
	Optional<Club> selectByClubName(@Param("clubName") String clubName);

	/**
	 * 根据社团代码查询社团
	 *
	 * @param clubCode 社团代码
	 * @return 社团信息
	 */
	Optional<Club> selectByClubCode(@Param("clubCode") String clubCode);

	/**
	 * 统计符合条件的社团数量
	 *
	 * @param keyword 关键词（社团名称、代码）
	 * @param status  状态过滤
	 * @param founderId 创始人ID过滤（可选）
	 * @return 总数
	 */
	Long countByFilters(@Param("keyword") String keyword,
		@Param("status") Integer status,
		@Param("founderId") Long founderId,
		@Param("minMemberCount") Integer minMemberCount,
		@Param("maxMemberCount") Integer maxMemberCount);

	/**
	 * 分页查询社团列表
	 *
	 * @param keyword  关键词
	 * @param status   状态过滤
	 * @param founderId 创始人ID过滤（可选）
	 * @param offset   偏移量
	 * @param size     页大小
	 * @return 社团列表
	 */
	List<Club> selectPage(@Param("keyword") String keyword,
		@Param("status") Integer status,
		@Param("founderId") Long founderId,
		@Param("minMemberCount") Integer minMemberCount,
		@Param("maxMemberCount") Integer maxMemberCount,
		@Param("sortField") String sortField,
		@Param("sortOrder") String sortOrder,
		@Param("offset") Long offset,
		@Param("size") Integer size);

	/**
	 * 插入新社团
	 *
	 * @param club 社团信息
	 */
	void insert(Club club);

	/**
	 * 更新社团信息
	 *
	 * @param club 社团信息
	 */
	void updateById(Club club);

	/**
	 * 更新社团状态
	 *
	 * @param id     社团ID
	 * @param status 状态值
	 */
	void updateStatus(@Param("id") Long id, @Param("status") Integer status);

	/**
	 * 更新成员数量
	 *
	 * @param id          社团ID
	 * @param memberCount 成员数量
	 */
	void updateMemberCount(@Param("id") Long id, @Param("memberCount") Integer memberCount);

	/**
	 * 删除社团
	 *
	 * @param id 社团ID
	 */
	void deleteById(@Param("id") Long id);

	/**
	 * 更新社团管理员（创始人）为指定用户
	 *
	 * @param id        社团ID
	 * @param founderId 新的管理员用户ID
	 */
	void updateFounder(@Param("id") Long id, @Param("founderId") Long founderId);

	/**
	 * 统计指定用户当前担任社团管理员的数量
	 *
	 * @param founderId 用户ID
	 * @return 社团数量
	 */
	Long countByFounderId(@Param("founderId") Long founderId);

	/**
	 * 根据创始人ID查询社团列表
	 *
	 * @param founderId 创始人ID
	 * @return 社团列表
	 */
	List<Club> selectByFounderId(@Param("founderId") Long founderId);

	/**
	 * 根据社长ID查询社团列表
	 *
	 * @param presidentId 社长ID
	 * @return 社团列表
	 */
	List<Club> selectByPresidentId(@Param("presidentId") Long presidentId);

	List<ClubLeaderboardRow> selectLeaderboardByMemberCount(@Param("limit") int limit);

	List<ClubLeaderboardRow> selectLeaderboardByActivityCount(@Param("limit") int limit);

	@Data
	class ClubLeaderboardRow {
		private Long clubId;
		private String clubName;
		private Long metricValue;
	}
}

