package com.nexters.palang.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds,
            @Value("${jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = accessTokenExpirationSeconds * 1000;
        this.refreshTokenExpirationMillis = refreshTokenExpirationSeconds * 1000;
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, accessTokenExpirationMillis);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTokenExpirationMillis);
    }

    public LocalDateTime refreshTokenExpiryFromNow() {
        return LocalDateTime.now().plusSeconds(refreshTokenExpirationMillis / 1000);
    }

    // 만료/서명 오류 시 io.jsonwebtoken의 ExpiredJwtException/JwtException을 그대로 던진다.
    // 호출부(JwtAuthenticationFilter, AuthService)가 각자의 맥락에 맞는 에러 코드로 변환한다.
    public Long getUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    private String createToken(Long userId, long expirationMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }
}
