package com.travel.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.user.dto.UserLoginRequest;
import com.travel.user.dto.UserLoginResponse;
import com.travel.user.dto.UserRegisterRequest;
import com.travel.user.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户ID
     */
    Long register(UserRegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    User getByPhone(String phone);

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getById(Long userId);
}
