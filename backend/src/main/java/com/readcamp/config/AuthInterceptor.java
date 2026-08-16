package com.readcamp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readcamp.common.JwtUtil;
import com.readcamp.common.Result;
import com.readcamp.common.UserContext;
import com.readcamp.entity.User;
import com.readcamp.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 认证拦截器：解析 Authorization: Bearer <token>，校验用户状态后写入 UserContext
 * 失败直接写 401 JSON（不抛异常，避免依赖异常解析链路）
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(response, "未登录");
        }
        String token = header.substring(7).trim();

        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(response, "登录已过期，请重新登录");
        }

        Long userId = Long.valueOf(claims.getSubject());
        User user = userMapper.selectById(userId);
        if (user == null) {
            return unauthorized(response, "用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return unauthorized(response, "账号已被禁用");
        }
        // 用库里最新信息（角色变更即时生效），不回填密码
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(40100, message)));
        return false;
    }
}
