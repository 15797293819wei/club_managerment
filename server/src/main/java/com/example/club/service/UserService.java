package com.example.club.service;

import com.example.club.domain.Role;
import com.example.club.domain.User;
import com.example.club.domain.UserRole;
import com.example.club.domain.UserRoleRelation;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.RoleMapper;
import com.example.club.mapper.UserMapper;
import com.example.club.mapper.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户领域服务
 * <p>处理用户的增删改查、权限分配、状态变更等核心业务逻辑。</p>
 */
@Service
public class UserService {

	private static final String DEFAULT_REGISTER_ROLE_CODE = "STUDENT";
	public static final String ROLE_CODE_CLUB_ADMIN = "CLUB_ADMIN";

	private final UserMapper userMapper;
	private final RoleMapper roleMapper;
	private final UserRoleMapper userRoleMapper;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserMapper userMapper,
					   RoleMapper roleMapper,
					   UserRoleMapper userRoleMapper,
					   PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.roleMapper = roleMapper;
		this.userRoleMapper = userRoleMapper;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 分页查询用户列表
	 *
	 * @param keyword 关键字（用户名、姓名、邮箱）
	 * @param status  状态过滤
	 * @param page    页码
	 * @param size    页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<UserSummary> page(String keyword, String username, String studentId, String roleCode, Integer status, int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = userMapper.countByFilters(keyword, username, studentId, roleCode, status);
		if (total == 0) {
			return PageResult.empty();
		}
		List<User> users = userMapper.selectPage(keyword, username, studentId, roleCode, status, offset, size);
		List<Long> userIds = users.stream()
			.map(User::getId)
			.toList();
		Map<Long, List<RoleSimple>> roleMap = loadRoleInfo(userIds);
		List<UserSummary> records = users.stream()
			.map(user -> new UserSummary(
				user.getId(),
				user.getUsername(),
				user.getRealName(),
				user.getStudentId(),
				user.getEmail(),
				user.getPhone(),
				user.getStatus(),
				user.getCreatedTime(),
				user.getUpdatedTime(),
				roleMap.getOrDefault(user.getId(), List.of())
			))
			.toList();
		return new PageResult<>(total, records);
	}

	/**
	 * 查询用户详情
	 *
	 * @param id 用户ID
	 * @return 用户详情
	 */
	@Transactional(readOnly = true)
	public UserDetail detail(Long id) {
		User user = userMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40400, "用户不存在"));
		List<RoleSimple> roles = loadRoleInfo(List.of(user.getId()))
			.getOrDefault(user.getId(), List.of());
		return new UserDetail(
			user.getId(),
			user.getUsername(),
			user.getRealName(),
			user.getStudentId(),
			user.getEmail(),
			user.getPhone(),
			user.getGender(),
			user.getAvatar(),
			user.getStatus(),
			user.getLastLoginTime(),
			user.getAccountLockedUntil(),
			user.getLoginFailureCount(),
			user.getCreatedTime(),
			user.getUpdatedTime(),
			roles
		);
	}

	/**
	 * 创建新用户
	 *
	 * @param command 创建命令
	 * @return 新用户ID
	 */
	@Transactional
	public Long create(UserCreateCommand command) {
		validateUsernameUnique(command.username(), null);
		validateStudentIdUnique(command.studentId(), null);
		List<Role> roles = ensureRoleIdsValid(command.roleIds());

		User user = new User();
		user.setUsername(command.username());
		user.setPassword(passwordEncoder.encode(command.password()));
		user.setRealName(command.realName());
		user.setStudentId(command.studentId());
		user.setEmail(command.email());
		user.setPhone(command.phone());
		user.setGender(command.gender());
		user.setAvatar(command.avatar());
		user.setStatus(Optional.ofNullable(command.status()).orElse(1));
		userMapper.insert(user);

		bindRoles(user.getId(), roles.stream().map(Role::getId).toList());
		return user.getId();
	}

	/**
	 * 自助注册普通学生用户
	 *
	 * @param username  用户名
	 * @param password  密码
	 * @param realName  真实姓名
	 * @param studentId 学号（可选）
	 * @param email     邮箱（可选）
	 * @param phone     手机号（可选）
	 * @return 新用户ID
	 */
	@Transactional
	public Long register(String username, String password, String realName, String studentId, String email, String phone) {
		// 验证密码强度
		com.example.club.util.PasswordValidator.PasswordValidationResult passwordValidation = 
			com.example.club.util.PasswordValidator.validate(password);
		if (!passwordValidation.isValid()) {
			throw new BusinessException(40002, passwordValidation.getMessage());
		}
		
		validateUsernameUnique(username, null);
		validateStudentIdUnique(studentId, null);

		Role studentRole = getActiveRoleOrThrow(DEFAULT_REGISTER_ROLE_CODE, "默认学生角色不存在或已被禁用");

		User user = new User();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(password));
		user.setRealName(realName);
		user.setStudentId(studentId);
		user.setEmail(email);
		user.setPhone(phone);
		user.setStatus(1);
		user.setPasswordChangedTime(LocalDateTime.now()); // 设置密码修改时间

		userMapper.insert(user);
		userMapper.updatePasswordChangedTime(user.getId(), LocalDateTime.now());
		bindRoles(user.getId(), List.of(studentRole.getId()));
		return user.getId();
	}

	/**
	 * 为用户授予指定角色（若未绑定）
	 *
	 * @param userId   用户ID
	 * @param roleCode 角色编码
	 */
	@Transactional
	public void grantRoleIfAbsent(Long userId, String roleCode) {
		// 确保用户存在
		userMapper.selectById(userId)
			.orElseThrow(() -> new BusinessException(40400, "用户不存在"));
		Role role = getActiveRoleOrThrow(roleCode, "角色不存在或已被禁用：" + roleCode);
		Set<Long> currentRoleIds = userRoleMapper.selectRoleIdsByUserId(userId);
		if (!currentRoleIds.contains(role.getId())) {
			UserRole relation = new UserRole();
			relation.setUserId(userId);
			relation.setRoleId(role.getId());
			userRoleMapper.insert(relation);
		}
	}

	/**
	 * 若用户已绑定指定角色，则撤销该角色
	 *
	 * @param userId   用户ID
	 * @param roleCode 角色编码
	 */
	@Transactional
	public void revokeRoleIfPresent(Long userId, String roleCode) {
		var roleOptional = roleMapper.selectByCode(roleCode);
		if (roleOptional.isEmpty()) {
			return;
		}
		Long roleId = roleOptional.get().getId();
		Set<Long> currentRoleIds = userRoleMapper.selectRoleIdsByUserId(userId);
		if (currentRoleIds.contains(roleId)) {
			userRoleMapper.deleteByUserIdAndRoleId(userId, roleId);
		}
	}

	/**
	 * 更新用户信息
	 *
	 * @param id      用户ID
	 * @param command 更新命令
	 */
	@Transactional
	public void update(Long id, UserUpdateCommand command) {
		User existing = userMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40400, "用户不存在"));

		if (StringUtils.hasText(command.username()) && !Objects.equals(command.username(), existing.getUsername())) {
			validateUsernameUnique(command.username(), id);
		}
		if (StringUtils.hasText(command.studentId()) && !Objects.equals(command.studentId(), existing.getStudentId())) {
			validateStudentIdUnique(command.studentId(), id);
		}

		User user = new User();
		user.setId(id);
		if (StringUtils.hasText(command.username())) {
			user.setUsername(command.username());
		}
		if (StringUtils.hasText(command.password())) {
			// 验证密码强度
			com.example.club.util.PasswordValidator.PasswordValidationResult passwordValidation = 
				com.example.club.util.PasswordValidator.validate(command.password());
			if (!passwordValidation.isValid()) {
				throw new BusinessException(40002, passwordValidation.getMessage());
			}
			user.setPassword(passwordEncoder.encode(command.password()));
			user.setPasswordChangedTime(LocalDateTime.now());
		}
		if (StringUtils.hasText(command.realName())) {
			user.setRealName(command.realName());
		}
		user.setStudentId(command.studentId());
		user.setEmail(command.email());
		user.setPhone(command.phone());
		user.setGender(command.gender());
		user.setAvatar(command.avatar());
		user.setStatus(command.status());

		userMapper.updateById(user);
		
		// 如果更新了密码，更新密码修改时间
		if (StringUtils.hasText(command.password())) {
			userMapper.updatePasswordChangedTime(id, LocalDateTime.now());
		}

		if (command.roleIds() != null) {
			List<Role> roles = ensureRoleIdsValid(command.roleIds());
			assignRolesInternal(id, roles.stream().map(Role::getId).toList());
		}
	}

	/**
	 * 修改用户状态（启用/禁用）
	 *
	 * @param id     用户ID
	 * @param status 状态值
	 */
	@Transactional
	public void changeStatus(Long id, Integer status) {
		userMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40400, "用户不存在"));
		if (status == null || (status != 0 && status != 1)) {
			throw new BusinessException(40010, "状态值非法");
		}
		userMapper.updateStatus(id, status);
	}

	/**
	 * 批量修改用户状态
	 *
	 * @param ids    用户ID集合
	 * @param status 状态值
	 */
	@Transactional
	public void batchChangeStatus(List<Long> ids, Integer status) {
		if (CollectionUtils.isEmpty(ids)) {
			throw new BusinessException(40011, "请选择需要操作的用户");
		}
		if (status == null || (status != 0 && status != 1)) {
			throw new BusinessException(40010, "状态值非法");
		}
		userMapper.batchUpdateStatus(ids, status);
	}

	/**
	 * 系统管理员重置用户密码
	 *
	 * @param id          用户ID
	 * @param newPassword 新密码
	 */
	@Transactional
	public void resetPasswordByAdmin(Long id, String newPassword) {
		User user = userMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40400, "用户不存在"));
		var validation = com.example.club.util.PasswordValidator.validate(newPassword);
		if (!validation.isValid()) {
			throw new BusinessException(40012, validation.getMessage());
		}
		String encoded = passwordEncoder.encode(newPassword);
		User update = new User();
		update.setId(user.getId());
		update.setPassword(encoded);
		update.setPasswordChangedTime(LocalDateTime.now());
		userMapper.updateById(update);
		userMapper.updatePasswordChangedTime(id, LocalDateTime.now());
		// 重置失败次数与锁定信息
		userMapper.resetLoginFailureCount(id);
		// 清理密码重置令牌
		userMapper.clearPasswordResetToken(id);
	}

	/**
	 * 为用户分配角色集合
	 *
	 * @param id      用户ID
	 * @param roleIds 角色ID集合
	 */
	@Transactional
	public void assignRoles(Long id, List<Long> roleIds) {
		userMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40400, "用户不存在"));
		List<Role> roles = ensureRoleIdsValid(roleIds);
		assignRolesInternal(id, roles.stream().map(Role::getId).toList());
	}

	/**
	 * 删除用户及其角色关联
	 *
	 * @param id 用户ID
	 */
	@Transactional
	public void delete(Long id) {
		userMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40400, "用户不存在"));
		userRoleMapper.deleteByUserId(id);
		userMapper.deleteById(id);
	}

	/**
	 * 重新绑定用户角色（先删后插）
	 *
	 * @param userId  用户ID
	 * @param roleIds 角色集合
	 */
	private void assignRolesInternal(Long userId, List<Long> roleIds) {
		userRoleMapper.deleteByUserId(userId);
		if (!CollectionUtils.isEmpty(roleIds)) {
			userRoleMapper.insertBatch(userId, roleIds);
		}
	}

	/**
	 * 为新用户绑定角色
	 *
	 * @param userId  用户ID
	 * @param roleIds 角色集合
	 */
	private void bindRoles(Long userId, List<Long> roleIds) {
		if (CollectionUtils.isEmpty(roleIds)) {
			return;
		}
		userRoleMapper.insertBatch(userId, roleIds);
	}

	/**
	 * 校验用户名唯一性
	 *
	 * @param username 用户名
	 * @param excludeId 排除的用户ID
	 */
	private void validateUsernameUnique(String username, Long excludeId) {
		if (!StringUtils.hasText(username)) {
			throw new BusinessException(40011, "用户名不能为空");
		}
		userMapper.selectByUsername(username)
			.filter(user -> !Objects.equals(user.getId(), excludeId))
			.ifPresent(user -> {
				throw new BusinessException(40012, "用户名已存在");
			});
	}

	/**
	 * 校验学号唯一性
	 *
	 * @param studentId 学号
	 * @param excludeId 排除的用户ID
	 */
	private void validateStudentIdUnique(String studentId, Long excludeId) {
		if (!StringUtils.hasText(studentId)) {
			return;
		}
		userMapper.selectByStudentId(studentId)
			.filter(user -> !Objects.equals(user.getId(), excludeId))
			.ifPresent(user -> {
				throw new BusinessException(40013, "学号已存在");
			});
	}

	/**
	 * 校验角色集合有效性（存在且启用）
	 *
	 * @param roleIds 角色ID集合
	 * @return 角色实体列表
	 */
	private List<Role> ensureRoleIdsValid(List<Long> roleIds) {
		if (CollectionUtils.isEmpty(roleIds)) {
			return List.of();
		}
		List<Long> distinctIds = roleIds.stream().distinct().toList();
		List<Role> roles = roleMapper.selectByIds(distinctIds);
		if (CollectionUtils.isEmpty(roles) || roles.size() != distinctIds.size()) {
			throw new BusinessException(40014, "存在无效的角色ID");
		}
		boolean hasInactive = roles.stream().anyMatch(role -> role.getStatus() != null && role.getStatus() == 0);
		if (hasInactive) {
			throw new BusinessException(40015, "存在已禁用的角色，无法分配");
		}
		return roles;
	}

	/**
	 * 批量加载用户-角色信息
	 *
	 * @param userIds 用户ID集合
	 * @return 用户ID -> 角色集合映射
	 */
	private Map<Long, List<RoleSimple>> loadRoleInfo(List<Long> userIds) {
		if (CollectionUtils.isEmpty(userIds)) {
			return Map.of();
		}
		List<UserRoleRelation> relations = userRoleMapper.selectRoleInfoByUserIds(userIds);
		if (CollectionUtils.isEmpty(relations)) {
			return Map.of();
		}
		return relations.stream()
			.collect(Collectors.groupingBy(
				UserRoleRelation::getUserId,
				Collectors.collectingAndThen(
					Collectors.mapping(this::toRoleSimple, Collectors.toCollection(ArrayList::new)),
					list -> {
						list.sort(Comparator.comparing(RoleSimple::roleCode));
						return list;
					}
				)
			));
	}

	/**
	 * 转换用户角色关系为简要信息
	 *
	 * @param relation 用户角色关系
	 * @return 角色简要信息
	 */
	private RoleSimple toRoleSimple(UserRoleRelation relation) {
		return new RoleSimple(relation.getRoleId(), relation.getRoleCode(), relation.getRoleName());
	}

	private Role getActiveRoleOrThrow(String roleCode, String message) {
		return roleMapper.selectByCode(roleCode)
			.filter(role -> role.getStatus() == null || role.getStatus() == 1)
			.orElseThrow(() -> new BusinessException(50020, message));
	}

	public record UserCreateCommand(String username, String password, String realName, String studentId,
									String email, String phone, Integer gender, String avatar, Integer status,
									List<Long> roleIds) {
	}

	public record UserUpdateCommand(String username, String password, String realName, String studentId,
									String email, String phone, Integer gender, String avatar, Integer status,
									List<Long> roleIds) {
	}

	public record UserSummary(Long id, String username, String realName, String studentId, String email, String phone,
							  Integer status, LocalDateTime createdTime, LocalDateTime updatedTime,
							  List<RoleSimple> roles) {
	}

	public record UserDetail(Long id, String username, String realName, String studentId, String email, String phone,
							 Integer gender, String avatar, Integer status, LocalDateTime lastLoginTime,
							 LocalDateTime accountLockedUntil, Integer loginFailureCount,
							 LocalDateTime createdTime, LocalDateTime updatedTime, List<RoleSimple> roles) {
	}

	public record RoleSimple(Long id, String roleCode, String roleName) {
	}
}


