package com.example.club.service;

import com.example.club.domain.Role;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.RoleMapper;
import com.example.club.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 角色领域服务
 * <p>封装角色的业务逻辑，包括分页查询、创建更新、状态校验等。</p>
 */
@Service
public class RoleService {

	private final RoleMapper roleMapper;
	private final UserRoleMapper userRoleMapper;

	public RoleService(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
		this.roleMapper = roleMapper;
		this.userRoleMapper = userRoleMapper;
	}

	/**
	 * 分页查询角色列表
	 *
	 * @param keyword 关键词
	 * @param status  状态过滤
	 * @param page    页码
	 * @param size    页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<RoleInfo> page(String keyword, Integer status, int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = roleMapper.countByFilters(keyword, status);
		if (total == 0) {
			return PageResult.empty();
		}
		List<Role> roles = roleMapper.selectPage(keyword, status, offset, size);
		List<RoleInfo> records = roles.stream()
			.map(this::toRoleInfo)
			.toList();
		return new PageResult<>(total, records);
	}

	/**
	 * 查询角色详情
	 *
	 * @param id 角色ID
	 * @return 角色信息
	 */
	@Transactional(readOnly = true)
	public RoleInfo getDetail(Long id) {
		Role role = roleMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40401, "角色不存在"));
		return toRoleInfo(role);
	}

	/**
	 * 新增角色
	 *
	 * @param command 创建命令
	 * @return 新角色ID
	 */
	@Transactional
	public Long create(RoleCommand command) {
		validateRoleCodeUnique(command.roleCode(), null);
		Role role = new Role();
		role.setRoleCode(command.roleCode());
		role.setRoleName(command.roleName());
		role.setDescription(command.description());
		role.setStatus(normalizeStatus(command.status()));
		roleMapper.insert(role);
		return role.getId();
	}

	/**
	 * 更新角色信息
	 *
	 * @param id      角色ID
	 * @param command 更新命令
	 */
	@Transactional
	public void update(Long id, RoleCommand command) {
		Role existing = roleMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40401, "角色不存在"));
		if (StringUtils.hasText(command.roleCode()) && !Objects.equals(command.roleCode(), existing.getRoleCode())) {
			validateRoleCodeUnique(command.roleCode(), id);
		}
		Role role = new Role();
		role.setId(id);
		if (StringUtils.hasText(command.roleCode())) {
			role.setRoleCode(command.roleCode());
		}
		if (StringUtils.hasText(command.roleName())) {
			role.setRoleName(command.roleName());
		}
		role.setDescription(command.description());
		if (command.status() != null) {
			role.setStatus(normalizeStatus(command.status()));
		}
		roleMapper.updateById(role);
	}

	/**
	 * 删除角色
	 *
	 * @param id 角色ID
	 */
	@Transactional
	public void delete(Long id) {
		roleMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40401, "角色不存在"));
		long bindCount = userRoleMapper.countByRoleId(id);
		if (bindCount > 0) {
			throw new BusinessException(40001, "角色已分配给用户，无法删除");
		}
		roleMapper.deleteById(id);
	}

	/**
	 * 查询全部启用状态角色
	 *
	 * @return 启用角色列表
	 */
	@Transactional(readOnly = true)
	public List<RoleSimple> listAllActive() {
		List<Role> roles = roleMapper.selectAllActive();
		if (CollectionUtils.isEmpty(roles)) {
			return List.of();
		}
		return roles.stream()
			.map(role -> new RoleSimple(role.getId(), role.getRoleCode(), role.getRoleName()))
			.toList();
	}

	private void validateRoleCodeUnique(String roleCode, Long excludeId) {
		if (!StringUtils.hasText(roleCode)) {
			throw new BusinessException(40000, "角色编码不能为空");
		}
		var optional = roleMapper.selectByCode(roleCode);
		if (optional.isPresent() && !Objects.equals(optional.get().getId(), excludeId)) {
			throw new BusinessException(40002, "角色编码已存在");
		}
	}

	private RoleInfo toRoleInfo(Role role) {
		return new RoleInfo(
			role.getId(),
			role.getRoleCode(),
			role.getRoleName(),
			role.getDescription(),
			role.getStatus(),
			role.getCreatedTime(),
			role.getUpdatedTime()
		);
	}

	public record RoleCommand(String roleCode, String roleName, String description, Integer status) {
	}

	public record RoleInfo(Long id, String roleCode, String roleName, String description, Integer status,
						   LocalDateTime createdTime, LocalDateTime updatedTime) {
	}

	public record RoleSimple(Long id, String roleCode, String roleName) {
	}

	private Integer normalizeStatus(Integer status) {
		if (status == null) {
			return 1;
		}
		if (status == 0 || status == 1) {
			return status;
		}
		throw new BusinessException(40003, "角色状态取值非法");
	}
}


