package com.example.club.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 社团活跃度趋势数据点 DTO
 */
@Data
@Schema(description = "社团活跃度趋势数据点")
public class ClubActivityTrendPointDTO {

	@Schema(description = "时间周期标识，例如2024-01或2024-01-15")
	private String period;

	@Schema(description = "该周期内活动数量")
	private Long activityCount;

	@Schema(description = "该周期内报名总数")
	private Long registrationCount;

	@Schema(description = "该周期内签到总数")
	private Long signInCount;
}
