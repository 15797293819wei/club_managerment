package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社团解散申请实体类
 * 用于存储社团解散申请信息，包括申请人、原因、审核状态等数据
 */
@Data
public class ClubDissolutionApplication {
	// 申请ID，主键
	private Long id;
	// 社团ID
	private Long clubId;
	// 申请人ID（一般为社长或社团管理员）
	private Long applicantId;
	// 解散原因
	private String reason;
	// 状态（0-待审核，1-已通过，2-已驳回）
	private Integer status;
	// 审核人ID
	private Long reviewerId;
	// 审核时间
	private LocalDateTime reviewTime;
	// 审核意见
	private String reviewComment;
	// 创建时间
	private LocalDateTime createdTime;
	// 更新时间
	private LocalDateTime updatedTime;
}

