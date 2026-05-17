package com.example.club.mapper;

import com.example.club.domain.AttendanceRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface AttendanceRuleMapper {

	Optional<AttendanceRule> selectByClubId(@Param("clubId") Long clubId);

	void insert(AttendanceRule rule);

	void update(AttendanceRule rule);
}


