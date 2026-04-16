package com.travel.user.security;

import com.travel.common.enums.ErrorCode;
import com.travel.common.exception.BusinessException;
import com.travel.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 1. 从请求头中获取 Token
            String token = extractTokenFromRequest(request);
            if (StringUtils.hasText(token)) {
                // 2. 验证 Token
                if (jwtUtil.validateToken(token)) {
                    // 3. 解析 Token 获取用户ID
                    Long userId = jwtUtil.getUserIdFromToken(token);

                    // 4. 创建认证信息
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. 设置到 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("Token 验证失败: {}", token);
                }
            }
        } catch (Exception e) {
            log.error("JWT 认证失败: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 继续执行后续过滤器
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取 Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
