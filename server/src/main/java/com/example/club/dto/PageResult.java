package com.example.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页结果封装
 *
 * @param <T> 数据记录类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
	/**
	 * 数据总条数
	 */
	private long total;
	/**
	 * 当前页数据记录
	 */
	private List<T> records;

	/**
	 * 构建空的分页结果
	 * @param <T> 记录类型
	 * @return 空分页对象
	 */
	public static <T> PageResult<T> empty() {
		return new PageResult<>(0, Collections.emptyList());
	}
}


