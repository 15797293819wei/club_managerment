package com.example.club.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 社团活跃度汇总数据 DTO
 */
@Data
@Schema(description = "社团活跃度汇总数据")
public class ClubActivitySummaryDTO {

	@Schema(description = "社团ID")
	private Long clubId;

	@Schema(description = "社团名称")
	private String clubName;

	@Schema(description = "统计期内活动数量")
	private Long activityCount;

	@Schema(description = "统计期内报名总数")
	private Long registrationCount;

	@Schema(description = "统计期内签到总数")
	private Long signInCount;

	@Schema(description = "活动记录的总参与人次")
	private Long totalParticipants;
}
