package com.example.club.service;

import com.example.club.domain.Club;
import com.example.club.domain.Member;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ClubMapper;
import com.example.club.mapper.MemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

/**
 * 社团管理员交接等高级操作
 */
@Service
public class ClubOwnershipService {

	private final ClubMapper clubMapper;
	private final MemberMapper memberMapper;
	private final MemberService memberService;
	private final UserService userService;
	private final NotificationService notificationService;

	public ClubOwnershipService(ClubMapper clubMapper,
								MemberMapper memberMapper,
								MemberService memberService,
								UserService userService,
								NotificationService notificationService) {
		this.clubMapper = clubMapper;
		this.memberMapper = memberMapper;
		this.memberService = memberService;
		this.userService = userService;
		this.notificationService = notificationService;
	}

	/**
	 * 转交社团管理员
	 *
	 * @param clubId               社团ID
	 * @param targetUserId         新管理员用户ID
	 * @param operatorId           操作人用户ID
	 * @param operatorIsSystemAdmin 是否为系统管理员
	 */
	@Transactional
	public void transferAdmin(Long clubId, Long targetUserId, Long operatorId, boolean operatorIsSystemAdmin) {
		Club club = clubMapper.selectById(clubId)
			.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
		Long currentAdminId = club.getFounderId();
		if (currentAdminId == null) {
			throw new BusinessException(50030, "社团管理员信息缺失，无法转交");
		}
		if (!operatorIsSystemAdmin && !Objects.equals(currentAdminId, operatorId)) {
			throw new BusinessException(40303, "仅系统管理员或当前社团管理员可执行转交操作");
		}
		if (Objects.equals(currentAdminId, targetUserId)) {
			throw new BusinessException(40040, "目标用户已是该社团管理员");
		}

		Member targetMember = memberMapper.selectByClubIdAndUserId(clubId, targetUserId)
			.orElseThrow(() -> new BusinessException(40404, "目标用户尚未加入该社团"));
		if (targetMember.getStatus() == null || targetMember.getStatus() != 1) {
			throw new BusinessException(40036, "目标成员状态异常，无法成为社团管理员");
		}

		// 更新目标成员角色为社长（PRESIDENT）
		// 注意：社长角色在members表中不存储，而是通过club表的presidentId字段标识
		// 但为了兼容性，我们仍然更新成员的角色（可以设置为MINISTER，但实际显示时会根据presidentId判断为PRESIDENT）
		if (!"MINISTER".equals(targetMember.getRole())) {
			memberMapper.updateRole(targetMember.getId(), "MINISTER");
		}

		// 将原管理员（社长）降级为普通成员（MEMBER）
		Long oldPresidentId = club.getPresidentId();
		if (oldPresidentId != null && !Objects.equals(oldPresidentId, currentAdminId)) {
			// 如果原社长不是创始人，将其降级为普通成员
			memberMapper.selectByClubIdAndUserId(clubId, oldPresidentId)
				.ifPresentOrElse(
					member -> {
						memberMapper.updateRole(member.getId(), "MEMBER");
					},
					() -> {
						// 如果原社长不在成员表中，添加为普通成员
						memberService.add(new MemberService.MemberCommand(clubId, oldPresidentId, "MEMBER"));
					}
				);
		}
		
		// 将原创始人降级为普通成员（如果原创始人不是原社长）
		if (!Objects.equals(currentAdminId, oldPresidentId)) {
			memberMapper.selectByClubIdAndUserId(clubId, currentAdminId)
				.ifPresentOrElse(
					member -> {
						memberMapper.updateRole(member.getId(), "MEMBER");
					},
					() -> memberService.add(new MemberService.MemberCommand(clubId, currentAdminId, "MEMBER"))
				);
		}

		// 更新社团创始人（管理员）以及社长信息
		clubMapper.updateFounder(clubId, targetUserId);
		Club updatePresident = new Club();
		updatePresident.setId(clubId);
		updatePresident.setPresidentId(targetUserId);
		clubMapper.updateById(updatePresident);

		// 处理全局角色
		userService.grantRoleIfAbsent(targetUserId, UserService.ROLE_CODE_CLUB_ADMIN);
		Long remainCount = clubMapper.countByFounderId(currentAdminId);
		if (remainCount == null || remainCount == 0L) {
			userService.revokeRoleIfPresent(currentAdminId, UserService.ROLE_CODE_CLUB_ADMIN);
		}

		// 通知被变更用户
		String clubName = club.getClubName() != null ? club.getClubName() : "未知社团";
		notificationService.send(
			targetUserId,
			NotificationService.TYPE_ROLE_CHANGED,
			"社团角色变更通知",
			"您已成为社团【" + clubName + "】的社长。",
			clubId
		);
		Set<Long> downgradeUserIds = new HashSet<>();
		if (oldPresidentId != null && !Objects.equals(oldPresidentId, targetUserId)) {
			downgradeUserIds.add(oldPresidentId);
		}
		if (!Objects.equals(currentAdminId, targetUserId)) {
			downgradeUserIds.add(currentAdminId);
		}
		for (Long userId : downgradeUserIds) {
			notificationService.send(
				userId,
				NotificationService.TYPE_ROLE_CHANGED,
				"社团角色变更通知",
				"您在社团【" + clubName + "】的角色已变更为：成员。",
				clubId
			);
		}
	}
}


