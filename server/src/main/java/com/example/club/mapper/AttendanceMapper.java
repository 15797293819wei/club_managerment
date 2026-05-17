package com.example.club.mapper;

import com.example.club.domain.Attendance;
import com.example.club.dto.attendance.AttendanceStatsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考勤映射器接口
 * 提供考勤数据的增删改查操作
 */
@Mapper
public interface AttendanceMapper {
	/**
	 * 根据ID查询考勤记录
	 *
	 * @param id 考勤ID
	 * @return 考勤信息
	 */
	Attendance selectById(@Param("id") Long id);

	/**
	 * 统计符合条件的考勤记录数量
	 *
	 * @param clubId     社团ID（可选）
	 * @param activityId 活动ID（可选）
	 * @param memberId   成员ID（可选）
	 * @return 总数
	 */
	Long countByFilters(@Param("clubId") Long clubId,
						@Param("activityId") Long activityId,
						@Param("memberId") Long memberId,
						@Param("startTime") LocalDateTime startTime,
						@Param("endTime") LocalDateTime endTime);

	/**
	 * 分页查询考勤记录列表
	 *
	 * @param clubId     社团ID（可选）
	 * @param activityId 活动ID（可选）
	 * @param memberId   成员ID（可选）
	 * @param offset     偏移量
	 * @param size       页大小
	 * @return 考勤记录列表
	 */
	List<Attendance> selectPage(@Param("clubId") Long clubId,
								@Param("activityId") Long activityId,
								@Param("memberId") Long memberId,
								@Param("startTime") LocalDateTime startTime,
								@Param("endTime") LocalDateTime endTime,
								@Param("offset") Long offset,
								@Param("size") Integer size);

	/**
	 * 根据活动与成员查询单条考勤
	 */
	Attendance selectByActivityIdAndMemberId(@Param("activityId") Long activityId,
											 @Param("memberId") Long memberId);

	/**
	 * 插入新考勤记录
	 *
	 * @param attendance 考勤信息
	 */
	void insert(Attendance attendance);

	List<Attendance> selectForExport(@Param("clubId") Long clubId,
									 @Param("activityId") Long activityId,
									 @Param("memberId") Long memberId,
									 @Param("startTime") LocalDateTime startTime,
									 @Param("endTime") LocalDateTime endTime);

	AttendanceStatsRow selectSummary(@Param("clubId") Long clubId,
									 @Param("activityId") Long activityId,
									 @Param("memberId") Long memberId,
									 @Param("startTime") LocalDateTime startTime,
									 @Param("endTime") LocalDateTime endTime);

	List<AttendanceStatsRow> selectStatsByMember(@Param("clubId") Long clubId,
												 @Param("startTime") LocalDateTime startTime,
												 @Param("endTime") LocalDateTime endTime);

	List<AttendanceStatsRow> selectStatsByActivity(@Param("clubId") Long clubId,
												   @Param("startTime") LocalDateTime startTime,
												   @Param("endTime") LocalDateTime endTime);

	List<AttendanceStatsRow> selectStatsByDate(@Param("clubId") Long clubId,
											   @Param("startTime") LocalDateTime startTime,
											   @Param("endTime") LocalDateTime endTime);

	void markException(@Param("id") Long id,
					   @Param("reason") String reason);

	void resolveException(@Param("id") Long id,
						  @Param("reason") String reason,
						  @Param("handledBy") Long handledBy,
						  @Param("handledTime") LocalDateTime handledTime);
}

