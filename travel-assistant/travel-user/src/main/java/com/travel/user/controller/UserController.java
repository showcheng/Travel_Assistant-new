package com.travel.user.controller;

import com.travel.common.response.Result;
import com.travel.user.dto.UserLoginRequest;
import com.travel.user.dto.UserLoginResponse;
import com.travel.user.dto.UserRegisterRequest;
import com.travel.user.entity.User;
import com.travel.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "用户接口", description = "用户注册、登录、信息查询")
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/register")
    @Operation(summary = "用户注册")
    public Result<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("用户注册: phone={}", request.getPhone());
        Long userId = userService.register(request);
        return Result.success(userId);
    }

    @PostMapping("/auth/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        log.info("用户登录: phone={}", request.getPhone());
        UserLoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @PostMapping("/auth/sms/send")
    @Operation(summary = "发送短信验证码")
    public Result<Void> sendSmsCode(@RequestParam String phone) {
        log.info("发送短信验证码: phone={}", phone);
        // TODO: 调用短信服务发送验证码
        // 临时：开发环境默认验证码为123456
        return Result.success();
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        log.info("用户退出登录");
        // TODO: 清除Redis中的token缓存
        return Result.success();
    }

    @GetMapping("/user/info")
    @Operation(summary = "获取当前用户信息")
    public Result<User> getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            Long userId = (Long) authentication.getPrincipal();
            User user = userService.getById(userId);
            // 清除敏感信息
            user.setPasswordHash(null);
            return Result.success(user);
        }
        return Result.error("未登录");
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询用户信息")
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 清除敏感信息
        user.setPasswordHash(null);
        return Result.success(user);
    }
}
