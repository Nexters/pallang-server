package com.nexters.palang.domain.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.auth.infrastructure.dto.ApplePublicKeysResponse;
import com.nexters.palang.global.security.AuthErrorCode;
import com.nexters.palang.global.security.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

// 카카오와 달리 Apple에는 "액세스 토큰으로 사용자 정보 조회" REST API가 없어, 클라이언트가 전달한
// identity token(JWT)의 서명을 Apple 공개키(JWKS)로 직접 검증해야 한다. 웹(Service ID)과 iOS 앱
// (Bundle ID)의 aud 값이 서로 다르므로 둘 다 허용한다.
@Component
public class AppleOAuthClient {

    private static final String ISSUER = "https://appleid.apple.com";
    private static final Duration JWKS_CACHE_TTL = Duration.ofHours(1);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient appleWebClient;
    private final List<String> allowedAudiences;

    private volatile List<ApplePublicKeysResponse.ApplePublicKey> cachedKeys = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public AppleOAuthClient(
            WebClient appleWebClient,
            @Value("${apple.web-client-id}") String webClientId,
            @Value("${apple.app-client-id}") String appClientId) {
        this.appleWebClient = appleWebClient;
        this.allowedAudiences = List.of(webClientId, appClientId);
    }

    public AppleUserInfo getUserInfo(String identityToken) {
        try {
            return verify(identityToken, currentKeys());
        } catch (AppleKeyNotFoundException e) {
            // Apple이 서명 키를 회전했을 수 있으니 캐시를 한 번 강제 갱신해 재시도한다.
            try {
                return verify(identityToken, refreshKeys());
            } catch (RuntimeException retryFailure) {
                throw new AuthException(AuthErrorCode.APPLE_AUTH_FAILED);
            }
        } catch (RuntimeException e) {
            throw new AuthException(AuthErrorCode.APPLE_AUTH_FAILED);
        }
    }

    // 네트워크 호출 없이 서명/iss/aud 검증만 수행한다. JWKS 목록을 인자로 받아 순수 로직만 단위 테스트한다.
    AppleUserInfo verify(String identityToken, List<ApplePublicKeysResponse.ApplePublicKey> keys) {
        PublicKey publicKey = resolvePublicKey(extractHeaderKid(identityToken), keys);

        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(identityToken)
                .getPayload();

        // 어느 client id(웹 Service ID/앱 Bundle ID)로 검증됐는지도 함께 반환한다 —
        // authorizationCode 교환 시 client_secret의 sub로 동일한 client id를 써야 하기 때문이다.
        String matchedAudience = allowedAudiences.stream()
                .filter(aud -> claims.getAudience().contains(aud))
                .findFirst()
                .orElseThrow(() -> new JwtException("unexpected apple audience: " + claims.getAudience()));

        return new AppleUserInfo(claims.getSubject(), claims.get("email", String.class), matchedAudience);
    }

    // jjwt는 서명 검증 없이 헤더만 읽는 API가 없어, kid를 얻기 위해 헤더 부분만 직접 디코딩한다.
    private String extractHeaderKid(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtException("malformed apple identity token");
        }
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            Map<?, ?> header = OBJECT_MAPPER.readValue(headerBytes, Map.class);
            Object kid = header.get("kid");
            if (kid == null) {
                throw new JwtException("apple identity token header has no kid");
            }
            return kid.toString();
        } catch (IOException | IllegalArgumentException e) {
            throw new JwtException("failed to parse apple identity token header", e);
        }
    }

    private PublicKey resolvePublicKey(String kid, List<ApplePublicKeysResponse.ApplePublicKey> keys) {
        ApplePublicKeysResponse.ApplePublicKey key = keys.stream()
                .filter(k -> kid.equals(k.kid()))
                .findFirst()
                .orElseThrow(AppleKeyNotFoundException::new);
        return toRsaPublicKey(key);
    }

    private PublicKey toRsaPublicKey(ApplePublicKeysResponse.ApplePublicKey key) {
        try {
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.n()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.e()));
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<ApplePublicKeysResponse.ApplePublicKey> currentKeys() {
        if (cachedKeys.isEmpty() || Instant.now().isAfter(cachedAt.plus(JWKS_CACHE_TTL))) {
            return refreshKeys();
        }
        return cachedKeys;
    }

    private synchronized List<ApplePublicKeysResponse.ApplePublicKey> refreshKeys() {
        ApplePublicKeysResponse response = appleWebClient.get()
                .uri("/auth/keys")
                .retrieve()
                .bodyToMono(ApplePublicKeysResponse.class)
                .block();
        if (response == null || response.keys() == null || response.keys().isEmpty()) {
            throw new IllegalStateException("failed to fetch apple jwks");
        }
        cachedKeys = response.keys();
        cachedAt = Instant.now();
        return cachedKeys;
    }

    private static final class AppleKeyNotFoundException extends RuntimeException {
    }

    // 이메일은 Apple 계정 이메일 비공개 설정이나 최초 동의 거부 시 null일 수 있다.
    // audience는 이 토큰 검증에 실제로 매칭된 client id(웹 Service ID 또는 앱 Bundle ID)다.
    public record AppleUserInfo(String snsId, String email, String audience) {
    }
}
