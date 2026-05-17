package com.example.club.service;

import com.example.club.domain.Club;
import com.example.club.domain.Member;
import com.example.club.domain.User;
import com.example.club.dto.PageResult;
import com.example.club.dto.statistics.MemberParticipationDTO;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ClubMapper;
import com.example.club.mapper.MemberMapper;
import com.example.club.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成员领域服务
 * 处理成员的增删改查、角色分配、状态变更等核心业务逻辑。
 */
@Service
public class MemberService {

	private static final int DEFAULT_RECENT_DAYS = 30;
	private static final int MIN_RECENT_DAYS = 7;
	private static final int MAX_RECENT_DAYS = 180;
	private static final int TOP_PARTICIPANT_LIMIT = 5;
	private static final DateTimeFormatter CSV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final MemberMapper memberMapper;
	private final ClubMapper clubMapper;
	private final UserMapper userMapper;
	private final ClubService clubService;
	private final StatisticsService statisticsService;
	private final NotificationService notificationService;

	public MemberService(MemberMapper memberMapper,
						 ClubMapper clubMapper,
						 UserMapper userMapper,
						 ClubService clubService,
						 StatisticsService statisticsService,
						 NotificationService notificationService) {
		this.memberMapper = memberMapper;
		this.clubMapper = clubMapper;
		this.userMapper = userMapper;
		this.clubService = clubService;
		this.statisticsService = statisticsService;
		this.notificationService = notificationService;
	}

	/**
	 * 分页查询成员列表
	 *
	 * @param clubId 社团ID（可选）
	 * @param userId 用户ID（可选）
	 * @param role   角色过滤（可选）
	 * @param status 状态过滤（可选）
	 * @param page   页码
	 * @param size   页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<MemberInfo> page(Long clubId, Long userId, String role, Integer status, int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = memberMapper.countByFilters(clubId, userId, role, status);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Member> members = memberMapper.selectPage(clubId, userId, role, status, offset, size);
		List<MemberInfo> records = buildMemberInfos(members);
		return new PageResult<>(total, records);
	}

	@Transactional(readOnly = true)
	public List<MemberInfo> listAll(Long clubId, String role, Integer status) {
		List<Member> members = memberMapper.selectList(clubId, null, role, status);
		return buildMemberInfos(members);
	}

	/**
	 * 获取成员的详情概览（包含基本信息、用户档案、参与统计）
	 */
	@Transactional(readOnly = true)
	public MemberDetail getMemberDetailOverview(Long memberId) {
		MemberInfo basic = getDetail(memberId);
		var user = userMapper.selectById(basic.userId())
			.orElseThrow(() -> new BusinessException(40401, "用户不存在"));
		List<ClubService.JoinedClubInfo> joinedClubs = clubService.getJoinedClubs(basic.userId());
		MemberParticipationDTO participationDTO = null;
		try {
			participationDTO = statisticsService.getMemberParticipationDetail(basic.clubId(), basic.userId(), null, null);
		} catch (BusinessException ignored) {
			// 成员可能尚未参加任何活动，忽略统计异常
		}
		MemberParticipationSummary participation = new MemberParticipationSummary(
			participationDTO != null && participationDTO.getRegistrationCount() != null ? participationDTO.getRegistrationCount() : 0L,
			participationDTO != null && participationDTO.getSignInCount() != null ? participationDTO.getSignInCount() : 0L,
			participationDTO != null && participationDTO.getParticipationRate() != null ? participationDTO.getParticipationRate() : 0D
		);
		UserProfile profile = new UserProfile(
			user.getId(),
			user.getUsername(),
			user.getRealName(),
			user.getStudentId(),
			user.getEmail(),
			user.getPhone(),
			user.getAvatar(),
			user.getGender()
		);
		return new MemberDetail(basic, profile, joinedClubs, participation);
	}

	/**
	 * 获取社团成员统计信息
	 */
	@Transactional(readOnly = true)
	public ClubMemberStats getClubMemberStats(Long clubId, int recentDays) {
		if (clubId == null) {
			throw new BusinessException(40036, "必须指定社团ID");
		}
		int normalizedDays = Math.max(MIN_RECENT_DAYS, Math.min(MAX_RECENT_DAYS, recentDays <= 0 ? DEFAULT_RECENT_DAYS : recentDays));
		LocalDateTime since = LocalDateTime.now().minusDays(normalizedDays);
		long total = memberMapper.countByFilters(clubId, null, null, 1);
		long inactive = memberMapper.countByFilters(clubId, null, null, 0);
		long newMembers = Optional.ofNullable(memberMapper.countNewMembers(clubId, since)).orElse(0L);
		long activeMembers = Optional.ofNullable(memberMapper.countActiveMembers(clubId, since)).orElse(0L);
		Map<String, Long> roleDistribution = memberMapper.countRoleDistribution(clubId).stream()
			.collect(Collectors.toMap(
				row -> {
					Object role = row.get("role");
					return role != null ? role.toString() : "UNKNOWN";
				},
				row -> ((Number) row.get("cnt")).longValue(),
				(existing, value) -> existing + value,
				LinkedHashMap::new
			));
		List<MemberParticipationDTO> ranking = statisticsService.getMemberParticipationRanking(clubId, since, null, TOP_PARTICIPANT_LIMIT);
		return new ClubMemberStats(total, inactive, newMembers, activeMembers, roleDistribution, ranking);
	}

	/**
	 * 批量调整成员角色
	 */
	@Transactional
	public void batchUpdateRole(Long clubId, List<Long> memberIds, String role) {
		validateClubId(clubId);
		validateMemberSelection(memberIds);
		if (!isValidRole(role)) {
			throw new BusinessException(40037, "角色值非法，可选值：MEMBER, STAFF, VICE_MINISTER, MINISTER");
		}
		List<Member> members = loadAndValidateMembers(clubId, memberIds);
		var club = clubMapper.selectById(clubId).orElse(null);
		if (club != null && club.getPresidentId() != null) {
			boolean containsPresident = members.stream()
				.anyMatch(m -> Objects.equals(club.getPresidentId(), m.getUserId()));
			if (containsPresident) {
				throw new BusinessException(40038, "无法批量调整社长角色");
			}
		}
		if (role.equalsIgnoreCase("PRESIDENT")) {
			throw new BusinessException(40038, "无法批量调整社长角色");
		}
		memberMapper.updateRoleBatch(memberIds, role);
		String clubName = club != null && club.getClubName() != null ? club.getClubName() : "未知社团";
		String roleText = translateRole(role);
		for (Member m : members) {
			notificationService.send(
				m.getUserId(),
				NotificationService.TYPE_ROLE_CHANGED,
				"社团角色变更通知",
				"您在社团【" + clubName + "】的角色已变更为：" + roleText + "。",
				clubId
			);
		}
	}

	/**
	 * 批量移除成员
	 */
	@Transactional
	public void batchRemoveMembers(Long clubId, List<Long> memberIds) {
		validateClubId(clubId);
		validateMemberSelection(memberIds);
		loadAndValidateMembers(clubId, memberIds);
		memberMapper.deleteByIds(memberIds);
		updateClubMemberCount(clubId);
	}

	/**
	 * 导出成员列表为CSV
	 */
	@Transactional(readOnly = true)
	public byte[] exportMembers(Long clubId, String role, Integer status) {
		if (clubId == null) {
			throw new BusinessException(40036, "必须指定社团ID");
		}
		List<Member> members = memberMapper.selectList(clubId, null, role, status);
		if (members == null || members.isEmpty()) {
			return "成员ID,用户名,角色,状态,加入时间\n".getBytes(StandardCharsets.UTF_8);
		}
		List<MemberInfo> infos = buildMemberInfos(members);
		List<Long> userIds = infos.stream().map(MemberInfo::userId).distinct().toList();
		Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
			userMapper.selectByIds(userIds).stream()
				.collect(Collectors.toMap(User::getId, user -> user));
		StringBuilder builder = new StringBuilder();
		builder.append("成员ID,用户名,真实姓名,角色,状态,加入时间\n");
		for (MemberInfo info : infos) {
			User user = userMap.get(info.userId());
			String realName = user != null && user.getRealName() != null ? user.getRealName() : "";
			String statusText = info.status() != null && info.status() == 1 ? "正常" : "已退出";
			String joinTime = info.joinTime() != null ? info.joinTime().format(CSV_TIME_FORMATTER) : "";
			builder.append(info.id()).append(',')
				.append(escapeCsv(info.username())).append(',')
				.append(escapeCsv(realName)).append(',')
				.append(escapeCsv(info.role())).append(',')
				.append(escapeCsv(statusText)).append(',')
				.append(escapeCsv(joinTime))
				.append('\n');
		}
		return builder.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * 查询成员详情
	 *
	 * @param id 成员ID
	 * @return 成员详情
	 */
	@Transactional(readOnly = true)
	public MemberInfo getDetail(Long id) {
		Member member = memberMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40404, "成员不存在"));
		String clubName = clubMapper.selectById(member.getClubId())
			.map(c -> c.getClubName())
			.orElse("未知社团");
		List<Map<String, Object>> userMaps = userMapper.selectUsernamesByIds(List.of(member.getUserId()));
		String username = userMaps.isEmpty() ? "未知用户" :
			userMaps.stream()
				.filter(m -> ((Number) m.get("id")).longValue() == member.getUserId())
				.findFirst()
				.map(m -> (String) m.get("username"))
				.orElse("未知用户");
		
		// 检查是否是社长
		Club club = clubMapper.selectById(member.getClubId()).orElse(null);
		String actualRole = member.getRole();
		if (club != null && Objects.equals(club.getPresidentId(), member.getUserId())) {
			actualRole = "PRESIDENT";
		}
		Member memberWithRole = new Member();
		memberWithRole.setId(member.getId());
		memberWithRole.setClubId(member.getClubId());
		memberWithRole.setUserId(member.getUserId());
		memberWithRole.setRole(actualRole);
		memberWithRole.setJoinTime(member.getJoinTime());
		memberWithRole.setStatus(member.getStatus());
		memberWithRole.setCreatedTime(member.getCreatedTime());
		memberWithRole.setUpdatedTime(member.getUpdatedTime());
		
		return toMemberInfo(memberWithRole, clubName, username);
	}

	/**
	 * 添加成员（通常由入社申请审核通过后调用）
	 *
	 * @param command 添加命令
	 * @return 新成员ID
	 */
	@Transactional
	public Long add(MemberCommand command) {
		// 校验社团是否存在
		clubMapper.selectById(command.clubId())
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));

		// 检查是否已经是成员
		Optional<Member> existing = memberMapper.selectByClubIdAndUserId(command.clubId(), command.userId());
		if (existing.isPresent()) {
			Member member = existing.get();
			if (member.getStatus() == 1) {
				throw new BusinessException(40027, "该用户已是该社团成员");
			}
			// 如果之前退出过，恢复成员身份
			member.setStatus(1);
			member.setRole(command.role() != null ? command.role() : "MEMBER");
			member.setJoinTime(LocalDateTime.now());
			memberMapper.updateById(member);
			return member.getId();
		}

		// 创建新成员
		Member member = new Member();
		member.setClubId(command.clubId());
		member.setUserId(command.userId());
		member.setRole(command.role() != null ? command.role() : "MEMBER");
		member.setJoinTime(LocalDateTime.now());
		member.setStatus(1); // 正常状态
		memberMapper.insert(member);

		// 更新社团成员数量
		updateClubMemberCount(command.clubId());

		return member.getId();
	}

	/**
	 * 更新成员角色
	 *
	 * @param id   成员ID
	 * @param role 新角色
	 */
	@Transactional
	public void updateRole(Long id, String role) {
		Member member = memberMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40404, "成员不存在"));
		if (role == null || role.isEmpty()) {
			throw new BusinessException(40028, "角色不能为空");
		}
		// 验证角色值
		if (!isValidRole(role)) {
			throw new BusinessException(40029, "角色值非法，可选值：MEMBER, STAFF, VICE_MINISTER, MINISTER, PRESIDENT");
		}
		memberMapper.updateRole(id, role);

		// 角色变更通知
		Long userId = member.getUserId();
		Club club = clubMapper.selectById(member.getClubId()).orElse(null);
		String clubName = club != null ? club.getClubName() : "未知社团";
		String title = "社团角色变更通知";
		String content = "您在社团【" + clubName + "】的角色已变更为：" + translateRole(role) + "。";
		notificationService.send(
			userId,
			NotificationService.TYPE_ROLE_CHANGED,
			title,
			content,
			member.getClubId()
		);
	}

	/**
	 * 修改成员状态
	 *
	 * @param id     成员ID
	 * @param status 状态值（0-已退出，1-正常）
	 */
	@Transactional
	public void changeStatus(Long id, Integer status) {
		Member member = memberMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40404, "成员不存在"));
		if (status == null || (status != 0 && status != 1)) {
			throw new BusinessException(40030, "状态值非法，只能为0（已退出）或1（正常）");
		}
		Long clubId = member.getClubId();
		memberMapper.updateStatus(id, status);

		// 如果成员退出，更新社团成员数量
		if (status == 0) {
			updateClubMemberCount(clubId);
		}
	}

	/**
	 * 成员自助退出社团
	 * <p>仅用于“自己退出社团”的场景：标记为已退出，并向社团管理员发送通知。</p>
	 *
	 * @param id 成员ID
	 */
	@Transactional
	public void quit(Long id) {
		Member member = memberMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40404, "成员不存在"));
		if (member.getStatus() != null && member.getStatus() == 0) {
			return;
		}
		Long clubId = member.getClubId();
		memberMapper.updateStatus(id, 0);
		updateClubMemberCount(clubId);

		// 发送“成员已退出社团”通知给社团管理员
		Club club = clubMapper.selectById(clubId)
			.orElse(null);
		if (club == null) {
			return;
		}
		User user = userMapper.selectById(member.getUserId())
			.orElse(null);

		String memberName = user != null && user.getRealName() != null && !user.getRealName().isEmpty()
			? user.getRealName()
			: (user != null && user.getUsername() != null ? user.getUsername() : "未知成员");
		String clubName = club.getClubName() != null ? club.getClubName() : "未知社团";

		String title = "成员退出社团通知";
		String content = memberName + " 成员已退出本社团【" + clubName + "】。";

		// 1. 通知创始人和社长
		java.util.Set<Long> adminUserIds = new java.util.HashSet<>();
		if (club.getFounderId() != null) {
			adminUserIds.add(club.getFounderId());
		}
		if (club.getPresidentId() != null) {
			adminUserIds.add(club.getPresidentId());
		}

		// 2. 通知该社团当前的管理成员（部长 / 副部长 / 干事）
		List<Member> activeMembers = memberMapper.selectList(clubId, null, null, 1);
		for (Member m : activeMembers) {
			String role = m.getRole();
			if ("MINISTER".equals(role) || "VICE_MINISTER".equals(role) || "STAFF".equals(role)) {
				adminUserIds.add(m.getUserId());
			}
		}

		// 不需要通知已退出的本人
		adminUserIds.remove(member.getUserId());

		for (Long adminUserId : adminUserIds) {
			notificationService.send(
				adminUserId,
				NotificationService.TYPE_MEMBER_QUIT,
				title,
				content,
				clubId
			);
		}
	}

	/**
	 * 删除成员
	 *
	 * @param id 成员ID
	 */
	@Transactional
	public void delete(Long id) {
		Member member = memberMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40404, "成员不存在"));
		Long clubId = member.getClubId();
		memberMapper.deleteById(id);
		// 更新社团成员数量
		updateClubMemberCount(clubId);
	}

	// 检查当前用户是否具备指定社团的管理权限
	@Transactional(readOnly = true)
	public void checkManagePermission(Long clubId, Long userId, boolean isSystemAdmin) {
		if (isSystemAdmin) {
			return; // 系统管理员有所有权限
		}
		// 检查是否为社团创始人、社长，或该社团的部长、副部长、干事（干事有踢出成员等基础管理权）
		var club = clubMapper.selectById(clubId)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		if (!Objects.equals(club.getFounderId(), userId) && !Objects.equals(club.getPresidentId(), userId)) {
			Optional<Member> member = memberMapper.selectByClubIdAndUserId(clubId, userId);
			if (member.isEmpty() || member.get().getStatus() != 1) {
				throw new BusinessException(40302, "无权管理该社团的成员");
			}
			String role = member.get().getRole();
			if (!"MINISTER".equals(role) && !"VICE_MINISTER".equals(role) && !"STAFF".equals(role)) {
				throw new BusinessException(40302, "无权管理该社团的成员");
			}
		}
	}

	/**
	 * 校验用户是否有权限分配成员身份
	 * 仅允许系统管理员或当前社团社长执行
	 *
	 * @param clubId 社团ID
	 * @param userId 用户ID
	 * @param isSystemAdmin 是否系统管理员
	 */
	@Transactional(readOnly = true)
	public void checkRoleAssignmentPermission(Long clubId, Long userId, boolean isSystemAdmin) {
		if (isSystemAdmin) {
			return;
		}
		var club = clubMapper.selectById(clubId)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		if (!Objects.equals(club.getPresidentId(), userId)) {
			throw new BusinessException(40302, "只有社长可以分配成员身份");
		}
	}

	@Transactional(readOnly = true)
	public void checkStatisticsPermission(Long clubId, Long userId, boolean isSystemAdmin) {
		if (isSystemAdmin) {
			return;
		}
		var club = clubMapper.selectById(clubId)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		if (Objects.equals(club.getFounderId(), userId) || Objects.equals(club.getPresidentId(), userId)) {
			return;
		}
		Optional<Member> member = memberMapper.selectByClubIdAndUserId(clubId, userId);
		if (member.isPresent() && member.get().getStatus() == 1) {
			String role = member.get().getRole();
			if ("MINISTER".equals(role) || "VICE_MINISTER".equals(role) || "STAFF".equals(role)) {
				return;
			}
		}
		throw new BusinessException(40302, "无权查看该社团的成员统计");
	}

	/**
	 * 获取角色优先级（用于排序）
	 * 社长=1, 部长=2, 副部长=3, 干事=4, 社员=5
	 */
	private int getRolePriority(String role) {
		if (role == null) return 5;
		return switch (role) {
			case "PRESIDENT" -> 1;
			case "MINISTER" -> 2;
			case "VICE_MINISTER" -> 3;
			case "STAFF" -> 4;
			case "MEMBER" -> 5;
			default -> 5;
		};
	}

	/**
	 * 更新社团成员数量
	 */
	private void updateClubMemberCount(Long clubId) {
		Long count = memberMapper.countByFilters(clubId, null, null, 1); // 只统计正常状态的成员
		clubMapper.updateMemberCount(clubId, count.intValue());
	}

	private void validateClubId(Long clubId) {
		if (clubId == null) {
			throw new BusinessException(40036, "必须指定社团ID");
		}
	}

	private void validateMemberSelection(List<Long> memberIds) {
		if (memberIds == null || memberIds.isEmpty()) {
			throw new BusinessException(40039, "请选择至少一名成员");
		}
	}

	private String translateRole(String role) {
		if (role == null) {
			return "成员";
		}
		return switch (role.toUpperCase()) {
			case "PRESIDENT" -> "社长";
			case "MINISTER" -> "部长";
			case "VICE_MINISTER" -> "副部长";
			case "STAFF" -> "干事";
			case "CLUB_ADMIN" -> "社团管理员";
			default -> "成员";
		};
	}

	private List<Member> loadAndValidateMembers(Long clubId, List<Long> memberIds) {
		List<Member> members = memberMapper.selectByIds(memberIds);
		if (members == null || members.size() != memberIds.size()) {
			throw new BusinessException(40404, "存在不存在的成员记录");
		}
		boolean mismatch = members.stream().anyMatch(m -> !Objects.equals(m.getClubId(), clubId));
		if (mismatch) {
			throw new BusinessException(40040, "检测到不属于该社团的成员");
		}
		return members;
	}

	private List<MemberInfo> buildMemberInfos(List<Member> members) {
		if (members == null || members.isEmpty()) {
			return List.of();
		}
		Set<Long> clubIds = members.stream()
			.map(Member::getClubId)
			.collect(Collectors.toSet());
		Map<Long, Club> clubMap = new HashMap<>();
		for (Long cid : clubIds) {
			clubMapper.selectById(cid).ifPresent(club -> clubMap.put(cid, club));
		}
		Map<Long, String> clubNames = clubIds.stream()
			.collect(Collectors.toMap(
				id -> id,
				id -> Optional.ofNullable(clubMap.get(id))
					.map(Club::getClubName)
					.orElse("未知社团")
			));
		List<Long> userIds = members.stream().map(Member::getUserId).distinct().toList();
		Map<Long, String> usernames = userIds.isEmpty() ? Map.of() :
			userMapper.selectUsernamesByIds(userIds).stream()
				.collect(Collectors.toMap(
					m -> ((Number) m.get("id")).longValue(),
					m -> (String) m.get("username"),
					(v1, v2) -> v1
				));
		return members.stream()
			.map(m -> {
				Club club = clubMap.get(m.getClubId());
				String actualRole = m.getRole();
				if (club != null && Objects.equals(club.getPresidentId(), m.getUserId())) {
					actualRole = "PRESIDENT";
				}
				Member memberWithRole = new Member();
				memberWithRole.setId(m.getId());
				memberWithRole.setClubId(m.getClubId());
				memberWithRole.setUserId(m.getUserId());
				memberWithRole.setRole(actualRole);
				memberWithRole.setJoinTime(m.getJoinTime());
				memberWithRole.setStatus(m.getStatus());
				memberWithRole.setCreatedTime(m.getCreatedTime());
				memberWithRole.setUpdatedTime(m.getUpdatedTime());
				return toMemberInfo(
					memberWithRole,
					clubNames.getOrDefault(m.getClubId(), "未知社团"),
					usernames.get(m.getUserId())
				);
			})
			.sorted((a, b) -> {
				int priorityA = getRolePriority(a.role());
				int priorityB = getRolePriority(b.role());
				if (priorityA != priorityB) {
					return Integer.compare(priorityA, priorityB);
				}
				if (a.joinTime() != null && b.joinTime() != null) {
					return a.joinTime().compareTo(b.joinTime());
				}
				return 0;
			})
			.toList();
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private String escapeCsv(String value) {
		String safe = nullToEmpty(value);
		if (safe.contains("\"")) {
			safe = safe.replace("\"", "\"\"");
		}
		if (safe.contains(",") || safe.contains("\n")) {
			return "\"" + safe + "\"";
		}
		return safe;
	}

	/**
	 * 验证角色值是否合法
	 */
	private boolean isValidRole(String role) {
		return "MEMBER".equals(role) || "STAFF".equals(role) ||
			"VICE_MINISTER".equals(role) || "MINISTER".equals(role) ||
			"PRESIDENT".equals(role);
	}

	private MemberInfo toMemberInfo(Member member, String clubName, String username) {
		return new MemberInfo(
			member.getId(),
			member.getClubId(),
			clubName,
			member.getUserId(),
			username,
			member.getRole(),
			member.getJoinTime(),
			member.getStatus(),
			member.getCreatedTime(),
			member.getUpdatedTime()
		);
	}

	public record MemberCommand(Long clubId, Long userId, String role) {
	}

	public record MemberInfo(Long id, Long clubId, String clubName, Long userId, String username, String role, LocalDateTime joinTime,
							 Integer status, LocalDateTime createdTime, LocalDateTime updatedTime) {
	}

	public record MemberDetail(MemberInfo basic, UserProfile profile, List<ClubService.JoinedClubInfo> joinedClubs,
							   MemberParticipationSummary participation) {
	}

	public record UserProfile(Long userId, String username, String realName, String studentId,
							  String email, String phone, String avatar, Integer gender) {
	}

	public record MemberParticipationSummary(Long registrationCount, Long signInCount, double participationRate) {
	}

	public record ClubMemberStats(long totalMembers, long inactiveMembers, long newMembers, long activeMembers,
								  Map<String, Long> roleDistribution, List<MemberParticipationDTO> topParticipants) {
	}
}

