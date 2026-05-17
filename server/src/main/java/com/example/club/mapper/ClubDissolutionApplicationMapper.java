package com.example.club.mapper;

import com.example.club.domain.ClubDissolutionApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 社团解散申请数据访问层
 */
@Mapper
public interface ClubDissolutionApplicationMapper {

	void insert(ClubDissolutionApplication application);

	Optional<ClubDissolutionApplication> selectById(@Param("id") Long id);

	Long countPendingByClubId(@Param("clubId") Long clubId);

	void updateById(ClubDissolutionApplication application);
}

