package com.example.club.service;

import com.example.club.domain.Announcement;
import com.example.club.domain.Club;
import com.example.club.domain.User;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.AnnouncementMapper;
import com.example.club.mapper.ClubMapper;
import com.example.club.mapper.MemberMapper;
import com.example.club.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 公告服务
@Service
public class AnnouncementService {

	private final AnnouncementMapper announcementMapper;
	private final MemberMapper memberMapper;
	private final UserMapper userMapper;
	private final ClubMapper clubMapper;
	private final MemberService memberService;
	private final NotificationService notificationService;

	public AnnouncementService(AnnouncementMapper announcementMapper,
							   MemberMapper memberMapper,
							   UserMapper userMapper,
							   ClubMapper clubMapper,
							   MemberService memberService,
							   NotificationService notificationService) {
		this.announcementMapper = announcementMapper;
		this.memberMapper = memberMapper;
		this.userMapper = userMapper;
		this.clubMapper = clubMapper;
		this.memberService = memberService;
		this.notificationService = notificationService;
	}

	 // 获取社团公告列表
	@Transactional(readOnly = true)
	public PageResult<AnnouncementView> pageForAudience(Long userId, int page, int size) {
		List<Long> clubIds = memberMapper.selectActiveClubIdsByUserId(userId);
		long offset = (long) (page - 1) * size;
		long total = announcementMapper.countForAudience(clubIds);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Announcement> announcements = announcementMapper.selectForAudience(clubIds, offset, size);
		return new PageResult<>(total, announcements.stream().map(this::toView).toList());
	}

	// 获取系统公告
	@Transactional(readOnly = true)
	public PageResult<AnnouncementView> pageForManage(Long userId, boolean systemAdmin,
													  String scopeType, Long clubId,
													  int page, int size) {
		if (!systemAdmin && clubId != null) {
			memberService.checkManagePermission(clubId, userId, false);
		}
		long offset = (long) (page - 1) * size;
		long total = announcementMapper.countForManage(userId, scopeType, clubId, systemAdmin);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Announcement> list = announcementMapper.selectForManage(userId, scopeType, clubId, systemAdmin, offset, size);
		return new PageResult<>(total, list.stream().map(this::toView).toList());
	}

	// 发布公告
	@Transactional
	public void publish(AnnouncementPublishCommand command, boolean systemAdmin) {
		if (!StringUtils.hasText(command.title())) {
			throw new BusinessException(40050, "公告标题不能为空");
		}
		if (!StringUtils.hasText(command.content())) {
			throw new BusinessException(40051, "公告内容不能为空");
		}
		User publisher = userMapper.selectById(command.publisherId())
			.orElseThrow(() -> new BusinessException(40400, "发布人不存在"));
		String publisherName = Optional.ofNullable(publisher.getRealName())
			.filter(StringUtils::hasText)
			.orElse(publisher.getUsername());

		String scopeType = systemAdmin ? "GLOBAL" : "CLUB";
		Long clubId = systemAdmin ? null : command.clubId();
		String clubName = null;
		if (!systemAdmin) {
			if (clubId == null) {
				throw new BusinessException(40052, "请选择要发布公告的社团");
			}
			memberService.checkManagePermission(clubId, command.publisherId(), false);
			Club club = clubMapper.selectById(clubId)
				.orElseThrow(() -> new BusinessException(40402, "社团不存在"));
			clubName = club.getClubName();
		} else if (clubId != null) {
			// 即使系统管理员传入 clubId，也仅作为展示用途
			clubName = clubMapper.selectById(clubId)
				.map(Club::getClubName)
				.orElse(null);
		}

		Announcement announcement = new Announcement();
		announcement.setTitle(command.title().trim());
		announcement.setContent(command.content().trim());
		announcement.setScopeType(scopeType);
		announcement.setClubId(clubId);
		announcement.setClubName(clubName);
		announcement.setPublisherId(command.publisherId());
		announcement.setPublisherName(publisherName);
		announcement.setPublisherRole(systemAdmin ? "SYSTEM_ADMIN" : "CLUB_ADMIN");
		announcement.setCreatedTime(LocalDateTime.now());
		announcement.setUpdatedTime(LocalDateTime.now());
		announcementMapper.insert(announcement);
		notifyAudience(announcement, systemAdmin);
	}

	private AnnouncementView toView(Announcement announcement) {
		return new AnnouncementView(
			announcement.getId(),
			announcement.getTitle(),
			announcement.getContent(),
			announcement.getScopeType(),
			announcement.getClubId(),
			announcement.getClubName(),
			announcement.getPublisherName(),
			announcement.getPublisherRole(),
			announcement.getCreatedTime()
		);
	}

	public record AnnouncementPublishCommand(Long publisherId, String title, String content, Long clubId) {
	}

	public record AnnouncementView(Long id,
								   String title,
								   String content,
								   String scopeType,
								   Long clubId,
								   String clubName,
								   String publisherName,
								   String publisherRole,
								   LocalDateTime createdTime) {
	}

	private void notifyAudience(Announcement announcement, boolean systemAdmin) {
		List<Long> userIds = systemAdmin
			? userMapper.selectActiveUserIds()
			: memberMapper.selectActiveUserIdsByClubId(announcement.getClubId());
		if (userIds == null || userIds.isEmpty()) {
			return;
		}
		String title = systemAdmin ? "系统公告提醒" : "社团公告提醒";
		String content;
		if (systemAdmin) {
			content = announcement.getPublisherName() + "发布了系统公告《"
				+ announcement.getTitle() + "》，请及时查看。";
		} else {
			String clubName = Optional.ofNullable(announcement.getClubName()).orElse("本社团");
			content = announcement.getPublisherName() + "在" + clubName
				+ "发布了公告《" + announcement.getTitle() + "》，请及时查看。";
		}
		for (Long userId : userIds) {
			notificationService.send(
				userId,
				NotificationService.TYPE_ANNOUNCEMENT,
				title,
				content,
				announcement.getId()
			);
		}
	}
}

