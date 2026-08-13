package com.nexters.palang.domain.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.domain.auth.infrastructure.dto.ApplePublicKeysResponse;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 네트워크(JWKS 조회) 없이, 서명/발급자/대상 검증 로직만 검증한다.
// 실제 RSA 키쌍으로 identity token을 서명하고, 그 공개키로 JWKS 목록을 직접 구성해 verify()에 넣는다.
class AppleOAuthClientTest {

    private static final String ISSUER = "https://appleid.apple.com";
    private static final String WEB_CLIENT_ID = "com.palang.web";
    private static final String APP_CLIENT_ID = "com.palang.app";
    private static final String KID = "test-kid";

    private AppleOAuthClient appleOAuthClient;
    private RSAPrivateKey privateKey;
    private List<ApplePublicKeysResponse.ApplePublicKey> keys;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        keys = List.of(new ApplePublicKeysResponse.ApplePublicKey(
                "RSA", KID, "sig", "RS256",
                encode(publicKey.getModulus()), encode(publicKey.getPublicExponent())));

        appleOAuthClient = new AppleOAuthClient(null, WEB_CLIENT_ID, APP_CLIENT_ID);
    }

    @Test
    @DisplayName("웹 Service ID를 대상으로 서명된 유효한 identity token은 검증에 성공하고 sub/email을 추출한다")
    void verifySucceedsForWebAudience() {
        String token = token(ISSUER, WEB_CLIENT_ID, "apple-sub-1", "user@example.com", 600);

        AppleOAuthClient.AppleUserInfo userInfo = appleOAuthClient.verify(token, keys);

        assertThat(userInfo.snsId()).isEqualTo("apple-sub-1");
        assertThat(userInfo.email()).isEqualTo("user@example.com");
        assertThat(userInfo.audience()).isEqualTo(WEB_CLIENT_ID);
    }

    @Test
    @DisplayName("앱 Bundle ID를 대상으로 서명된 유효한 identity token도 검증에 성공한다")
    void verifySucceedsForAppAudience() {
        String token = token(ISSUER, APP_CLIENT_ID, "apple-sub-2", null, 600);

        AppleOAuthClient.AppleUserInfo userInfo = appleOAuthClient.verify(token, keys);

        assertThat(userInfo.snsId()).isEqualTo("apple-sub-2");
        assertThat(userInfo.email()).isNull();
        assertThat(userInfo.audience()).isEqualTo(APP_CLIENT_ID);
    }

    @Test
    @DisplayName("발급자(iss)가 Apple이 아니면 검증에 실패한다")
    void verifyFailsWhenIssuerMismatched() {
        String token = token("https://not-apple.example.com", WEB_CLIENT_ID, "apple-sub-3", null, 600);

        assertThatThrownBy(() -> appleOAuthClient.verify(token, keys)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("aud가 허용된 client id가 아니면 검증에 실패한다")
    void verifyFailsWhenAudienceNotAllowed() {
        String token = token(ISSUER, "unknown-client-id", "apple-sub-4", null, 600);

        assertThatThrownBy(() -> appleOAuthClient.verify(token, keys)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("만료된 identity token은 검증에 실패한다")
    void verifyFailsWhenTokenExpired() {
        String token = token(ISSUER, WEB_CLIENT_ID, "apple-sub-5", null, -600);

        assertThatThrownBy(() -> appleOAuthClient.verify(token, keys)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("JWKS 목록에 kid와 일치하는 키가 없으면 검증에 실패한다")
    void verifyFailsWhenKeyNotFound() {
        String token = token(ISSUER, WEB_CLIENT_ID, "apple-sub-6", null, 600);

        assertThatThrownBy(() -> appleOAuthClient.verify(token, List.of())).isInstanceOf(RuntimeException.class);
    }

    private String token(String issuer, String audience, String subject, String email, long expirySeconds) {
        var builder = Jwts.builder()
                .header().add("kid", KID).and()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(subject)
                .expiration(Date.from(Instant.now().plusSeconds(expirySeconds)));
        if (email != null) {
            builder.claim("email", email);
        }
        return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
    }

    private String encode(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
