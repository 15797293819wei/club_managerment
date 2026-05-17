package com.example.club.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClubApplicationHistory {
	private Long id;
	private Long applicationId;
	private Long reviewerId;
	private Integer status;
	private String reviewComment;
	private LocalDateTime createdTime;
}

