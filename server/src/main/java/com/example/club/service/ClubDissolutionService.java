package com.example.club.service;

import com.example.club.domain.Club;
import com.example.club.domain.ClubDissolutionApplication;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ClubDissolutionApplicationMapper;
import com.example.club.mapper.ClubMapper;
import com.example.club.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 社团解散申请领域服务
 * 负责处理社团解散申请的提交与审核逻辑
 */
@Service
public class ClubDissolutionService {

	private final ClubDissolutionApplicationMapper dissolutionApplicationMapper;
	private final ClubMapper clubMapper;
	private final ClubService clubService;
	private final NotificationService notificationService;
	private final UserMapper userMapper;

	public ClubDissolutionService(ClubDissolutionApplicationMapper dissolutionApplicationMapper,
								  ClubMapper clubMapper,
								  ClubService clubService,
								  NotificationService notificationService,
								  UserMapper userMapper) {
		this.dissolutionApplicationMapper = dissolutionApplicationMapper;
		this.clubMapper = clubMapper;
		this.clubService = clubService;
		this.notificationService = notificationService;
		this.userMapper = userMapper;
	}

	/**
	 * 提交社团解散申请（由社长/社团管理员发起）
	 */
	@Transactional
	public Long submit(Long clubId, Long applicantId, String reason) {
		if (clubId == null || applicantId == null) {
			throw new BusinessException(40050, "参数不能为空");
		}
		if (reason == null || reason.trim().isEmpty()) {
			throw new BusinessException(40051, "解散原因不能为空");
		}

		Club club = clubMapper.selectById(clubId)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		if (club.getStatus() != null && club.getStatus() == 3) {
			throw new BusinessException(40030, "社团已处于解散状态");
		}

		// 检查是否已有待审核的解散申请
		Long pendingCount = dissolutionApplicationMapper.countPendingByClubId(clubId);
		if (pendingCount != null && pendingCount > 0) {
			throw new BusinessException(40052, "该社团已存在待审核的解散申请，请勿重复提交");
		}

		ClubDissolutionApplication application = new ClubDissolutionApplication();
		application.setClubId(clubId);
		application.setApplicantId(applicantId);
		application.setReason(reason);
		application.setStatus(0); // 待审核
		dissolutionApplicationMapper.insert(application);

		// 通知所有系统管理员有新的解散申请
		notifySystemAdminsNewApplication(club, application);

		return application.getId();
	}

	/**
	 * 审核社团解散申请（系统管理员）
	 */
	@Transactional
	public void review(Long id, Long reviewerId, Integer status, String reviewComment) {
		ClubDissolutionApplication application = dissolutionApplicationMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40404, "解散申请不存在"));

		if (application.getStatus() == null || application.getStatus() != 0) {
			throw new BusinessException(40053, "该解散申请已审核，无法重复审核");
		}
		if (status == null || (status != 1 && status != 2)) {
			throw new BusinessException(40054, "审核状态值非法，只能为1（通过）或2（驳回）");
		}

		application.setStatus(status);
		application.setReviewerId(reviewerId);
		application.setReviewTime(LocalDateTime.now());
		application.setReviewComment(reviewComment);
		dissolutionApplicationMapper.updateById(application);

		// 审核通过时真正解散社团
		if (status == 1) {
			clubService.dissolveClub(application.getClubId(), reviewerId, true);
		}

		// 通知申请人审核结果
		notifyApplicantReviewResult(application);
	}

	private void notifySystemAdminsNewApplication(Club club, ClubDissolutionApplication application) {
		// 查询所有启用的用户，逐个判断是否拥有 SYSTEM_ADMIN 角色
		List<Long> activeUserIds = userMapper.selectActiveUserIds();
		if (activeUserIds == null || activeUserIds.isEmpty()) {
			return;
		}
		for (Long userId : activeUserIds) {
			Set<String> roleCodes = userMapper.selectRoleCodesByUserId(userId);
			if (roleCodes != null && roleCodes.contains("SYSTEM_ADMIN")) {
				String title = "社团解散申请待审核";
				String content = "社团【" + club.getClubName() + "】提交了解散申请，原因："
					+ (application.getReason() != null ? application.getReason() : "未填写");
				notificationService.send(
					userId,
					NotificationService.TYPE_CLUB_DISSOLUTION_APPLICATION,
					title,
					content,
					application.getId()
				);
			}
		}
	}

	private void notifyApplicantReviewResult(ClubDissolutionApplication application) {
		Long applicantId = application.getApplicantId();
		if (applicantId == null) {
			return;
		}
		Club club = clubMapper.selectById(application.getClubId())
			.orElse(null);
		String clubName = club != null ? club.getClubName() : "相关社团";

		String title = "社团解散申请审核" + (application.getStatus() != null && application.getStatus() == 1 ? "通过" : "未通过");
		String content = "您提交的社团【" + clubName + "】解散申请已被"
			+ (application.getStatus() != null && application.getStatus() == 1 ? "通过" : "驳回") + "。"
			+ (application.getReviewComment() != null ? " 审核意见：" + application.getReviewComment() : "");

		notificationService.send(
			applicantId,
			NotificationService.TYPE_CLUB_APPLICATION_RESULT,
			title,
			content,
			application.getId()
		);
	}
}

