package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社团实体类
 * 用于存储社团基本信息，包括名称、描述、联系方式、创始人等数据
 */
@Data
public class Club {
	// 社团ID，主键
	private Long id;
	// 社团名称（唯一）
	private String clubName;
	// 社团代码（唯一）
	private String clubCode;
	// 社团简介
	private String description;
	// 社团宗旨
	private String purpose;
	// 社团章程
	private String constitution;
	// Logo URL
	private String logo;
	// 联系人
	private String contactPerson;
	// 联系电话
	private String contactPhone;
	// 联系邮箱
	private String contactEmail;
	// 创始人ID
	private Long founderId;
	// 社长ID
	private Long presidentId;
	// 成员数量
	private Integer memberCount;
	// 状态（0-待审核，1-已通过，2-已驳回，3-已解散）
	private Integer status;
	// 成立时间
	private LocalDateTime establishedTime;
	// 创建时间
	private LocalDateTime createdTime;
	// 更新时间
	private LocalDateTime updatedTime;
}

