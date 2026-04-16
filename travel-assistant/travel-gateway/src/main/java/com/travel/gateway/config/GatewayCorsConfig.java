package com.travel.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * 网关CORS跨域配置
 */
@Configuration
public class GatewayCorsConfig {

    /**
     * 配置CORS跨域过滤器
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许所有来源（开发环境）
        configuration.setAllowedOrigins(Arrays.asList("*"));

        // 允许所有请求头
        configuration.setAllowedHeaders(Collections.singletonList("*"));

        // 允许所有HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS",
            "HEAD",
            "PATCH"
        ));

        // 允许携带凭证
        configuration.setAllowCredentials(true);

        // 预检请求的有效期，单位为秒
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }
}