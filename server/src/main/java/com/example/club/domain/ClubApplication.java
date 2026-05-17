package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社团申请实体类
 * 用于存储社团创建申请信息，包括申请内容、审核状态等数据
 */
@Data
public class ClubApplication {
	// 申请ID，主键
	private Long id;
	// 申请人ID
	private Long applicantId;
	// 社团名称
	private String clubName;
	// 社团代码
	private String clubCode;
	// 社团简介
	private String description;
	// 社团宗旨
	private String purpose;
	// 社团章程
	private String constitution;
	// Logo URL
	private String logo;
	// 证明材料URL
	private String attachment;
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

