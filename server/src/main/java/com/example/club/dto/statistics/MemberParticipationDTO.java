package com.example.club.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 成员参与度统计 DTO
 */
@Data
@Schema(description = "成员参与度统计数据")
public class MemberParticipationDTO {

	@Schema(description = "用户ID")
	private Long userId;

	@Schema(description = "用户名")
	private String username;

	@Schema(description = "真实姓名")
	private String realName;

	@Schema(description = "统计期内报名数量")
	private Long registrationCount;

	@Schema(description = "统计期内签到数量")
	private Long signInCount;

	@Schema(description = "参与率，值范围0-1")
	private Double participationRate;
}
