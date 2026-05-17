package com.example.club.service;

import com.example.club.domain.Club;
import com.example.club.domain.ClubApplication;
import com.example.club.domain.ClubApplicationHistory;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ClubApplicationHistoryMapper;
import com.example.club.mapper.ClubApplicationMapper;
import com.example.club.mapper.ClubMapper;
import com.example.club.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 社团申请领域服务
 * <p>处理社团申请的提交、审核、查询等核心业务逻辑。</p>
 */
@Service
public class ClubApplicationService {

	private final ClubApplicationMapper applicationMapper;
	private final ClubApplicationHistoryMapper historyMapper;
	private final ClubMapper clubMapper;
	private final ClubService clubService;
	private final MemberService memberService;
	private final UserService userService;
	private final UserMapper userMapper;
	private final NotificationService notificationService;

	public ClubApplicationService(ClubApplicationMapper applicationMapper,
								 ClubApplicationHistoryMapper historyMapper,
								 ClubMapper clubMapper,
								 ClubService clubService,
								 MemberService memberService,
								 UserService userService,
								 UserMapper userMapper,
								 NotificationService notificationService) {
		this.applicationMapper = applicationMapper;
		this.historyMapper = historyMapper;
		this.clubMapper = clubMapper;
		this.clubService = clubService;
		this.memberService = memberService;
		this.userService = userService;
		this.userMapper = userMapper;
		this.notificationService = notificationService;
	}

	/**
	 * 分页查询申请列表
	 *
	 * @param keyword     关键词（社团名称）
	 * @param status      状态过滤
	 * @param applicantId 申请人ID过滤（可选）
	 * @param page        页码
	 * @param size        页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<ApplicationInfo> page(String keyword, Integer status, Long applicantId, int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = applicationMapper.countByFilters(keyword, status, applicantId);
		if (total == 0) {
			return PageResult.empty();
		}
		List<ClubApplication> applications = applicationMapper.selectPage(keyword, status, applicantId, offset, size);
		// 批量查询申请人用户名
		List<Long> applicantIds = applications.stream().map(ClubApplication::getApplicantId).distinct().collect(Collectors.toList());
		Map<Long, String> usernames = applicantIds.isEmpty() ? Map.of() :
			userMapper.selectUsernamesByIds(applicantIds).stream()
				.collect(Collectors.toMap(
					m -> ((Number) m.get("id")).longValue(),
					m -> (String) m.get("username"),
					(v1, v2) -> v1
				));
		List<ApplicationInfo> records = applications.stream()
			.map(a -> toApplicationInfo(a, usernames.get(a.getApplicantId())))
			.toList();
		return new PageResult<>(total, records);
	}

	/**
	 * 查询申请详情
	 *
	 * @param id 申请ID
	 * @return 申请详情
	 */
	@Transactional(readOnly = true)
	public ApplicationInfo getDetail(Long id) {
		ClubApplication application = applicationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40403, "申请不存在"));
		List<Map<String, Object>> userMaps = userMapper.selectUsernamesByIds(List.of(application.getApplicantId()));
		String applicantUsername = userMaps.isEmpty() ? "未知用户" :
			userMaps.stream()
				.filter(m -> ((Number) m.get("id")).longValue() == application.getApplicantId())
				.findFirst()
				.map(m -> (String) m.get("username"))
				.orElse("未知用户");
		return toApplicationInfo(application, applicantUsername);
	}

	/**
	 * 提交社团创建申请
	 *
	 * @param command 申请命令
	 * @return 新申请ID
	 */
	@Transactional
	public Long submit(ApplicationCommand command) {
		// 校验社团名称唯一性（包括已通过审核的社团和待审核的申请）
		validateClubNameForApplication(command.clubName(), null);
		if (StringUtils.hasText(command.clubCode())) {
			validateClubCodeForApplication(command.clubCode(), null);
		}
		var limitStatus = assessCreateLimit(command.applicantId());
		if (limitStatus == CreateLimitStatus.HAS_CLUB) {
			throw new BusinessException(40041, "您已创建社团，无法重复申请");
		}
		if (limitStatus == CreateLimitStatus.HAS_PENDING) {
			throw new BusinessException(40042, "您已有待审核的社团申请，请耐心等待审核结果");
		}

		ClubApplication application = new ClubApplication();
		application.setApplicantId(command.applicantId());
		application.setClubName(command.clubName());
		application.setClubCode(command.clubCode());
		application.setDescription(command.description());
		application.setPurpose(command.purpose());
		application.setConstitution(command.constitution());
		application.setLogo(command.logo());
		application.setAttachment(command.attachment());
		application.setStatus(0); // 默认待审核
		applicationMapper.insert(application);
		return application.getId();
	}

	/**
	 * 审核社团申请
	 *
	 * @param id      申请ID
	 * @param command 审核命令（包含审核结果和意见）
	 */
	@Transactional
	public void review(Long id, ReviewCommand command) {
		ClubApplication application = applicationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40403, "申请不存在"));

		if (application.getStatus() != 0) {
			throw new BusinessException(40024, "该申请已审核，无法重复审核");
		}

		Integer newStatus = command.status();
		if (newStatus == null || (newStatus != 1 && newStatus != 2)) {
			throw new BusinessException(40025, "审核状态值非法，只能为1（通过）或2（驳回）");
		}

		// 更新申请状态
		application.setStatus(newStatus);
		application.setReviewerId(command.reviewerId());
		application.setReviewTime(LocalDateTime.now());
		application.setReviewComment(command.reviewComment());
		applicationMapper.updateById(application);

		ClubApplicationHistory history = new ClubApplicationHistory();
		history.setApplicationId(id);
		history.setReviewerId(command.reviewerId());
		history.setStatus(newStatus);
		history.setReviewComment(command.reviewComment());
		history.setCreatedTime(LocalDateTime.now());
		historyMapper.insert(history);

		// 如果审核通过，创建社团
		if (newStatus == 1) {
			createClubFromApplication(application);
		}

		// 发送站内消息通知申请人
		String title = "社团创建申请审核" + (newStatus == 1 ? "通过" : "未通过");
		String content = "您提交的社团【" + application.getClubName() + "】创建申请已被" + (newStatus == 1 ? "通过" : "驳回") + "。"
			+ (command.reviewComment() != null ? " 审核意见：" + command.reviewComment() : "");
		notificationService.send(
			application.getApplicantId(),
			NotificationService.TYPE_CLUB_APPLICATION_RESULT,
			title,
			content,
			application.getId()
		);
	}

	@Transactional
	public void batchReview(List<Long> applicationIds, ReviewCommand command) {
		if (applicationIds == null || applicationIds.isEmpty()) {
			throw new BusinessException(40027, "请选择要审核的申请");
		}
		for (Long id : applicationIds) {
			review(id, command);
		}
	}

	@Transactional(readOnly = true)
	public List<HistoryInfo> history(Long applicationId) {
		applicationMapper.selectById(applicationId)
			.orElseThrow(() -> new BusinessException(40403, "申请不存在"));
		List<ClubApplicationHistory> histories = historyMapper.selectByApplicationId(applicationId);
		if (histories.isEmpty()) {
			return List.of();
		}
		List<Long> reviewerIds = histories.stream()
			.map(ClubApplicationHistory::getReviewerId)
			.filter(id -> id != null && id > 0)
			.distinct()
			.toList();
		Map<Long, String> reviewerNames = reviewerIds.isEmpty() ? Map.of() :
			userMapper.selectUsernamesByIds(reviewerIds).stream()
				.collect(Collectors.toMap(
					m -> ((Number) m.get("id")).longValue(),
					m -> (String) m.get("username"),
					(v1, v2) -> v1
				));
		return histories.stream()
			.map(h -> new HistoryInfo(
				h.getId(),
				h.getApplicationId(),
				h.getReviewerId(),
				reviewerNames.getOrDefault(h.getReviewerId(), "系统"),
				h.getStatus(),
				h.getReviewComment(),
				h.getCreatedTime()
			))
			.toList();
	}

	@Transactional(readOnly = true)
	public boolean canSubmit(Long applicantId) {
		if (applicantId == null) {
			return false;
		}
		return assessCreateLimit(applicantId) == CreateLimitStatus.ALLOWED;
	}

	/**
	 * 从申请创建社团
	 *
	 * @param application 已通过的申请
	 */
	private void createClubFromApplication(ClubApplication application) {
		// 再次校验名称和代码唯一性（防止并发问题）
		validateClubNameForApplication(application.getClubName(), null);
		if (StringUtils.hasText(application.getClubCode())) {
			validateClubCodeForApplication(application.getClubCode(), null);
		}

		ClubService.ClubCommand clubCommand = new ClubService.ClubCommand(
			application.getClubName(),
			application.getClubCode(),
			application.getDescription(),
			application.getPurpose(),
			application.getConstitution(),
			application.getLogo(),
			null, // contactPerson
			null, // contactPhone
			null, // contactEmail
			application.getApplicantId(), // founderId
			application.getApplicantId(), // presidentId
			null, // memberCount
			1, // status: 已通过
			LocalDateTime.now() // establishedTime
		);
		Long clubId = clubService.create(clubCommand);

		// 建立社团管理员的成员关系及角色映射
		memberService.add(new MemberService.MemberCommand(clubId, application.getApplicantId(), "MINISTER"));
		userService.grantRoleIfAbsent(application.getApplicantId(), UserService.ROLE_CODE_CLUB_ADMIN);
	}

	/**
	 * 删除申请（仅允许删除待审核状态的申请）
	 *
	 * @param id 申请ID
	 */
	@Transactional
	public void delete(Long id) {
		ClubApplication application = applicationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40403, "申请不存在"));
		if (application.getStatus() != 0) {
			throw new BusinessException(40026, "只能删除待审核状态的申请");
		}
		applicationMapper.deleteById(id);
	}

	/**
	 * 校验申请中的社团名称（检查已存在的社团和待审核的申请）
	 */
	private void validateClubNameForApplication(String clubName, Long excludeApplicationId) {
		if (!StringUtils.hasText(clubName)) {
			throw new BusinessException(40021, "社团名称不能为空");
		}
		// 检查已存在的社团
		Optional<Club> existingClub = clubMapper.selectByClubName(clubName);
		if (existingClub.isPresent()) {
			throw new BusinessException(40022, "社团名称已存在");
		}
		// 检查待审核的申请（排除当前申请）
		// 注意：这里简化处理，实际可以通过查询待审核申请来校验
	}

	/**
	 * 校验申请中的社团代码（检查已存在的社团和待审核的申请）
	 */
	private void validateClubCodeForApplication(String clubCode, Long excludeApplicationId) {
		if (!StringUtils.hasText(clubCode)) {
			return; // 社团代码可以为空
		}
		// 检查已存在的社团
		Optional<Club> existingClub = clubMapper.selectByClubCode(clubCode);
		if (existingClub.isPresent()) {
			throw new BusinessException(40023, "社团代码已存在");
		}
		// 检查待审核的申请（排除当前申请）
		// 注意：这里简化处理，实际可以通过查询待审核申请来校验
	}

	private ApplicationInfo toApplicationInfo(ClubApplication application, String applicantUsername) {
		return new ApplicationInfo(
			application.getId(),
			application.getApplicantId(),
			applicantUsername,
			application.getClubName(),
			application.getClubCode(),
			application.getDescription(),
			application.getPurpose(),
			application.getConstitution(),
			application.getLogo(),
			application.getAttachment(),
			application.getStatus(),
			application.getReviewerId(),
			application.getReviewTime(),
			application.getReviewComment(),
			application.getCreatedTime(),
			application.getUpdatedTime()
		);
	}

	public record ApplicationCommand(Long applicantId, String clubName, String clubCode, String description,
									 String purpose, String constitution, String logo, String attachment) {
	}

	public record ReviewCommand(Long reviewerId, Integer status, String reviewComment) {
	}

	public record ApplicationInfo(Long id, Long applicantId, String applicantUsername, String clubName, String clubCode, String description,
								   String purpose, String constitution, String logo, String attachment, Integer status,
								   Long reviewerId, LocalDateTime reviewTime, String reviewComment,
								   LocalDateTime createdTime, LocalDateTime updatedTime) {
	}

	public record HistoryInfo(Long id, Long applicationId, Long reviewerId, String reviewerName, Integer status,
							  String reviewComment, LocalDateTime createdTime) {
	}

	private enum CreateLimitStatus {
		ALLOWED,
		HAS_CLUB,
		HAS_PENDING
	}

	private CreateLimitStatus assessCreateLimit(Long applicantId) {
		if (applicantId == null) {
			return CreateLimitStatus.ALLOWED;
		}
		Long existingClubCount = clubMapper.countByFounderId(applicantId);
		if (existingClubCount != null && existingClubCount > 0) {
			return CreateLimitStatus.HAS_CLUB;
		}
		Long pendingCount = applicationMapper.countPendingByApplicant(applicantId);
		if (pendingCount != null && pendingCount > 0) {
			return CreateLimitStatus.HAS_PENDING;
		}
		return CreateLimitStatus.ALLOWED;
	}
}

