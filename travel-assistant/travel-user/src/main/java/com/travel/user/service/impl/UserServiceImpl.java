package com.travel.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.common.enums.ErrorCode;
import com.travel.common.exception.BusinessException;
import com.travel.common.utils.JwtUtil;
import com.travel.user.dto.UserLoginRequest;
import com.travel.user.dto.UserLoginResponse;
import com.travel.user.dto.UserRegisterRequest;
import com.travel.user.entity.User;
import com.travel.user.mapper.UserMapper;
import com.travel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration:604800000}")
    private Long jwtExpiration;

    @Override
    public Long register(UserRegisterRequest request) {
        // 1. 验证验证码（TODO: 接入短信服务）
        validateSmsCode(request.getPhone(), request.getSmsCode());

        // 2. 检查手机号是否已注册
        User existUser = this.getByPhone(request.getPhone());
        if (existUser != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 3. 创建用户
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + request.getPhone().substring(7));
        user.setRegisterSource("WEB");
        user.setStatus(0);

        // 4. 保存到数据库
        this.save(user);

        log.info("用户注册成功: userId={}, phone={}", user.getId(), user.getPhone());

        return user.getId();
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        // 1. 查询用户
        User user = this.getByPhone(request.getPhone());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        // 3. 检查账号状态
        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.USER_ACCOUNT_LOCKED);
        }

        // 4. 生成 Token
        String accessToken = jwtUtil.generateToken(user.getId());
        String refreshToken = jwtUtil.generateToken(user.getId());

        // 5. 构建响应
        return UserLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .expiresIn(jwtExpiration / 1000)
                .build();
    }

    @Override
    public User getByPhone(String phone) {
        return this.lambdaQuery()
                .eq(User::getPhone, phone)
                .one();
    }

    @Override
    public User getById(Long userId) {
        return super.getById(userId);
    }

    /**
     * 验证短信验证码
     */
    private void validateSmsCode(String phone, String smsCode) {
        // TODO: 从 Redis 获取验证码并验证
        // String cachedCode = redisUtil.get("sms:code:" + phone);
        // if (cachedCode == null || !cachedCode.equals(smsCode)) {
        //     throw new BusinessException(ErrorCode.USER_SMS_CODE_ERROR);
        // }

        // 临时：开发环境下默认验证码为 "123456"
        if (!"123456".equals(smsCode)) {
            throw new BusinessException(ErrorCode.USER_SMS_CODE_ERROR);
        }
    }
}
