package com.example.club.service;

import com.example.club.domain.Activity;
import com.example.club.domain.Evaluation;
import com.example.club.domain.Registration;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ActivityMapper;
import com.example.club.mapper.EvaluationMapper;
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
 * 评价领域服务
 * <p>处理活动评价的增删改查、评分统计等核心业务逻辑。</p>
 */
@Service
public class EvaluationService {

	private final EvaluationMapper evaluationMapper;
	private final ActivityMapper activityMapper;
	private final RegistrationMapper registrationMapper;
	private final UserMapper userMapper;
	private final NotificationService notificationService;

	public EvaluationService(EvaluationMapper evaluationMapper,
							 ActivityMapper activityMapper,
							 RegistrationMapper registrationMapper,
							 UserMapper userMapper,
							 NotificationService notificationService) {
		this.evaluationMapper = evaluationMapper;
		this.activityMapper = activityMapper;
		this.registrationMapper = registrationMapper;
		this.userMapper = userMapper;
		this.notificationService = notificationService;
	}

	/**
	 * 分页查询评价列表
	 *
	 * @param activityId 活动ID（可选）
	 * @param userId 用户ID（可选）
	 * @param page   页码
	 * @param size   页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<EvaluationInfo> page(Long activityId, Long userId, int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = evaluationMapper.countByFilters(activityId, userId);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Evaluation> evaluations = evaluationMapper.selectPage(activityId, userId, offset, size);
		// 批量查询用户名
		List<Long> userIds = evaluations.stream().map(Evaluation::getUserId).distinct().collect(Collectors.toList());
		Map<Long, String> usernames = userIds.isEmpty() ? Map.of() :
			userMapper.selectUsernamesByIds(userIds).stream()
				.collect(Collectors.toMap(
					m -> ((Number) m.get("id")).longValue(),
					m -> (String) m.get("username"),
					(v1, v2) -> v1
				));
		List<EvaluationInfo> records = evaluations.stream()
			.map(e -> toEvaluationInfo(e, usernames.get(e.getUserId())))
			.toList();
		return new PageResult<>(total, records);
	}

	/**
	 * 查询评价详情
	 *
	 * @param id 评价ID
	 * @return 评价详情
	 */
	@Transactional(readOnly = true)
	public EvaluationInfo getDetail(Long id) {
		Evaluation evaluation = evaluationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40408, "评价不存在"));
		List<Map<String, Object>> userMaps = userMapper.selectUsernamesByIds(List.of(evaluation.getUserId()));
		String username = userMaps.isEmpty() ? "未知用户" :
			userMaps.stream()
				.filter(m -> ((Number) m.get("id")).longValue() == evaluation.getUserId())
				.findFirst()
				.map(m -> (String) m.get("username"))
				.orElse("未知用户");
		return toEvaluationInfo(evaluation, username);
	}

	/**
	 * 创建评价
	 *
	 * @param activityId 活动ID
	 * @param userId     用户ID
	 * @param rating     评分（1-5分）
	 * @param comment    评价内容（可选）
	 * @return 新评价ID
	 */
	@Transactional
	public Long create(Long activityId, Long userId, Integer rating, String comment) {
		// 校验活动是否存在
		Activity activity = activityMapper.selectById(activityId)
			.orElseThrow(() -> new BusinessException(40405, "活动不存在"));

		// 校验活动状态（只有已结束的活动可以评价）
		if (activity.getStatus() != 2) {
			throw new BusinessException(40049, "只有已结束的活动可以评价");
		}

		// 校验是否已报名
		Optional<Registration> registration = registrationMapper.selectByActivityIdAndUserId(activityId, userId);
		if (registration.isEmpty() || registration.get().getStatus() != 1) {
			throw new BusinessException(40050, "只有已报名的用户才能评价");
		}

		// 检查是否已经评价
		Optional<Evaluation> existing = evaluationMapper.selectByActivityIdAndUserId(activityId, userId);
		if (existing.isPresent()) {
			throw new BusinessException(40051, "您已经评价过该活动");
		}

		// 校验评分
		if (rating == null || rating < 1 || rating > 5) {
			throw new BusinessException(40052, "评分必须在1-5分之间");
		}

		// 创建新评价
		Evaluation evaluation = new Evaluation();
		evaluation.setActivityId(activityId);
		evaluation.setUserId(userId);
		evaluation.setRating(rating);
		evaluation.setComment(comment);
		evaluationMapper.insert(evaluation);

		// 通知活动创建者收到了新的评价
		notifyActivityOwner(activity, userId, rating, comment);

		return evaluation.getId();
	}

	/**
	 * 更新评价
	 *
	 * @param id      评价ID
	 * @param rating  评分（1-5分）
	 * @param comment 评价内容（可选）
	 */
	@Transactional
	public void update(Long id, Integer rating, String comment) {
		Evaluation evaluation = evaluationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40408, "评价不存在"));

		// 校验评分
		if (rating != null && (rating < 1 || rating > 5)) {
			throw new BusinessException(40052, "评分必须在1-5分之间");
		}

		if (rating != null) {
			evaluation.setRating(rating);
		}
		if (comment != null) {
			evaluation.setComment(comment);
		}
		evaluationMapper.updateById(evaluation);
	}

	/**
	 * 删除评价
	 *
	 * @param id 评价ID
	 */
	@Transactional
	public void delete(Long id) {
		evaluationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40408, "评价不存在"));
		evaluationMapper.deleteById(id);
		// 删除不产生通知，只更新列表与统计
	}

	/**
	 * 获取活动的评分统计
	 *
	 * @param activityId 活动ID
	 * @return 评分统计信息
	 */
	@Transactional(readOnly = true)
	public RatingStatistics getRatingStatistics(Long activityId) {
		// 校验活动是否存在
		activityMapper.selectById(activityId)
			.orElseThrow(() -> new BusinessException(40405, "活动不存在"));

		Double averageRating = evaluationMapper.selectAverageRating(activityId);
		Long count = evaluationMapper.countByActivityId(activityId);

		return new RatingStatistics(
			activityId,
			averageRating != null ? averageRating : 0.0,
			count != null ? count : 0L
		);
	}

	private void notifyActivityOwner(Activity activity, Long evaluatorUserId, Integer rating, String comment) {
		if (activity.getCreatorId() == null || activity.getCreatorId().equals(evaluatorUserId)) {
			return;
		}
		List<Map<String, Object>> userMaps = userMapper.selectUsernamesByIds(List.of(evaluatorUserId));
		String username = userMaps.isEmpty() ? "未知用户" :
			userMaps.stream()
				.filter(m -> ((Number) m.get("id")).longValue() == evaluatorUserId)
				.findFirst()
				.map(m -> (String) m.get("username"))
				.orElse("未知用户");
		String title = "活动收到新的评价";
		String content = "您的活动【" + activity.getActivityName() + "】收到来自用户【" + username + "】的评价：评分 " + rating
			+ " 分" + (comment != null && !comment.isBlank() ? "，内容：" + comment : "。");
		notificationService.send(
			activity.getCreatorId(),
			NotificationService.TYPE_EVALUATION_NOTICE,
			title,
			content,
			activity.getId()
		);
	}

	private EvaluationInfo toEvaluationInfo(Evaluation evaluation, String username) {
		return new EvaluationInfo(
			evaluation.getId(),
			evaluation.getActivityId(),
			evaluation.getUserId(),
			username,
			evaluation.getRating(),
			evaluation.getComment(),
			evaluation.getCreatedTime(),
			evaluation.getUpdatedTime()
		);
	}

	public record EvaluationInfo(
		Long id,
		Long activityId,
		Long userId,
		String username,
		Integer rating,
		String comment,
		LocalDateTime createdTime,
		LocalDateTime updatedTime
	) {
	}

	public record RatingStatistics(
		Long activityId,
		Double averageRating,
		Long count
	) {
	}
}

