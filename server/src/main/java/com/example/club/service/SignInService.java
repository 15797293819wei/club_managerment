package com.example.club.service;

import com.example.club.domain.Activity;
import com.example.club.domain.Registration;
import com.example.club.domain.SignIn;
import com.example.club.dto.PageResult;
import com.example.club.exception.BusinessException;
import com.example.club.mapper.ActivityMapper;
import com.example.club.mapper.MemberMapper;
import com.example.club.mapper.RegistrationMapper;
import com.example.club.mapper.SignInMapper;
import com.example.club.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 签到领域服务
 * <p>处理活动签到的增删改查等核心业务逻辑。</p>
 */
@Service
public class SignInService {

	private final SignInMapper signInMapper;
	private final ActivityMapper activityMapper;
	private final RegistrationMapper registrationMapper;
	private final MemberMapper memberMapper;
	private final UserMapper userMapper;
	private final AttendanceService attendanceService;

	public SignInService(SignInMapper signInMapper,
						 ActivityMapper activityMapper,
						 RegistrationMapper registrationMapper,
						 MemberMapper memberMapper,
						 UserMapper userMapper,
						 AttendanceService attendanceService) {
		this.signInMapper = signInMapper;
		this.activityMapper = activityMapper;
		this.registrationMapper = registrationMapper;
		this.memberMapper = memberMapper;
		this.userMapper = userMapper;
		this.attendanceService = attendanceService;
	}

	/**
	 * 分页查询签到列表
	 *
	 * @param activityId 活动ID（可选）
	 * @param userId 用户ID（可选）
	 * @param page   页码
	 * @param size   页大小
	 * @return 分页数据
	 */
	@Transactional(readOnly = true)
	public PageResult<SignInInfo> page(Long activityId, Long userId, int page, int size) {
		long offset = (long) (page - 1) * size;
		long total = signInMapper.countByFilters(activityId, userId);
		if (total == 0) {
			return PageResult.empty();
		}
		List<SignIn> signIns = signInMapper.selectPage(activityId, userId, offset, size);
		// 批量查询用户名
		List<Long> userIds = signIns.stream().map(SignIn::getUserId).distinct().collect(Collectors.toList());
		Map<Long, String> usernames = userIds.isEmpty() ? Map.of() :
			userMapper.selectUsernamesByIds(userIds).stream()
				.collect(Collectors.toMap(
					m -> ((Number) m.get("id")).longValue(),
					m -> (String) m.get("username"),
					(v1, v2) -> v1
				));
		List<SignInInfo> records = signIns.stream()
			.map(s -> toSignInInfo(s, usernames.get(s.getUserId())))
			.toList();
		return new PageResult<>(total, records);
	}

	/**
	 * 查询签到详情
	 *
	 * @param id 签到ID
	 * @return 签到详情
	 */
	@Transactional(readOnly = true)
	public SignInInfo getDetail(Long id) {
		SignIn signIn = signInMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40407, "签到不存在"));
		List<Map<String, Object>> userMaps = userMapper.selectUsernamesByIds(List.of(signIn.getUserId()));
		String username = userMaps.isEmpty() ? "未知用户" :
			userMaps.stream()
				.filter(m -> ((Number) m.get("id")).longValue() == signIn.getUserId())
				.findFirst()
				.map(m -> (String) m.get("username"))
				.orElse("未知用户");
		return toSignInInfo(signIn, username);
	}

	/**
	 * 签到打卡
	 *
	 * @param activityId 活动ID
	 * @param userId     用户ID
	 * @param signInType 签到方式（1-手动签到，2-二维码签到）
	 * @param location   签到地点（可选，用于二维码签到）
	 * @param remark     备注（可选）
	 * @return 新签到ID
	 */
	@Transactional
	public Long signIn(Long activityId, Long userId, Integer signInType, String location, String remark) {
		// 校验活动是否存在
		Activity activity = activityMapper.selectById(activityId)
			.orElseThrow(() -> new BusinessException(40405, "活动不存在"));

		// 校验活动状态（只有进行中的活动可以签到）
		if (activity.getStatus() != 1) {
			throw new BusinessException(40045, "只有进行中的活动可以签到");
		}

		// 校验是否已报名
		Optional<Registration> registration = registrationMapper.selectByActivityIdAndUserId(activityId, userId);
		if (registration.isEmpty() || registration.get().getStatus() != 1) {
			throw new BusinessException(40046, "只有已报名的用户才能签到");
		}

		// 检查是否已经签到
		Optional<SignIn> existing = signInMapper.selectByActivityIdAndUserId(activityId, userId);
		if (existing.isPresent()) {
			throw new BusinessException(40047, "您已经签到该活动");
		}

		// 校验签到方式
		if (signInType == null || (signInType != 1 && signInType != 2)) {
			throw new BusinessException(40048, "签到方式值非法，只能为1（手动签到）或2（二维码签到）");
		}

		// 创建新签到
		SignIn signIn = new SignIn();
		signIn.setActivityId(activityId);
		signIn.setUserId(userId);
		signIn.setSignInTime(LocalDateTime.now());
		signIn.setSignInType(signInType);
		signIn.setLocation(location);
		signIn.setRemark(remark);
		signInMapper.insert(signIn);

		// 自动记录考勤
		memberMapper.selectByClubIdAndUserId(activity.getClubId(), userId)
			.ifPresent(member -> attendanceService.recordActivityAttendance(
				activity.getClubId(),
				activity.getId(),
				member.getId(),
				signIn.getSignInTime(),
				remark
			));

		return signIn.getId();
	}

	/**
	 * 删除签到
	 *
	 * @param id 签到ID
	 */
	@Transactional
	public void delete(Long id) {
		signInMapper.selectById(id)
			.orElseThrow(() -> new BusinessException(40407, "签到不存在"));
		signInMapper.deleteById(id);
	}

	private SignInInfo toSignInInfo(SignIn signIn, String username) {
		return new SignInInfo(
			signIn.getId(),
			signIn.getActivityId(),
			signIn.getUserId(),
			username,
			signIn.getSignInTime(),
			signIn.getSignInType(),
			signIn.getLocation(),
			signIn.getRemark(),
			signIn.getCreatedTime()
		);
	}

	public record SignInInfo(
		Long id,
		Long activityId,
		Long userId,
		String username,
		LocalDateTime signInTime,
		Integer signInType,
		String location,
		String remark,
		LocalDateTime createdTime
	) {
	}
}

