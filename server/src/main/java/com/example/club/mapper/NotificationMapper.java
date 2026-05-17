package com.example.club.mapper;

import com.example.club.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper {

	void insert(Notification notification);

	List<Notification> selectByUser(@Param("userId") Long userId,
									@Param("onlyUnread") Boolean onlyUnread,
									@Param("offset") Long offset,
									@Param("size") Integer size);

	Long countByUser(@Param("userId") Long userId,
					 @Param("onlyUnread") Boolean onlyUnread);

	void markRead(@Param("id") Long id,
				  @Param("readTime") LocalDateTime readTime);

	void markAllRead(@Param("userId") Long userId,
					 @Param("readTime") LocalDateTime readTime);
}


