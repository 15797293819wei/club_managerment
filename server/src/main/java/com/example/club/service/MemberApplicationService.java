package com.example.club.service;

import com.example.club.domain.Member;
import com.example.club.domain.MemberApplication;
import com.example.club.dto.PageResult;
import com.example.club.dto.statistics.MemberParticipationDTO;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ClubMapper;
import com.example.club.mapper.MemberApplicationMapper;
import com.example.club.mapper.MemberMapper;
import com.example.club.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 入社申请领域服务
 * <p>处理入社申请的提交、审核、查询等核心业务逻辑。</p>
 */
@Service
public class MemberApplicationService {

	private final MemberApplicationMapper applicationMapper;
	private final ClubMapper clubMapper;
	private final MemberMapper memberMapper;
	private final MemberService memberService;
	private final UserMapper userMapper;
	private final ClubService clubService;
	private final StatisticsService statisticsService;
	private final NotificationService notificationService;

	public MemberApplicationService(MemberApplicationMapper applicationMapper,
									ClubMapper clubMapper,
									MemberMapper memberMapper,
									MemberService memberService,
									UserMapper userMapper,
									ClubService clubService,
									StatisticsService statisticsService,
									NotificationService notificationService) {
		this.applicationMapper = applicationMapper;
		this.clubMapper = clubMapper;
		this.memberMapper = memberMapper;
		this.memberService = memberService;
		this.userMapper = userMapper;
		this.clubService = clubService;
		this.statisticsService = statisticsService;
		this.notificationService = notificationService;
	}

	/**
	 * 分页查询申请列表
	 *
	 * @param clubId     社团ID（可选）
	 * @param applicantId 申请人ID（可选）
	 * @param status     状态过滤（可选）
	 * @param page        页码
	 * @param size        页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<ApplicationInfo> page(Long clubId, Long applicantId, Integer status, int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = applicationMapper.countByFilters(clubId, applicantId, status);
		if (total == 0) {
			return PageResult.empty();
		}
		List<MemberApplication> applications = applicationMapper.selectPage(clubId, applicantId, status, offset, size);
		// 批量查询社团名称和用户名
		Map<Long, String> clubNames = applications.stream()
			.map(MemberApplication::getClubId)
			.distinct()
			.collect(Collectors.toMap(
				id -> id,
				id -> clubMapper.selectById(id).map(c -> c.getClubName()).orElse("未知社团")
			));
		List<Long> applicantIds = applications.stream().map(MemberApplication::getApplicantId).distinct().collect(Collectors.toList());
		Map<Long, String> usernames = applicantIds.isEmpty() ? Map.of() :
			userMapper.selectUsernamesByIds(applicantIds).stream()
				.collect(Collectors.toMap(
					m -> ((Number) m.get("id")).longValue(),
					m -> (String) m.get("username"),
					(v1, v2) -> v1
				));
		List<ApplicationInfo> records = applications.stream()
			.map(a -> toApplicationInfo(a, clubNames.get(a.getClubId()), usernames.get(a.getApplicantId())))
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
		MemberApplication application = applicationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40405, "申请不存在"));
		String clubName = clubMapper.selectById(application.getClubId())
			.map(c -> c.getClubName())
			.orElse("未知社团");
		List<Map<String, Object>> userMaps = userMapper.selectUsernamesByIds(List.of(application.getApplicantId()));
		String applicantUsername = userMaps.isEmpty() ? "未知用户" :
			userMaps.stream()
				.filter(m -> ((Number) m.get("id")).longValue() == application.getApplicantId())
				.findFirst()
				.map(m -> (String) m.get("username"))
				.orElse("未知用户");
		return toApplicationInfo(application, clubName, applicantUsername);
	}

	/**
	 * 提交入社申请
	 *
	 * @param command 申请命令
	 * @return 新申请ID
	 */
	@Transactional
	public Long submit(ApplicationCommand command) {
		// 校验社团是否存在且已通过审核
		var club = clubMapper.selectById(command.clubId())
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		if (club.getStatus() != 1) {
			throw new BusinessException(40031, "该社团尚未通过审核，无法申请加入");
		}

		// 检查是否已经是成员
		Optional<Member> existingMember = memberMapper.selectByClubIdAndUserId(command.clubId(), command.applicantId());
		if (existingMember.isPresent() && existingMember.get().getStatus() == 1) {
			throw new BusinessException(40027, "您已是该社团成员");
		}

		// 检查是否有待审核的申请
		Optional<MemberApplication> pendingApplication = applicationMapper.selectPendingByClubIdAndApplicantId(
			command.clubId(), command.applicantId());
		if (pendingApplication.isPresent()) {
			throw new BusinessException(40032, "您已有待审核的入社申请");
		}

		// 创建申请
		MemberApplication application = new MemberApplication();
		application.setClubId(command.clubId());
		application.setApplicantId(command.applicantId());
		application.setApplicationReason(command.applicationReason());
		application.setStatus(0); // 待审核
		applicationMapper.insert(application);
		return application.getId();
	}

	/**
	 * 审核入社申请
	 *
	 * @param id      申请ID
	 * @param command 审核命令（包含审核结果和意见）
	 */
	@Transactional
	public void review(Long id, ReviewCommand command) {
		MemberApplication application = applicationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40405, "申请不存在"));

		if (application.getStatus() != 0) {
			throw new BusinessException(40033, "该申请已审核，无法重复审核");
		}

		Integer newStatus = command.status();
		if (newStatus == null || (newStatus != 1 && newStatus != 2)) {
			throw new BusinessException(40034, "审核状态值非法，只能为1（通过）或2（驳回）");
		}

		// 更新申请状态
		application.setStatus(newStatus);
		application.setReviewerId(command.reviewerId());
		application.setReviewTime(LocalDateTime.now());
		application.setReviewComment(command.reviewComment());
		applicationMapper.updateById(application);

		// 如果审核通过，添加成员
		if (newStatus == 1) {
			MemberService.MemberCommand memberCommand = new MemberService.MemberCommand(
				application.getClubId(),
				application.getApplicantId(),
				"MEMBER" // 默认普通成员
			);
			memberService.add(memberCommand);
		}

		// 发送站内消息通知申请人
		String clubName = clubMapper.selectById(application.getClubId())
			.map(c -> c.getClubName())
			.orElse("未知社团");
		String title = "入社申请审核" + (newStatus == 1 ? "通过" : "未通过");
		String content = "您申请加入社团【" + clubName + "】的请求已被" + (newStatus == 1 ? "通过" : "驳回") + "。"
			+ (command.reviewComment() != null ? " 审核意见：" + command.reviewComment() : "");
		notificationService.send(
			application.getApplicantId(),
			NotificationService.TYPE_MEMBER_APPLICATION_RESULT,
			title,
			content,
			application.getId()
		);
	}

	@Transactional
	public void batchReview(Long clubId, List<Long> applicationIds, ReviewCommand command) {
		if (clubId == null) {
			throw new BusinessException(40041, "必须指定社团ID");
		}
		if (applicationIds == null || applicationIds.isEmpty()) {
			throw new BusinessException(40042, "请选择至少一条申请");
		}
		for (Long applicationId : applicationIds) {
			ApplicationInfo info = getDetail(applicationId);
			if (!Objects.equals(info.clubId(), clubId)) {
				throw new BusinessException(40043, "存在不属于该社团的申请");
			}
			review(applicationId, command);
		}
	}

	@Transactional(readOnly = true)
	public ApplicationInspection getInspectionDetail(Long id) {
		ApplicationInfo info = getDetail(id);
		var user = userMapper.selectById(info.applicantId())
			.orElseThrow(() -> new BusinessException(40401, "申请人不存在"));
		List<MemberApplication> historyEntities = applicationMapper.selectRecentByApplicant(info.applicantId(), 5);
		List<ApplicationHistoryItem> history = historyEntities.stream()
			.map(item -> new ApplicationHistoryItem(
				item.getId(),
				item.getClubId(),
				item.getStatus(),
				item.getReviewComment(),
				item.getCreatedTime(),
				item.getUpdatedTime()
			))
			.toList();
		boolean alreadyMember = memberMapper.selectByClubIdAndUserId(info.clubId(), info.applicantId())
			.filter(m -> m.getStatus() == 1)
			.isPresent();
		List<ClubService.JoinedClubInfo> joinedClubs = clubService.getJoinedClubs(info.applicantId());
		MemberParticipationDTO participation = null;
		try {
			participation = statisticsService.getMemberParticipationDetail(info.clubId(), info.applicantId(), null, null);
		} catch (BusinessException ignored) {
		}
		ApplicantProfile profile = new ApplicantProfile(
			user.getId(),
			user.getUsername(),
			user.getRealName(),
			user.getStudentId(),
			user.getEmail(),
			user.getPhone(),
			user.getAvatar()
		);
		return new ApplicationInspection(info, profile, joinedClubs, participation, history, alreadyMember);
	}

	/**
	 * 删除申请（仅允许删除待审核状态的申请）
	 *
	 * @param id 申请ID
	 */
	@Transactional
	public void delete(Long id) {
		MemberApplication application = applicationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40405, "申请不存在"));
		if (application.getStatus() != 0) {
			throw new BusinessException(40035, "只能删除待审核状态的申请");
		}
		applicationMapper.deleteById(id);
	}

	private ApplicationInfo toApplicationInfo(MemberApplication application, String clubName, String applicantUsername) {
		return new ApplicationInfo(
			application.getId(),
			application.getClubId(),
			clubName,
			application.getApplicantId(),
			applicantUsername,
			application.getApplicationReason(),
			application.getStatus(),
			application.getReviewerId(),
			application.getReviewTime(),
			application.getReviewComment(),
			application.getCreatedTime(),
			application.getUpdatedTime()
		);
	}

	public record ApplicationCommand(Long clubId, Long applicantId, String applicationReason) {
	}

	public record ReviewCommand(Long reviewerId, Integer status, String reviewComment) {
	}

	public record ApplicationInfo(Long id, Long clubId, String clubName, Long applicantId, String applicantUsername, String applicationReason, Integer status,
								  Long reviewerId, LocalDateTime reviewTime, String reviewComment,
								  LocalDateTime createdTime, LocalDateTime updatedTime) {
	}

	public record ApplicantProfile(Long userId, String username, String realName, String studentId,
								   String email, String phone, String avatar) {
	}

	public record ApplicationHistoryItem(Long id, Long clubId, Integer status, String reviewComment,
										 LocalDateTime createdTime, LocalDateTime updatedTime) {
	}

	public record ApplicationInspection(ApplicationInfo application,
										ApplicantProfile applicant,
										List<ClubService.JoinedClubInfo> otherClubs,
										MemberParticipationDTO participation,
										List<ApplicationHistoryItem> history,
										boolean alreadyMember) {
	}
}

