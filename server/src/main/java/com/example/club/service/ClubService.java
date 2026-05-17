package com.example.club.service;

import com.example.club.domain.Club;
import com.example.club.domain.Member;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ClubMapper;
import com.example.club.mapper.MemberMapper;
import com.example.club.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * 社团领域服务
 * <p>处理社团的增删改查、状态变更等核心业务逻辑。</p>
 */
@Service
public class ClubService {

	private final ClubMapper clubMapper;
	private final UserMapper userMapper;
	private final MemberMapper memberMapper;

	public ClubService(ClubMapper clubMapper, UserMapper userMapper, MemberMapper memberMapper) {
		this.clubMapper = clubMapper;
		this.userMapper = userMapper;
		this.memberMapper = memberMapper;
	}

	/**
	 * 分页查询社团列表
	 *
	 * @param keyword  关键词（社团名称、代码）
	 * @param status   状态过滤
	 * @param founderId 创始人ID过滤（可选，用于限制查询范围）
	 * @param page     页码
	 * @param size     页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<ClubInfo> page(String keyword, Integer status, Long founderId, int page, int size) {
		return page(keyword, status, founderId, null, null, null, null, page, size);
	}

	public PageResult<ClubInfo> page(String keyword, Integer status, Long founderId,
									 Integer minMembers, Integer maxMembers,
									 String sortBy, String sortOrder,
									 int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = clubMapper.countByFilters(keyword, status, founderId, minMembers, maxMembers);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Club> clubs = clubMapper.selectPage(
			keyword,
			status,
			founderId,
			minMembers,
			maxMembers,
			resolveSortField(sortBy),
			resolveSortOrder(sortOrder),
			offset,
			size
		);
		Map<Long, String> founderNameMap = loadFounderUsernames(clubs);
		List<ClubInfo> records = clubs.stream()
			.map(club -> toClubInfo(club, founderNameMap))
			.toList();
		return new PageResult<>(total, records);
	}

	/**
	 * 面向普通用户的社团分页查询（仅展示已通过的社团）
	 *
	 * @param keyword 关键词
	 * @param page    页码
	 * @param size    页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<ClubInfo> pageForPublic(String keyword,
											 Integer minMembers,
											 Integer maxMembers,
											 String sortBy,
											 String sortOrder,
											 int page,
											 int size) {
		return page(keyword, 1, null, minMembers, maxMembers, sortBy, sortOrder, page, size);
	}

	/**
	 * 获取用户管理的社团列表
	 * 包括：作为创始人、社长、部长、副部长的社团
	 *
	 * @param userId 用户ID
	 * @return 社团列表
	 */
	@Transactional(readOnly = true)
	public List<ClubInfo> getManagedClubs(Long userId) {
		Set<Long> clubIdSet = new HashSet<>();
		
		// 1. 查询作为创始人的社团
		List<Club> founderClubs = clubMapper.selectByFounderId(userId);
		if (founderClubs != null) {
			founderClubs.forEach(c -> clubIdSet.add(c.getId()));
		}
		
		// 2. 查询作为社长的社团
		List<Club> presidentClubs = clubMapper.selectByPresidentId(userId);
		if (presidentClubs != null) {
			presidentClubs.forEach(c -> clubIdSet.add(c.getId()));
		}
		
		// 3. 查询作为部长或副部长的社团
		List<Member> ministerMembers = memberMapper.selectPage(null, userId, "MINISTER", 1, 0L, 1000);
		if (ministerMembers != null) {
			ministerMembers.forEach(m -> clubIdSet.add(m.getClubId()));
		}
		
		List<Member> viceMinisterMembers = memberMapper.selectPage(null, userId, "VICE_MINISTER", 1, 0L, 1000);
		if (viceMinisterMembers != null) {
			viceMinisterMembers.forEach(m -> clubIdSet.add(m.getClubId()));
		}
		
		if (clubIdSet.isEmpty()) {
			return List.of();
		}
		
		// 查询社团详情
		List<Club> clubs = new ArrayList<>();
		for (Long clubId : clubIdSet) {
			Optional<Club> club = clubMapper.selectById(clubId);
			if (club.isPresent()) {
				clubs.add(club.get());
			}
		}
		
		Map<Long, String> founderNameMap = loadFounderUsernames(clubs);
		return clubs.stream()
			.map(club -> toClubInfo(club, founderNameMap))
			.toList();
	}

	/**
	 * 获取用户加入的社团列表（包括角色信息）
	 * 包括：作为成员、干事、副部长、部长的所有社团
	 *
	 * @param userId 用户ID
	 * @return 加入的社团列表（包含角色信息）
	 */
	@Transactional(readOnly = true)
	public List<JoinedClubInfo> getJoinedClubs(Long userId) {
		// 查询用户加入的所有社团（状态为正常）
		List<Member> members = memberMapper.selectPage(null, userId, null, 1, 0L, 1000);
		if (members == null || members.isEmpty()) {
			return List.of();
		}
		
		// 获取所有社团ID
		Set<Long> clubIds = members.stream()
			.map(Member::getClubId)
			.collect(Collectors.toSet());
		
		// 查询社团详情
		List<Club> clubs = new ArrayList<>();
		for (Long clubId : clubIds) {
			Optional<Club> club = clubMapper.selectById(clubId);
			if (club.isPresent()) {
				clubs.add(club.get());
			}
		}
		
		Map<Long, String> founderNameMap = loadFounderUsernames(clubs);
		
		// 构建成员角色映射
		Map<Long, String> memberRoleMap = members.stream()
			.collect(Collectors.toMap(
				Member::getClubId,
				Member::getRole,
				(v1, v2) -> v1 // 如果有重复，保留第一个
			));

		// 转换为JoinedClubInfo
		return clubs.stream()
			.map(club -> {
				String founderUsername = Optional.ofNullable(club.getFounderId())
					.map(founderNameMap::get)
					.orElse(null);
				String role = memberRoleMap.get(club.getId());
				// 如果该用户是社长（presidentId），则角色为PRESIDENT
				Member member = members.stream()
					.filter(m -> m.getClubId().equals(club.getId()))
					.findFirst()
					.orElse(null);
				if (member != null && Objects.equals(club.getPresidentId(), member.getUserId())) {
					role = "PRESIDENT";
				}
				return new JoinedClubInfo(
					club.getId(),
					club.getClubName(),
					club.getClubCode(),
					club.getDescription(),
					club.getLogo(),
					club.getFounderId(),
					founderUsername,
					club.getMemberCount(),
					club.getStatus(),
					role,
					club.getCreatedTime(),
					club.getUpdatedTime()
				);
			})
			.toList();
	}

	/**
	 * 查询社团详情
	 *
	 * @param id 社团ID
	 * @return 社团详情
	 */
	@Transactional(readOnly = true)
	public ClubInfo getDetail(Long id) {
		Club club = clubMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		String founderUsername = Optional.ofNullable(club.getFounderId())
			.flatMap(userMapper::selectById)
			.map(user -> user.getUsername())
			.orElse(null);
		return toClubInfo(club, founderUsername);
	}

	/**
	 * 创建新社团
	 *
	 * @param command 创建命令
	 * @return 新社团ID
	 */
	@Transactional
	public Long create(ClubCommand command) {
		validateClubNameUnique(command.clubName(), null);
		if (StringUtils.hasText(command.clubCode())) {
			validateClubCodeUnique(command.clubCode(), null);
		}
		ensureUserCanCreateClub(command.founderId());

		Club club = new Club();
		club.setClubName(command.clubName());
		club.setClubCode(command.clubCode());
		club.setDescription(command.description());
		club.setPurpose(command.purpose());
		club.setConstitution(command.constitution());
		club.setLogo(command.logo());
		club.setContactPerson(command.contactPerson());
		club.setContactPhone(command.contactPhone());
		club.setContactEmail(command.contactEmail());
		club.setFounderId(command.founderId());
		club.setPresidentId(command.presidentId());
		club.setMemberCount(0);
		club.setStatus(Optional.ofNullable(command.status()).orElse(0)); // 默认待审核
		club.setEstablishedTime(command.establishedTime());
		clubMapper.insert(club);
		return club.getId();
	}

	/**
	 * 更新社团信息
	 *
	 * @param id      社团ID
	 * @param command 更新命令
	 */
	@Transactional
	public void update(Long id, ClubCommand command) {
		Club existing = clubMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));

		if (StringUtils.hasText(command.clubName()) && !Objects.equals(command.clubName(), existing.getClubName())) {
			validateClubNameUnique(command.clubName(), id);
		}
		if (StringUtils.hasText(command.clubCode()) && !Objects.equals(command.clubCode(), existing.getClubCode())) {
			validateClubCodeUnique(command.clubCode(), id);
		}

		Club club = new Club();
		club.setId(id);
		if (StringUtils.hasText(command.clubName())) {
			club.setClubName(command.clubName());
		}
		if (StringUtils.hasText(command.clubCode())) {
			club.setClubCode(command.clubCode());
		}
		club.setDescription(command.description());
		club.setPurpose(command.purpose());
		club.setConstitution(command.constitution());
		club.setLogo(command.logo());
		club.setContactPerson(command.contactPerson());
		club.setContactPhone(command.contactPhone());
		club.setContactEmail(command.contactEmail());
		if (command.presidentId() != null) {
			club.setPresidentId(command.presidentId());
		}
		if (command.memberCount() != null) {
			club.setMemberCount(command.memberCount());
		}
		if (command.status() != null) {
			club.setStatus(command.status());
		}
		if (command.establishedTime() != null) {
			club.setEstablishedTime(command.establishedTime());
		}
		clubMapper.updateById(club);
	}

	/**
	 * 修改社团状态
	 *
	 * @param id     社团ID
	 * @param status 状态值（0-待审核，1-已通过，2-已驳回，3-已解散）
	 */
	@Transactional
	public void changeStatus(Long id, Integer status) {
		clubMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		if (status == null || status < 0 || status > 3) {
			throw new BusinessException(40020, "状态值非法");
		}
		clubMapper.updateStatus(id, status);
	}

	/**
	 * 解散社团
	 *
	 * @param clubId 社团ID
	 * @param operatorId 操作人
	 * @param isSystemAdmin 是否系统管理员
	 */
	@Transactional
	public void dissolveClub(Long clubId, Long operatorId, boolean isSystemAdmin) {
		Club club = clubMapper.selectById(clubId)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		if (!isSystemAdmin && !Objects.equals(club.getFounderId(), operatorId)
			&& !Objects.equals(club.getPresidentId(), operatorId)) {
			throw new BusinessException(40301, "无权解散该社团");
		}
		if (club.getStatus() != null && club.getStatus() == 3) {
			throw new BusinessException(40030, "社团已处于解散状态");
		}
		clubMapper.updateStatus(clubId, 3);
		memberMapper.updateStatusByClubId(clubId, 0);
		clubMapper.updateMemberCount(clubId, 0);
	}

	@Transactional(readOnly = true)
	public List<ClubLeaderboardItem> getLeaderboard(String type, int limit) {
		int normalizedLimit = Math.max(1, Math.min(limit, 20));
		List<ClubMapper.ClubLeaderboardRow> rows;
		String metricLabel;
		if ("activity_count".equalsIgnoreCase(type)) {
			rows = clubMapper.selectLeaderboardByActivityCount(normalizedLimit);
			metricLabel = "活动数量";
		} else {
			rows = clubMapper.selectLeaderboardByMemberCount(normalizedLimit);
			metricLabel = "成员数量";
		}
		return rows.stream()
			.map(row -> new ClubLeaderboardItem(
				row.getClubId(),
				row.getClubName(),
				row.getMetricValue() != null ? row.getMetricValue() : 0L,
				metricLabel
			))
			.toList();
	}

	/**
	 * 删除社团
	 *
	 * @param id 社团ID
	 */
	@Transactional
	public void delete(Long id) {
		clubMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		clubMapper.deleteById(id);
	}

	/**
	 * 校验用户是否有权限管理该社团
	 * 系统管理员可以管理所有社团，社团管理员只能管理自己创建的社团
	 *
	 * @param clubId   社团ID
	 * @param userId   用户ID
	 * @param isSystemAdmin 是否为系统管理员
	 */
	@Transactional(readOnly = true)
	public void checkManagePermission(Long clubId, Long userId, boolean isSystemAdmin) {
		if (isSystemAdmin) {
			return; // 系统管理员有所有权限
		}
		Club club = clubMapper.selectById(clubId)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		if (!Objects.equals(club.getFounderId(), userId) && !Objects.equals(club.getPresidentId(), userId)) {
			throw new BusinessException(40301, "无权管理该社团");
		}
	}

	private String resolveSortField(String sortBy) {
		if (sortBy == null) {
			return null;
		}
		return switch (sortBy) {
			case "createdTime" -> "created_time";
			case "memberCount" -> "member_count";
			case "establishedTime" -> "established_time";
			default -> null;
		};
	}

	private String resolveSortOrder(String sortOrder) {
		if (sortOrder == null) {
			return null;
		}
		return "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
	}

	private void validateClubNameUnique(String clubName, Long excludeId) {
		if (!StringUtils.hasText(clubName)) {
			throw new BusinessException(40021, "社团名称不能为空");
		}
		var optional = clubMapper.selectByClubName(clubName);
		if (optional.isPresent() && !Objects.equals(optional.get().getId(), excludeId)) {
			throw new BusinessException(40022, "社团名称已存在");
		}
	}

	private void validateClubCodeUnique(String clubCode, Long excludeId) {
		if (!StringUtils.hasText(clubCode)) {
			return; // 社团代码可以为空
		}
		var optional = clubMapper.selectByClubCode(clubCode);
		if (optional.isPresent() && !Objects.equals(optional.get().getId(), excludeId)) {
			throw new BusinessException(40023, "社团代码已存在");
		}
	}

	private void ensureUserCanCreateClub(Long founderId) {
		if (founderId == null) {
			return;
		}
		Long existingCount = clubMapper.countByFounderId(founderId);
		if (existingCount != null && existingCount > 0) {
			throw new BusinessException(40041, "每位用户仅能创建一个社团，当前账号已存在社团记录");
		}
	}

	private Map<Long, String> loadFounderUsernames(List<Club> clubs) {
		Set<Long> founderIds = clubs.stream()
			.map(Club::getFounderId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		if (founderIds.isEmpty()) {
			return Map.of();
		}
		return userMapper.selectByIds(founderIds.stream().toList()).stream()
			.collect(Collectors.toMap(user -> user.getId(), user -> user.getUsername()));
	}

	private ClubInfo toClubInfo(Club club, Map<Long, String> founderNameMap) {
		String founderUsername = Optional.ofNullable(club.getFounderId())
			.map(founderNameMap::get)
			.orElse(null);
		return toClubInfo(club, founderUsername);
	}

	private ClubInfo toClubInfo(Club club, String founderUsername) {
		return new ClubInfo(
			club.getId(),
			club.getClubName(),
			club.getClubCode(),
			club.getDescription(),
			club.getPurpose(),
			club.getConstitution(),
			club.getLogo(),
			club.getContactPerson(),
			club.getContactPhone(),
			club.getContactEmail(),
			club.getFounderId(),
			founderUsername,
			club.getPresidentId(),
			club.getMemberCount(),
			club.getStatus(),
			club.getEstablishedTime(),
			club.getCreatedTime(),
			club.getUpdatedTime()
		);
	}

	public record ClubCommand(String clubName, String clubCode, String description, String purpose,
							  String constitution, String logo, String contactPerson, String contactPhone,
							  String contactEmail, Long founderId, Long presidentId, Integer memberCount,
							  Integer status, LocalDateTime establishedTime) {
	}

	public record ClubInfo(Long id, String clubName, String clubCode, String description, String purpose,
						   String constitution, String logo, String contactPerson, String contactPhone,
						   String contactEmail, Long founderId, String founderUsername, Long presidentId, Integer memberCount,
						   Integer status, LocalDateTime establishedTime, LocalDateTime createdTime,
						   LocalDateTime updatedTime) {
	}

	public record JoinedClubInfo(Long id, String clubName, String clubCode, String description, String logo,
								 Long founderId, String founderUsername, Integer memberCount, Integer status,
								 String role, LocalDateTime createdTime, LocalDateTime updatedTime) {
	}

	public record ClubLeaderboardItem(Long clubId, String clubName, Long metricValue, String metricLabel) {
	}
}

