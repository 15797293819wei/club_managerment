package com.example.club.service;

import com.example.club.domain.Notification;
import com.example.club.dto.PageResult;
import com.example.club.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内消息通知服务
 */
@Service
public class NotificationService {

	public static final String TYPE_MEMBER_APPLICATION_RESULT = "MEMBER_APPLICATION_RESULT";
	public static final String TYPE_CLUB_APPLICATION_RESULT = "CLUB_APPLICATION_RESULT";
	public static final String TYPE_CLUB_DISSOLUTION_APPLICATION = "CLUB_DISSOLUTION_APPLICATION";
	public static final String TYPE_ACTIVITY_REGISTRATION = "ACTIVITY_REGISTRATION";
	public static final String TYPE_ACTIVITY_REMINDER = "ACTIVITY_REMINDER";
	public static final String TYPE_EVALUATION_NOTICE = "EVALUATION_NOTICE";
	public static final String TYPE_ROLE_CHANGED = "ROLE_CHANGED";
	public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
	public static final String TYPE_MEMBER_QUIT = "MEMBER_QUIT";

	private final NotificationMapper notificationMapper;

	public NotificationService(NotificationMapper notificationMapper) {
		this.notificationMapper = notificationMapper;
	}

	@Transactional
	public void send(Long userId, String type, String title, String content, Long relatedId) {
		Notification n = new Notification();
		n.setUserId(userId);
		n.setType(type);
		n.setTitle(title);
		n.setContent(content);
		n.setRelatedId(relatedId);
		n.setReadFlag(0);
		notificationMapper.insert(n);
	}

	@Transactional(readOnly = true)
	public PageResult<Notification> page(Long userId, boolean onlyUnread, int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = notificationMapper.countByUser(userId, onlyUnread);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Notification> list = notificationMapper.selectByUser(userId, onlyUnread, offset, size);
		return new PageResult<>(total, list);
	}

	@Transactional
	public void markRead(Long id) {
		notificationMapper.markRead(id, LocalDateTime.now());
	}

	@Transactional
	public void markAllRead(Long userId) {
		notificationMapper.markAllRead(userId, LocalDateTime.now());
	}

	@Transactional(readOnly = true)
	public long countUnread(Long userId) {
		return notificationMapper.countByUser(userId, true);
	}
}


