package com.example.club.mapper;

import com.example.club.domain.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问层接口
 * <p>负责角色信息的增删改查、分页统计以及角色集合查询。</p>
 */
@Mapper
public interface RoleMapper {
	/**
	 * 根据主键查询角色
	 *
	 * @param id 角色ID
	 * @return 角色信息，可为空
	 */
	Optional<Role> selectById(@Param("id") Long id);

	/**
	 * 根据角色编码查询角色
	 *
	 * @param roleCode 角色编码
	 * @return 角色信息，可为空
	 */
	Optional<Role> selectByCode(@Param("roleCode") String roleCode);

	/**
	 * 分页查询角色列表
	 *
	 * @param keyword 关键字（编码/名称）
	 * @param status  状态过滤
	 * @param offset  偏移量
	 * @param size    每页条数
	 * @return 角色集合
	 */
	List<Role> selectPage(@Param("keyword") String keyword,
		@Param("status") Integer status,
		@Param("offset") long offset,
		@Param("size") int size);

	/**
	 * 根据筛选条件统计角色数量
	 *
	 * @param keyword 关键字
	 * @param status  状态
	 * @return 总条数
	 */
	long countByFilters(@Param("keyword") String keyword, @Param("status") Integer status);

	/**
	 * 新增角色
	 *
	 * @param role 角色实体
	 * @return 影响行数
	 */
	int insert(Role role);

	/**
	 * 根据ID更新角色
	 *
	 * @param role 角色实体
	 * @return 影响行数
	 */
	int updateById(Role role);

	/**
	 * 根据ID删除角色
	 *
	 * @param id 角色ID
	 * @return 影响行数
	 */
	int deleteById(@Param("id") Long id);

	/**
	 * 根据ID集合批量查询角色
	 *
	 * @param ids 角色ID集合
	 * @return 角色列表
	 */
	List<Role> selectByIds(@Param("ids") List<Long> ids);

	/**
	 * 查询所有启用状态的角色
	 *
	 * @return 启用角色列表
	 */
	List<Role> selectAllActive();
}


