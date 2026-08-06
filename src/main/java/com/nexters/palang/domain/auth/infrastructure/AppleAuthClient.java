package com.nexters.palang.domain.auth.infrastructure;

import com.nexters.palang.domain.auth.infrastructure.dto.AppleTokenResponse;
import com.nexters.palang.global.security.AuthErrorCode;
import com.nexters.palang.global.security.AuthException;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

// 로그인 시점에는 authorizationCode를 애플에 즉시 교환해 회원탈퇴 revoke용 refresh token을 미리
// 확보하고(authorizationCode는 발급 후 수 분 내에만 유효해 탈퇴 시점엔 다시 교환할 방법이 없다),
// 회원탈퇴 시점에는 그 refresh token으로 애플 연동을 해제(revoke)한다. 둘 다 실패해도(키 미설정 포함)
// 로그인/탈퇴 자체는 계속 진행하는 best-effort 성격이다.
@Slf4j
@Component
public class AppleAuthClient {

    private static final String TOKEN_URI = "/auth/token";
    private static final String REVOKE_URI = "/auth/revoke";
    private static final String AUDIENCE = "https://appleid.apple.com";
    private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);

    private final WebClient appleWebClient;
    private final String teamId;
    private final String keyId;
    // 회원탈퇴 시점엔 로그인 때와 달리 검증된 identity token이 없어 aud(client id)를 알 수 없다.
    // 현재는 iOS 앱만 지원하므로 앱 Bundle ID를 그대로 쓴다 — 웹 로그인을 붙이면 refresh token 발급 시
    // 사용한 client id를 함께 저장해두고 그 값을 써야 한다.
    private final String appClientId;
    private final PrivateKey privateKey;

    public AppleAuthClient(
            WebClient appleWebClient,
            @Value("${apple.team-id}") String teamId,
            @Value("${apple.key-id}") String keyId,
            @Value("${apple.app-client-id}") String appClientId,
            @Value("${apple.private-key}") String privateKeyPem) {
        this.appleWebClient = appleWebClient;
        this.teamId = teamId;
        this.keyId = keyId;
        this.appClientId = appClientId;
        // Sign in with Apple Key가 아직 발급/설정되지 않은 환경(로컬 등)에서도 앱이 정상 기동되도록,
        // 비어 있으면 파싱을 건너뛰고 exchangeForRefreshToken/revoke를 무조건 no-op으로 만든다.
        this.privateKey = (privateKeyPem == null || privateKeyPem.isBlank()) ? null : parsePrivateKey(privateKeyPem);
    }

    public Optional<String> exchangeForRefreshToken(String authorizationCode, String clientId) {
        if (privateKey == null) {
            log.debug("애플 Private Key가 설정되지 않아 authorizationCode 교환을 건너뜁니다.");
            return Optional.empty();
        }
        try {
            String clientSecret = buildClientSecret(clientId);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("code", authorizationCode);
            form.add("grant_type", "authorization_code");

            AppleTokenResponse response = appleWebClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(AppleTokenResponse.class)
                    .block();

            return Optional.ofNullable(response).map(AppleTokenResponse::refreshToken);
        } catch (RuntimeException e) {
            log.warn("애플 authorizationCode 교환에 실패해 revoke용 refresh token을 확보하지 못했습니다.", e);
            return Optional.empty();
        }
    }

    // 회원탈퇴 시 로그인 때 저장해둔 refresh token으로 애플 연동을 해제한다.
    // Private Key가 설정되지 않은 환경에서는 no-op으로 건너뛴다.
    public void revoke(String appleRefreshToken) {
        if (privateKey == null) {
            log.debug("애플 Private Key가 설정되지 않아 연동 해제를 건너뜁니다.");
            return;
        }
        try {
            String clientSecret = buildClientSecret(appClientId);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", appClientId);
            form.add("client_secret", clientSecret);
            form.add("token", appleRefreshToken);
            form.add("token_type_hint", "refresh_token");

            appleWebClient.post()
                    .uri(REVOKE_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (RuntimeException e) {
            throw new AuthException(AuthErrorCode.APPLE_REVOKE_FAILED);
        }
    }

    // client_secret은 Apple이 요구하는 자체 서명 JWT다. iss=Team ID, sub=요청에 쓴 client id,
    // aud는 항상 Apple 고정값이다. 요청마다 새로 만들어 짧게(5분) 만료시킨다.
    String buildClientSecret(String clientId) {
        Date now = new Date();
        return Jwts.builder()
                .header().add("kid", keyId).and()
                .issuer(teamId)
                .subject(clientId)
                .audience().add(AUDIENCE).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + CLIENT_SECRET_TTL.toMillis()))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey parsePrivateKey(String pem) {
        try {
            String sanitized = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] derBytes = Base64.getDecoder().decode(sanitized);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(derBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new IllegalStateException("failed to parse apple private key", e);
        }
    }
}
