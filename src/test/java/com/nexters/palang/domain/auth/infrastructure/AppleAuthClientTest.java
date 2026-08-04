package com.nexters.palang.domain.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 네트워크(토큰 교환) 없이, client_secret JWT를 만드는 로직만 검증한다.
class AppleAuthClientTest {

    private static final String TEAM_ID = "TEAM1234";
    private static final String KEY_ID = "KEY1234";
    private static final String CLIENT_ID = "com.palang.app";

    @Test
    @DisplayName("client_secret은 Team ID/Key ID/client id로 서명된 유효한 ES256 JWT다")
    void buildClientSecretProducesSignedJwtWithExpectedClaims() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        AppleAuthClient appleAuthClient = new AppleAuthClient(null, TEAM_ID, KEY_ID, pem);

        String clientSecret = appleAuthClient.buildClientSecret(CLIENT_ID);

        Claims claims = parseWithPublicKey(clientSecret, keyPair.getPublic());
        assertThat(claims.getIssuer()).isEqualTo(TEAM_ID);
        assertThat(claims.getSubject()).isEqualTo(CLIENT_ID);
        assertThat(claims.getAudience()).containsExactly("https://appleid.apple.com");
    }

    @Test
    @DisplayName("private key가 설정되지 않으면 authorizationCode 교환을 건너뛰고 빈 값을 반환한다")
    void exchangeForRefreshTokenIsNoOpWhenPrivateKeyMissing() {
        AppleAuthClient appleAuthClient = new AppleAuthClient(null, TEAM_ID, KEY_ID, "");

        Optional<String> result = appleAuthClient.exchangeForRefreshToken("auth-code", CLIENT_ID);

        assertThat(result).isEmpty();
    }

    private Claims parseWithPublicKey(String jwt, PublicKey publicKey) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}
