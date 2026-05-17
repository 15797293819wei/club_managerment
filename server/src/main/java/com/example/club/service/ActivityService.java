package com.example.club.service;

import com.example.club.domain.Activity;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ActivityMapper;
import com.example.club.mapper.ClubMapper;
import com.example.club.mapper.MemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 活动领域服务
 * <p>处理活动的增删改查、状态变更等核心业务逻辑。</p>
 */
@Service
public class ActivityService {

	private final ActivityMapper activityMapper;
	private final ClubMapper clubMapper;
	private final MemberMapper memberMapper;

	public ActivityService(ActivityMapper activityMapper, ClubMapper clubMapper, MemberMapper memberMapper) {
		this.activityMapper = activityMapper;
		this.clubMapper = clubMapper;
		this.memberMapper = memberMapper;
	}

	/**
	 * 分页查询活动列表
	 *
	 * @param clubId 社团ID（可选）
	 * @param clubName 社团名称关键词（可选，用于搜索社团名称）
	 * @param keyword 关键词（可选，用于搜索活动名称）
	 * @param status 状态过滤（可选）
	 * @param page   页码
	 * @param size   页大小
	 * @return 分页数据
	 */
	public PageResult<ActivityInfo> page(Long clubId,
										 List<Long> clubIds,
										 String clubName,
										 String keyword,
										 Integer status,
										 int page,
										 int size) {
		long offset = (long) (page - 1) * size;
		long total = activityMapper.countByFilters(clubId, clubIds, clubName, keyword, status);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Activity> activities = activityMapper.selectPage(clubId, clubIds, clubName, keyword, status, offset, size);
		activities.forEach(this::refreshStatusIfNeeded);
		// 批量查询社团名称
		Map<Long, String> clubNames = activities.stream()
			.map(Activity::getClubId)
			.distinct()
			.collect(Collectors.toMap(
				id -> id,
				id -> clubMapper.selectById(id).map(c -> c.getClubName()).orElse("未知社团")
			));
		List<ActivityInfo> records = activities.stream()
			.map(a -> toActivityInfo(a, clubNames.get(a.getClubId())))
			.toList();
		return new PageResult<>(total, records);
	}

	/**
	 * 查询活动详情
	 *
	 * @param id 活动ID
	 * @return 活动详情
	 */
	public ActivityInfo getDetail(Long id) {
		Activity activity = activityMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40405, "活动不存在"));
		refreshStatusIfNeeded(activity);
		String clubName = clubMapper.selectById(activity.getClubId())
			.map(c -> c.getClubName())
			.orElse("未知社团");
		return toActivityInfo(activity, clubName);
	}

	private void refreshStatusIfNeeded(Activity activity) {
		if (activity == null) {
			return;
		}
		Integer currentStatus = activity.getStatus();
		if (currentStatus != null && currentStatus == 3) {
			return; // 已取消不自动变更
		}
		LocalDateTime now = LocalDateTime.now();
		Integer newStatus = currentStatus == null ? 0 : currentStatus;
		if (activity.getStartTime() != null) {
			if (activity.getEndTime() != null) {
				if (now.isAfter(activity.getEndTime())) {
					newStatus = 2; // 已结束
				} else if (!now.isBefore(activity.getStartTime())) {
					newStatus = 1; // 进行中
				} else {
					newStatus = 0; // 待开始
				}
			} else {
				newStatus = now.isBefore(activity.getStartTime()) ? 0 : 1;
			}
		}
		if (!Objects.equals(newStatus, currentStatus)) {
			activity.setStatus(newStatus);
			activityMapper.updateStatus(activity.getId(), newStatus);
		}
	}

	/**
	 * 创建活动（发布活动）
	 *
	 * @param command 创建命令
	 * @return 新活动ID
	 */
	@Transactional
	public Long create(ActivityCommand command) {
		// 校验社团是否存在
		clubMapper.selectById(command.clubId())
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));

		// 校验开始时间不能早于当前时间
		if (command.startTime().isBefore(LocalDateTime.now())) {
			throw new BusinessException(40031, "活动开始时间不能早于当前时间");
		}

		// 校验结束时间不能早于开始时间
		if (command.endTime() != null && command.endTime().isBefore(command.startTime())) {
			throw new BusinessException(40032, "活动结束时间不能早于开始时间");
		}

		// 校验报名截止时间不能晚于开始时间
		if (command.registrationDeadline() != null && command.registrationDeadline().isAfter(command.startTime())) {
			throw new BusinessException(40033, "报名截止时间不能晚于活动开始时间");
		}

		// 校验最大参与人数
		if (command.maxParticipants() != null && command.maxParticipants() <= 0) {
			throw new BusinessException(40034, "最大参与人数必须大于0");
		}

		Activity activity = new Activity();
		activity.setClubId(command.clubId());
		activity.setActivityName(command.activityName());
		activity.setActivityType(command.activityType());
		activity.setDescription(command.description());
		activity.setStartTime(command.startTime());
		activity.setEndTime(command.endTime());
		activity.setLocation(command.location());
		activity.setMaxParticipants(command.maxParticipants());
		activity.setCurrentParticipants(0);
		activity.setRegistrationDeadline(command.registrationDeadline());
		activity.setStatus(0); // 待开始
		activity.setCreatorId(command.creatorId());
		activityMapper.insert(activity);

		return activity.getId();
	}

	/**
	 * 更新活动信息
	 *
	 * @param id      活动ID
	 * @param command 更新命令
	 */
	@Transactional
	public void update(Long id, ActivityCommand command) {
		Activity activity = activityMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40405, "活动不存在"));

		// 活动开始后仅允许修改非关键信息（描述、地点），避免影响已发布排期
		if (activity.getStatus() != null && activity.getStatus() != 0) {
			throw new BusinessException(40038, "活动已开始或已结束，不能再修改时间和报名设置，请仅在开始前编辑");
		}

		// 校验开始时间
		if (command.startTime() != null) {
			if (command.startTime().isBefore(LocalDateTime.now())) {
				throw new BusinessException(40031, "活动开始时间不能早于当前时间");
			}
		}

		// 校验结束时间
		LocalDateTime startTime = command.startTime() != null ? command.startTime() : activity.getStartTime();
		if (command.endTime() != null && command.endTime().isBefore(startTime)) {
			throw new BusinessException(40032, "活动结束时间不能早于开始时间");
		}

		// 校验报名截止时间
		if (command.registrationDeadline() != null && command.registrationDeadline().isAfter(startTime)) {
			throw new BusinessException(40033, "报名截止时间不能晚于活动开始时间");
		}

		// 校验最大参与人数不能小于当前参与人数
		if (command.maxParticipants() != null) {
			if (command.maxParticipants() <= 0) {
				throw new BusinessException(40034, "最大参与人数必须大于0");
			}
			if (command.maxParticipants() < activity.getCurrentParticipants()) {
				throw new BusinessException(40036, "最大参与人数不能小于当前参与人数");
			}
		}

		// 更新活动信息
		if (command.activityName() != null) {
			activity.setActivityName(command.activityName());
		}
		if (command.activityType() != null) {
			activity.setActivityType(command.activityType());
		}
		if (command.description() != null) {
			activity.setDescription(command.description());
		}
		if (command.startTime() != null) {
			activity.setStartTime(command.startTime());
		}
		if (command.endTime() != null) {
			activity.setEndTime(command.endTime());
		}
		if (command.location() != null) {
			activity.setLocation(command.location());
		}
		if (command.maxParticipants() != null) {
			activity.setMaxParticipants(command.maxParticipants());
		}
		if (command.registrationDeadline() != null) {
			activity.setRegistrationDeadline(command.registrationDeadline());
		}
		activityMapper.updateById(activity);
	}

	/**
	 * 取消活动
	 *
	 * @param id 活动ID
	 */
	@Transactional
	public void cancel(Long id) {
		Activity activity = activityMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40405, "活动不存在"));

		// 已结束或已取消的活动不能再取消
		if (activity.getStatus() == 2 || activity.getStatus() == 3) {
			throw new BusinessException(40037, "活动已结束或已取消，无法再次取消");
		}

		activityMapper.updateStatus(id, 3); // 已取消
	}

	/**
	 * 删除活动
	 *
	 * @param id 活动ID
	 */
	@Transactional
	public void delete(Long id) {
		// 验证活动存在
		activityMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40405, "活动不存在"));

		// 所有已发布的活动都可以删除（移除状态限制）
		activityMapper.deleteById(id);
	}

	/**
	 * 获取用户管理的社团ID列表
	 * 包括：作为创始人、社长、部长、副部长的社团
	 *
	 * @param userId 用户ID
	 * @return 社团ID列表
	 */
	@Transactional(readOnly = true)
	public List<Long> getManagedClubIds(Long userId) {
		Set<Long> clubIdSet = new HashSet<>();
		
		// 1. 查询作为创始人的社团
		List<com.example.club.domain.Club> founderClubs = clubMapper.selectByFounderId(userId);
		if (founderClubs != null) {
			founderClubs.forEach(c -> clubIdSet.add(c.getId()));
		}
		
		// 2. 查询作为社长的社团
		List<com.example.club.domain.Club> presidentClubs = clubMapper.selectByPresidentId(userId);
		if (presidentClubs != null) {
			presidentClubs.forEach(c -> clubIdSet.add(c.getId()));
		}
		
		// 3. 查询作为部长或副部长的社团
		List<com.example.club.domain.Member> ministerMembers = memberMapper.selectPage(null, userId, "MINISTER", 1, 0L, 1000);
		if (ministerMembers != null) {
			ministerMembers.forEach(m -> clubIdSet.add(m.getClubId()));
		}
		
		List<com.example.club.domain.Member> viceMinisterMembers = memberMapper.selectPage(null, userId, "VICE_MINISTER", 1, 0L, 1000);
		if (viceMinisterMembers != null) {
			viceMinisterMembers.forEach(m -> clubIdSet.add(m.getClubId()));
		}
		
		return new ArrayList<>(clubIdSet);
	}

	@Transactional(readOnly = true)
	public List<Long> getJoinedClubIds(Long userId) {
		return memberMapper.selectActiveClubIdsByUserId(userId);
	}

	/**
	 * 校验用户是否有权限管理该社团的活动
	 * 系统管理员可以管理所有社团的活动，社团管理员只能管理自己社团的活动
	 *
	 * @param clubId       社团ID
	 * @param userId       用户ID
	 * @param isSystemAdmin 是否为系统管理员
	 */
	@Transactional(readOnly = true)
	public void checkManagePermission(Long clubId, Long userId, boolean isSystemAdmin) {
		if (isSystemAdmin) {
			return; // 系统管理员有所有权限
		}
		// 检查是否为社团创始人或社长
		var club = clubMapper.selectById(clubId)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		if (!Objects.equals(club.getFounderId(), userId) && !Objects.equals(club.getPresidentId(), userId)) {
			// 检查是否为该社团的部长或副部长
			var member = memberMapper.selectByClubIdAndUserId(clubId, userId);
			if (member.isEmpty() || member.get().getStatus() != 1) {
				throw new BusinessException(40303, "无权管理该社团的活动");
			}
			String role = member.get().getRole();
			if (!"MINISTER".equals(role) && !"VICE_MINISTER".equals(role)) {
				throw new BusinessException(40303, "无权管理该社团的活动");
			}
		}
	}

	private ActivityInfo toActivityInfo(Activity activity, String clubName) {
		return new ActivityInfo(
			activity.getId(),
			activity.getClubId(),
			clubName,
			activity.getActivityName(),
			activity.getActivityType(),
			activity.getDescription(),
			activity.getStartTime(),
			activity.getEndTime(),
			activity.getLocation(),
			activity.getMaxParticipants(),
			activity.getCurrentParticipants(),
			activity.getRegistrationDeadline(),
			activity.getStatus(),
			activity.getCreatorId(),
			activity.getCreatedTime(),
			activity.getUpdatedTime()
		);
	}

	public record ActivityCommand(
		Long clubId,
		String activityName,
		String activityType,
		String description,
		LocalDateTime startTime,
		LocalDateTime endTime,
		String location,
		Integer maxParticipants,
		LocalDateTime registrationDeadline,
		Long creatorId
	) {
	}

	public record ActivityInfo(
		Long id,
		Long clubId,
		String clubName,
		String activityName,
		String activityType,
		String description,
		LocalDateTime startTime,
		LocalDateTime endTime,
		String location,
		Integer maxParticipants,
		Integer currentParticipants,
		LocalDateTime registrationDeadline,
		Integer status,
		Long creatorId,
		LocalDateTime createdTime,
		LocalDateTime updatedTime
	) {
	}
}

