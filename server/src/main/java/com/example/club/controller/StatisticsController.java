package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.dto.statistics.ClubActivitySummaryDTO;
import com.example.club.dto.statistics.ClubActivityTrendPointDTO;
import com.example.club.dto.statistics.MemberParticipationDTO;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.MemberMapper;
import com.example.club.mapper.UserMapper;
import com.example.club.service.ActivityService;
import com.example.club.service.MemberService;
import com.example.club.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据统计接口
 */
@Tag(name = "数据统计", description = "提供社团活跃度、成员参与度等统计数据接口")
@RestController
@RequestMapping("/api/stats")
@Validated
public class StatisticsController {

	private final StatisticsService statisticsService;
	private final ActivityService activityService;
	private final UserMapper userMapper;
	private final com.example.club.mapper.ClubMapper clubMapper;
	private final MemberMapper memberMapper;
	private final MemberService memberService;

	public StatisticsController(StatisticsService statisticsService,
								ActivityService activityService,
								UserMapper userMapper,
								com.example.club.mapper.ClubMapper clubMapper,
								MemberMapper memberMapper,
								MemberService memberService) {
		this.statisticsService = statisticsService;
		this.activityService = activityService;
		this.userMapper = userMapper;
		this.clubMapper = clubMapper;
		this.memberMapper = memberMapper;
		this.memberService = memberService;
	}

	// ==================== 社团活跃度统计 ====================

	@Operation(summary = "社团活跃度汇总", description = "按条件汇总社团活动次数、报名数、签到数等数据")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权查看该社团数据")
	})
	@GetMapping("/club-activity/summary")
	public ApiResponse<List<ClubActivitySummaryDTO>> clubActivitySummary(@Valid ClubActivitySummaryRequest request) {
		validateClubAccess(request.getClubId());
		List<ClubActivitySummaryDTO> data = statisticsService.getClubActivitySummary(
			request.getClubId(),
			request.getStartTime(),
			request.getEndTime()
		);
		return ApiResponse.success(data);
	}

	@Operation(summary = "社团活跃度趋势", description = "按时间粒度统计活动趋势，用于绘制趋势图")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权查看该社团数据")
	})
	@GetMapping("/club-activity/trend")
	public ApiResponse<List<ClubActivityTrendPointDTO>> clubActivityTrend(@Valid ClubActivityTrendRequest request) {
		validateClubAccess(request.getClubId());
		List<ClubActivityTrendPointDTO> data = statisticsService.getClubActivityTrend(
				request.getClubId(),
				request.getStartTime(),
				request.getEndTime(),
				request.getGranularity()
		);
		return ApiResponse.success(data);
	}

	// ==================== 成员参与度统计 ====================

	@Operation(summary = "成员参与度排行榜", description = "统计成员报名次数、签到次数，生成排行榜")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权查看该社团数据")
	})
	@GetMapping("/member-participation/ranking")
	public ApiResponse<List<MemberParticipationDTO>> memberParticipationRanking(@Valid MemberParticipationRankingRequest request) {
		validateClubAccess(request.getClubId());
		List<MemberParticipationDTO> data = statisticsService.getMemberParticipationRanking(
			request.getClubId(),
			request.getStartTime(),
			request.getEndTime(),
			request.getLimit()
		);
		return ApiResponse.success(data);
	}

	@Operation(summary = "成员参与度详情", description = "查看指定成员在统计期内的报名次数、签到次数及参与率")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权查看该成员数据"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在或无统计数据")
	})
	@GetMapping("/member-participation/{userId}")
	public ApiResponse<MemberParticipationDTO> memberParticipationDetail(
		@Parameter(description = "用户ID", required = true) @PathVariable Long userId,
		@Valid MemberParticipationDetailRequest request) {
		validateMemberAccess(userId, request.getClubId());
		MemberParticipationDTO data = statisticsService.getMemberParticipationDetail(
			request.getClubId(),
			userId,
			request.getStartTime(),
			request.getEndTime()
		);
		return ApiResponse.success(data);
	}

	// ==================== Dashboard 统计 ====================

	@Operation(summary = "获取Dashboard统计数据", description = "获取首页统计数据，包括今日待办、活动安排、成员动态；可选 clubId 用于成员按所选社团查看该社团成员动态")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权查看该社团数据")
	})
	@GetMapping("/dashboard")
	public ApiResponse<StatisticsService.DashboardStats> getDashboardStats(
		@Parameter(description = "可选，用于成员按所选社团查看该社团的成员动态") @RequestParam(required = false) Long clubId) {
		boolean isSystemAdmin = isSystemAdmin();
		Long userId = getCurrentUserId();
		List<Long> clubIds = null;
		List<Long> joinedClubIds = null; // 用户加入的所有社团（包括普通成员）
		Long filterClubIdForMembers = null; // 成员按社团筛选成员动态时传入

		if (!isSystemAdmin) {
			// 查询用户作为创始人的社团
			List<com.example.club.domain.Club> founderClubs = clubMapper.selectByFounderId(userId);
			// 查询用户作为社长的社团
			List<com.example.club.domain.Club> presidentClubs = clubMapper.selectByPresidentId(userId);
			// 合并并去重
			Set<Long> clubIdSet = new HashSet<>();
			if (founderClubs != null) {
				founderClubs.forEach(c -> clubIdSet.add(c.getId()));
			}
			if (presidentClubs != null) {
				presidentClubs.forEach(c -> clubIdSet.add(c.getId()));
			}
			clubIds = new ArrayList<>(clubIdSet);

			// 查询用户加入的所有社团（包括普通成员）
			List<com.example.club.domain.Member> members = memberMapper.selectPage(null, userId, null, 1, 0L, 1000);
			Set<Long> joinedSet = new HashSet<>();
			if (members != null) {
				members.forEach(m -> joinedSet.add(m.getClubId()));
			}
			joinedSet.addAll(clubIdSet);
			joinedClubIds = new ArrayList<>(joinedSet);

			// 成员按所选社团查看成员动态：校验 clubId 属于已加入社团
			if (clubId != null) {
				if (!joinedSet.contains(clubId)) {
					throw new BusinessException(40304, "无权查看该社团数据");
				}
				filterClubIdForMembers = clubId;
			}
		}

		StatisticsService.DashboardStats stats = statisticsService.getDashboardStats(
			isSystemAdmin, userId, clubIds, joinedClubIds, filterClubIdForMembers);
		return ApiResponse.success(stats);
	}

	// ==================== 权限校验与工具方法 ====================

	private void validateClubAccess(Long clubId) {
		boolean systemAdmin = isSystemAdmin();
		if (clubId == null) {
			if (!systemAdmin) {
				throw new BusinessException(40304, "仅系统管理员可查询全部社团数据");
			}
			return;
		}
		memberService.checkStatisticsPermission(clubId, getCurrentUserId(), systemAdmin);
	}

	private void validateMemberAccess(Long targetUserId, Long clubId) {
		Long currentUserId = getCurrentUserId();
		boolean systemAdmin = isSystemAdmin();
		if (targetUserId.equals(currentUserId)) {
			// 可查看个人数据
			if (clubId != null) {
				activityService.checkManagePermission(clubId, currentUserId, systemAdmin);
			}
			return;
		}
		if (systemAdmin) {
			return;
		}
		if (clubId != null) {
			activityService.checkManagePermission(clubId, currentUserId, false);
		} else {
			throw new BusinessException(40305, "无权查看其他成员的总体参与度数据");
		}
	}

	private boolean isSystemAdmin() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return false;
		}
		return auth.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(authority -> authority.equals("ROLE_SYSTEM_ADMIN"));
	}

	private Long getCurrentUserId() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userMapper.selectByUsername(username)
			.map(user -> user.getId())
			.orElseThrow(() -> new RuntimeException("用户不存在"));
	}

	// ==================== 请求对象定义 ====================

	@Schema(description = "社团活跃度汇总查询参数")
	public static class ClubActivitySummaryRequest {
		@Schema(description = "社团ID，不填表示全量数据（仅系统管理员可用）")
		private Long clubId;

		@Schema(description = "统计起始时间，包含边界")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime startTime;

		@Schema(description = "统计结束时间，包含边界")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime endTime;

		public Long getClubId() { return clubId; }
		public void setClubId(Long clubId) { this.clubId = clubId; }
		public LocalDateTime getStartTime() { return startTime; }
		public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
		public LocalDateTime getEndTime() { return endTime; }
		public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
	}

	@Schema(description = "社团活跃度趋势查询参数")
	public static class ClubActivityTrendRequest extends ClubActivitySummaryRequest {
		@Schema(description = "时间粒度，支持DAY/WEEK/MONTH，默认MONTH", example = "MONTH")
		private String granularity;

		public String getGranularity() { return granularity; }
		public void setGranularity(String granularity) { this.granularity = granularity; }
	}

	@Schema(description = "成员参与度排行榜查询参数")
	public static class MemberParticipationRankingRequest {
		@Schema(description = "社团ID，不填表示全量数据（仅系统管理员可用）")
		private Long clubId;

		@Schema(description = "统计起始时间，包含边界")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime startTime;

		@Schema(description = "统计结束时间，包含边界")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime endTime;

		@Schema(description = "返回排行榜的最大条目数量，默认10，最大100", example = "10")
		@Min(value = 1, message = "limit最小为1")
		@Max(value = 100, message = "limit最大为100")
		private Integer limit;

		public Long getClubId() { return clubId; }
		public void setClubId(Long clubId) { this.clubId = clubId; }
		public LocalDateTime getStartTime() { return startTime; }
		public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
		public LocalDateTime getEndTime() { return endTime; }
		public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
		public Integer getLimit() { return limit; }
		public void setLimit(Integer limit) { this.limit = limit; }
	}

	@Schema(description = "成员参与度详情查询参数")
	public static class MemberParticipationDetailRequest {
		@Schema(description = "社团ID，可选。若指定，则进行权限校验")
		private Long clubId;

		@Schema(description = "统计起始时间，包含边界")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime startTime;

		@Schema(description = "统计结束时间，包含边界")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime endTime;

		public Long getClubId() { return clubId; }
		public void setClubId(Long clubId) { this.clubId = clubId; }
		public LocalDateTime getStartTime() { return startTime; }
		public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
		public LocalDateTime getEndTime() { return endTime; }
		public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
	}
}
