package com.kingsejong.global.auth;

import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * 로그인 토큰. API 명세 가정: JWT, 만료 30일, 갱신 없음.
 * 비밀 키는 app.jwt.secret (환경 변수 JWT_SECRET, 32바이트 이상).
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration ttl;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.ttl-days:30}") long ttlDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofDays(ttlDays);
    }

    public String create(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }

    /** 토큰이 맞으면 사용자 id. 틀리거나 만료됐으면 401 UNAUTHORIZED. */
    public Long parseUserId(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject();
            return Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
