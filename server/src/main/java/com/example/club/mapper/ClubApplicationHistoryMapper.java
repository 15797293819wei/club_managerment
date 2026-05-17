package com.example.club.mapper;

import com.example.club.domain.ClubApplicationHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClubApplicationHistoryMapper {

	void insert(ClubApplicationHistory history);

	List<ClubApplicationHistory> selectByApplicationId(@Param("applicationId") Long applicationId);
}

