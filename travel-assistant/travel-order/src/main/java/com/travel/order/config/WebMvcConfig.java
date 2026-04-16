package com.travel.order.config;

import com.travel.common.interceptor.UserAuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserAuthenticationInterceptor userAuthenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userAuthenticationInterceptor)
                .addPathPatterns("/api/**") // 拦截所有API请求
                .excludePathPatterns(
                        "/api/auth/**",      // 排除认证接口
                        "/api/products/**",  // 排除商品查询接口
                        "/swagger-ui/**",    // 排除Swagger UI
                        "/api-docs/**",      // 排除API文档
                        "/actuator/**"       // 排除监控端点
                );
    }
}