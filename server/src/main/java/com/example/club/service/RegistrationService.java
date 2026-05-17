package com.example.club.service;

import com.example.club.domain.Activity;
import com.example.club.domain.Registration;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ActivityMapper;
import com.example.club.mapper.RegistrationMapper;
import com.example.club.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 报名领域服务
 * <p>处理活动报名的增删改查等核心业务逻辑。</p>
 */
@Service
public class RegistrationService {

	private final RegistrationMapper registrationMapper;
	private final ActivityMapper activityMapper;
	private final UserMapper userMapper;
	private final NotificationService notificationService;

	public RegistrationService(RegistrationMapper registrationMapper,
							   ActivityMapper activityMapper,
							   UserMapper userMapper,
							   NotificationService notificationService) {
		this.registrationMapper = registrationMapper;
		this.activityMapper = activityMapper;
		this.userMapper = userMapper;
		this.notificationService = notificationService;
	}

	/**
	 * 分页查询报名列表
	 *
	 * @param activityId 活动ID（可选）
	 * @param userId 用户ID（可选）
	 * @param status 状态过滤（可选）
	 * @param page   页码
	 * @param size   页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<RegistrationInfo> page(Long activityId, Long userId, Integer status, int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = registrationMapper.countByFilters(activityId, userId, status);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Registration> registrations = registrationMapper.selectPage(activityId, userId, status, offset, size);
		// 批量查询用户名
		List<Long> userIds = registrations.stream().map(Registration::getUserId).distinct().collect(Collectors.toList());
		Map<Long, String> usernames = userIds.isEmpty() ? Map.of() :
			userMapper.selectUsernamesByIds(userIds).stream()
				.collect(Collectors.toMap(
					m -> ((Number) m.get("id")).longValue(),
					m -> (String) m.get("username"),
					(v1, v2) -> v1
				));
		List<RegistrationInfo> records = registrations.stream()
			.map(r -> toRegistrationInfo(r, usernames.get(r.getUserId())))
			.toList();
		return new PageResult<>(total, records);
	}

	/**
	 * 查询报名详情
	 *
	 * @param id 报名ID
	 * @return 报名详情
	 */
	@Transactional(readOnly = true)
	public RegistrationInfo getDetail(Long id) {
		Registration registration = registrationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40406, "报名不存在"));
		List<Map<String, Object>> userMaps = userMapper.selectUsernamesByIds(List.of(registration.getUserId()));
		String username = userMaps.isEmpty() ? "未知用户" :
			userMaps.stream()
				.filter(m -> ((Number) m.get("id")).longValue() == registration.getUserId())
				.findFirst()
				.map(m -> (String) m.get("username"))
				.orElse("未知用户");
		return toRegistrationInfo(registration, username);
	}

	/**
	 * 报名活动
	 *
	 * @param activityId 活动ID
	 * @param userId     用户ID
	 * @return 新报名ID
	 */
	@Transactional
	public Long register(Long activityId, Long userId) {
		// 校验活动是否存在
		Activity activity = activityMapper.selectById(activityId)
			.orElseThrow(() -> new BusinessException(40405, "活动不存在"));

		// 校验活动状态
		if (activity.getStatus() == 2 || activity.getStatus() == 3) {
			throw new BusinessException(40039, "活动已结束或已取消，无法报名");
		}

		// 校验报名截止时间
		if (activity.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(activity.getRegistrationDeadline())) {
			throw new BusinessException(40040, "报名已截止");
		}

		// 检查是否已经报名
		Optional<Registration> existing = registrationMapper.selectByActivityIdAndUserId(activityId, userId);
		if (existing.isPresent()) {
			Registration registration = existing.get();
			if (registration.getStatus() == 1) {
				throw new BusinessException(40041, "您已经报名该活动");
			}
			// 如果之前取消过，恢复报名
			registration.setStatus(1);
			registration.setRegistrationTime(LocalDateTime.now());
			registrationMapper.updateById(registration);
			// 更新活动参与人数
			activityMapper.updateCurrentParticipants(activityId, 1);

			// 发送报名成功消息
			sendRegistrationNotification(userId, activity, true);
			return registration.getId();
		}

		// 校验人数限制
		if (activity.getMaxParticipants() != null) {
			if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
				throw new BusinessException(40042, "活动报名人数已满");
			}
		}

		// 创建新报名
		Registration registration = new Registration();
		registration.setActivityId(activityId);
		registration.setUserId(userId);
		registration.setRegistrationTime(LocalDateTime.now());
		registration.setStatus(1); // 已报名
		registrationMapper.insert(registration);

		// 更新活动参与人数
		activityMapper.updateCurrentParticipants(activityId, 1);

		// 发送报名成功消息
		sendRegistrationNotification(userId, activity, true);
		return registration.getId();
	}

	/**
	 * 取消报名
	 *
	 * @param activityId 活动ID
	 * @param userId     用户ID
	 */
	@Transactional
	public void cancel(Long activityId, Long userId) {
		Registration registration = registrationMapper.selectByActivityIdAndUserId(activityId, userId)
			.orElseThrow(() -> new BusinessException(40406, "报名不存在"));

		if (registration.getStatus() == 0) {
			throw new BusinessException(40043, "报名已取消");
		}

		// 校验活动状态
		Activity activity = activityMapper.selectById(activityId)
			.orElseThrow(() -> new BusinessException(40405, "活动不存在"));

		// 活动已开始或已结束，不能取消报名
		if (activity.getStatus() == 1 || activity.getStatus() == 2) {
			throw new BusinessException(40044, "活动已开始或已结束，无法取消报名");
		}

		// 更新报名状态
		registrationMapper.updateStatus(registration.getId(), 0); // 已取消

		// 更新活动参与人数
		activityMapper.updateCurrentParticipants(activityId, -1);

		// 发送报名取消消息
		sendRegistrationNotification(userId, activity, false);
	}

	private void sendRegistrationNotification(Long userId, Activity activity, boolean success) {
		String title = "活动报名" + (success ? "成功" : "取消");
		String statusText = success ? "成功报名" : "取消报名";
		String content = "您已" + statusText + "活动【" + activity.getActivityName() + "】。";
		notificationService.send(
			userId,
			NotificationService.TYPE_ACTIVITY_REGISTRATION,
			title,
			content,
			activity.getId()
		);
	}

	/**
	 * 删除报名
	 *
	 * @param id 报名ID
	 */
	@Transactional
	public void delete(Long id) {
		Registration registration = registrationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40406, "报名不存在"));

		// 如果报名状态为已报名，需要更新活动参与人数
		if (registration.getStatus() == 1) {
			activityMapper.updateCurrentParticipants(registration.getActivityId(), -1);
		}

		registrationMapper.deleteById(id);
	}

	private RegistrationInfo toRegistrationInfo(Registration registration, String username) {
		return new RegistrationInfo(
			registration.getId(),
			registration.getActivityId(),
			registration.getUserId(),
			username,
			registration.getRegistrationTime(),
			registration.getStatus(),
			registration.getCreatedTime(),
			registration.getUpdatedTime()
		);
	}

	public record RegistrationInfo(
		Long id,
		Long activityId,
		Long userId,
		String username,
		LocalDateTime registrationTime,
		Integer status,
		LocalDateTime createdTime,
		LocalDateTime updatedTime
	) {
	}
}

