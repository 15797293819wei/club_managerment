package com.example.club.mapper;

import com.example.club.dto.statistics.ClubActivitySummaryDTO;
import com.example.club.dto.statistics.ClubActivityTrendPointDTO;
import com.example.club.dto.statistics.MemberParticipationDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 统计分析相关数据访问接口
 */
@Mapper
public interface StatisticsMapper {

	List<ClubActivitySummaryDTO> selectClubActivitySummary(@Param("clubId") Long clubId,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime);

	List<ClubActivityTrendPointDTO> selectClubActivityTrend(@Param("clubId") Long clubId,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("granularity") String granularity);

	List<MemberParticipationDTO> selectMemberParticipationRanking(@Param("clubId") Long clubId,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("limit") Integer limit);

	Optional<MemberParticipationDTO> selectMemberParticipationDetail(@Param("clubId") Long clubId,
			@Param("userId") Long userId,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime);
}
