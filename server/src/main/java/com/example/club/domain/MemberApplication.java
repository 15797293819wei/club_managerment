package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入社申请实体类
 * 用于存储学生加入社团的申请信息
 */
@Data
public class MemberApplication {
	// 申请ID，主键
	private Long id;
	// 社团ID
	private Long clubId;
	// 申请人ID
	private Long applicantId;
	// 申请理由
	private String applicationReason;
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

