
package com.example.club.service;

import com.example.club.dto.statistics.ClubActivitySummaryDTO;
import com.example.club.dto.statistics.ClubActivityTrendPointDTO;
import com.example.club.dto.statistics.MemberParticipationDTO;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ActivityMapper;
import com.example.club.mapper.ClubApplicationMapper;
import com.example.club.mapper.MemberApplicationMapper;
import com.example.club.mapper.MemberMapper;
import com.example.club.mapper.StatisticsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 统计分析领域服务
 */
@Service
public class StatisticsService {

	private static final int DEFAULT_RANKING_LIMIT = 10;
	private static final int MAX_RANKING_LIMIT = 100;

	private final StatisticsMapper statisticsMapper;
	private final ClubApplicationMapper clubApplicationMapper;
	private final MemberApplicationMapper memberApplicationMapper;
	private final ActivityMapper activityMapper;
	private final MemberMapper memberMapper;

	public StatisticsService(StatisticsMapper statisticsMapper,
							 ClubApplicationMapper clubApplicationMapper,
							 MemberApplicationMapper memberApplicationMapper,
							 ActivityMapper activityMapper,
							 MemberMapper memberMapper) {
		this.statisticsMapper = statisticsMapper;
		this.clubApplicationMapper = clubApplicationMapper;
		this.memberApplicationMapper = memberApplicationMapper;
		this.activityMapper = activityMapper;
		this.memberMapper = memberMapper;
	}

	/**
	 * 获取社团活跃度汇总统计
	 */
	@Transactional(readOnly = true)
	public List<ClubActivitySummaryDTO> getClubActivitySummary(Long clubId, LocalDateTime startTime, LocalDateTime endTime) {
		return statisticsMapper.selectClubActivitySummary(clubId, startTime, endTime);
	}

	/**
	 * 获取社团活跃度趋势数据
	 */
	@Transactional(readOnly = true)
	public List<ClubActivityTrendPointDTO> getClubActivityTrend(Long clubId, LocalDateTime startTime, LocalDateTime endTime, String granularity) {
		String normalized = normalizeGranularity(granularity);
		return statisticsMapper.selectClubActivityTrend(clubId, startTime, endTime, normalized);
	}

	/**
	 * 获取成员参与度排行榜
	 */
	@Transactional(readOnly = true)
	public List<MemberParticipationDTO> getMemberParticipationRanking(Long clubId, LocalDateTime startTime, LocalDateTime endTime, Integer limit) {
		int effectiveLimit = DEFAULT_RANKING_LIMIT;
		if (limit != null && limit > 0) {
			effectiveLimit = Math.min(limit, MAX_RANKING_LIMIT);
		}
		return statisticsMapper.selectMemberParticipationRanking(clubId, startTime, endTime, effectiveLimit);
	}

	/**
	 * 获取指定成员的参与度详情
	 */
	@Transactional(readOnly = true)
	public MemberParticipationDTO getMemberParticipationDetail(Long clubId, Long userId, LocalDateTime startTime, LocalDateTime endTime) {
		return statisticsMapper.selectMemberParticipationDetail(clubId, userId, startTime, endTime)
			.orElseThrow(() -> new BusinessException(40409, "用户不存在或无统计数据"));
	}

	/**
	 * 获取Dashboard统计数据
	 * 
	 * @param isSystemAdmin 是否为系统管理员
	 * @param userId 当前用户ID
	 * @param clubIds 用户管理的社团ID列表（如果是社团管理员）
	 * @param joinedClubIds 用户加入的所有社团ID列表（包括普通成员）
	 * @param filterClubIdForMembers 可选，指定社团ID时仅统计该社团的成员动态（用于成员按社团查看）
	 * @return Dashboard统计数据
	 */
	@Transactional(readOnly = true)
	public DashboardStats getDashboardStats(boolean isSystemAdmin, Long userId, List<Long> clubIds, List<Long> joinedClubIds, Long filterClubIdForMembers) {
		// 今日待办
		long pendingTasks;
		if (isSystemAdmin) {
			// 系统管理员：待审核的社团申请数量
			pendingTasks = clubApplicationMapper.countByFilters(null, 0, null);
		} else {
			// 社团管理员：待审核的入社申请数量（该用户管理的社团）
			if (clubIds == null || clubIds.isEmpty()) {
				pendingTasks = 0;
			} else {
				// 统计所有管理的社团的待审核申请
				pendingTasks = clubIds.stream()
					.mapToLong(clubId -> memberApplicationMapper.countByFilters(clubId, null, 0))
					.sum();
			}
		}
		
		// 活动安排：即将到来的活动数量（未来7天内，状态为待开始或进行中）
		long upcomingActivities;
		if (isSystemAdmin) {
			// 系统管理员：查看所有社团的即将到来的活动
			// 状态 0=待开始, 1=进行中
			long pending = activityMapper.countByFilters(null, null, null, 0);
			long ongoing = activityMapper.countByFilters(null, null, null, 1);
			upcomingActivities = pending + ongoing; // 简化处理，实际应该按时间筛选
		} else {
			// 普通成员或管理员：查看加入的社团的即将到来的活动
			if (joinedClubIds == null || joinedClubIds.isEmpty()) {
				upcomingActivities = 0;
			} else {
				upcomingActivities = joinedClubIds.stream()
					.mapToLong(clubId -> {
						long pending = activityMapper.countByFilters(clubId, null, null, 0);
						long ongoing = activityMapper.countByFilters(clubId, null, null, 1);
						return pending + ongoing;
					})
					.sum();
			}
		}
		
		// 成员动态：最近加入的成员数量（可选按指定社团统计）
		long recentMembers;
		if (filterClubIdForMembers != null) {
			// 成员按所选社团查看该社团成员动态
			recentMembers = Optional.ofNullable(memberMapper.countByFilters(filterClubIdForMembers, null, null, 1)).orElse(0L);
		} else if (isSystemAdmin) {
			// 系统管理员：查看所有社团的最近成员
			recentMembers = memberMapper.countByFilters(null, null, null, 1); // 简化处理
		} else {
			// 社团管理员：查看管理的社团的最近成员；普通成员未选社团时用加入的社团汇总
			if (clubIds != null && !clubIds.isEmpty()) {
				recentMembers = clubIds.stream()
					.mapToLong(cid -> Optional.ofNullable(memberMapper.countByFilters(cid, null, null, 1)).orElse(0L))
					.sum();
			} else if (joinedClubIds != null && !joinedClubIds.isEmpty()) {
				// 成员未传 clubId 时：汇总所有加入社团的成员数
				recentMembers = joinedClubIds.stream()
					.mapToLong(cid -> Optional.ofNullable(memberMapper.countByFilters(cid, null, null, 1)).orElse(0L))
					.sum();
			} else {
				recentMembers = 0;
			}
		}
		
		return new DashboardStats(pendingTasks, upcomingActivities, recentMembers);
	}

	private String normalizeGranularity(String granularity) {
		if (!StringUtils.hasText(granularity)) {
			return "MONTH";
		}
		String upper = granularity.trim().toUpperCase();
		return switch (upper) {
			case "DAY", "WEEK", "MONTH" -> upper;
			default -> "MONTH";
		};
	}

	public record DashboardStats(long pendingTasks, long upcomingActivities, long recentMembers) {
	}
}
