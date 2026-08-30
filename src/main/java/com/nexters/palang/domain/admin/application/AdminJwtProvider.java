package com.nexters.palang.domain.admin.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 관리자 페이지 전용 세션 토큰. 일반 유저 로그인(JwtTokenProvider)과 완전히 분리된 별도 비밀키로
// 서명한다 — 두 토큰 체계를 같은 키로 섞으면 유저 토큰으로 관리자 토큰을 위조하거나 그 반대가 가능해질
// 수 있어서다. admin.jwt-secret이 설정되지 않으면(로컬 개발 편의를 위해 앱을 죽이는 대신) 기동마다
// 바뀌는 임시 키를 생성해 사용한다 — 이 경우 아무도 유효한 토큰을 만들 수 없으므로 관리자 로그인은
// 사실상 비활성 상태가 된다(admin.login-username/password 미설정과 동일하게 "안전하게 꺼진" 상태).
@Slf4j
@Component
public class AdminJwtProvider {

    private static final String ADMIN_SUBJECT = "admin";

    private final SecretKey secretKey;
    private final long expirationMillis;

    public AdminJwtProvider(
            @Value("${admin.jwt-secret:}") String secret,
            @Value("${admin.session-expiration-seconds:43200}") long expirationSeconds) {
        this.secretKey = secret.isBlank() ? randomKey() : Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationSeconds * 1000;
        if (secret.isBlank()) {
            log.warn("admin.jwt-secret가 설정되지 않아 기동마다 바뀌는 임시 키를 사용합니다. "
                    + "관리자 로그인을 쓰려면 ADMIN_JWT_SECRET을 설정하세요.");
        }
    }

    public String createToken() {
        Date now = new Date();
        return Jwts.builder()
                .subject(ADMIN_SUBJECT)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            return ADMIN_SUBJECT.equals(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private static SecretKey randomKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Keys.hmacShaKeyFor(bytes);
    }
}
