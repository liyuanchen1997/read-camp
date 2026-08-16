package com.readcamp.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具（jjwt 0.12 API，HS256）
 * claims: sub=userId, username, role
 */
@Component
public class JwtUtil {

    @Value("${readcamp.jwt.secret:}")
    private String secret;

    @Value("${readcamp.jwt.expire-days:7}")
    private long expireDays;

    private SecretKey key;

    @PostConstruct
    void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("readcamp.jwt.secret 未配置或长度不足 32 字符（JWT 安全要求）");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username, Integer role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofDays(expireDays))))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 token；无效/过期抛 JwtException
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
