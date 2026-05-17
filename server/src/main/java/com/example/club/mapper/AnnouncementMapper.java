package com.example.club.mapper;

import com.example.club.domain.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AnnouncementMapper {

	void insert(Announcement announcement);

	Optional<Announcement> selectById(@Param("id") Long id);

	Long countForAudience(@Param("clubIds") List<Long> clubIds);

	List<Announcement> selectForAudience(@Param("clubIds") List<Long> clubIds,
										 @Param("offset") Long offset,
										 @Param("size") Integer size);

	Long countForManage(@Param("publisherId") Long publisherId,
						@Param("scopeType") String scopeType,
						@Param("clubId") Long clubId,
						@Param("systemAdmin") boolean systemAdmin);

	List<Announcement> selectForManage(@Param("publisherId") Long publisherId,
									   @Param("scopeType") String scopeType,
									   @Param("clubId") Long clubId,
									   @Param("systemAdmin") boolean systemAdmin,
									   @Param("offset") Long offset,
									   @Param("size") Integer size);
}

