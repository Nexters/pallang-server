package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminJwtProviderTest {

    @Test
    @DisplayName("발급한 토큰은 스스로 검증에 성공한다")
    void issuedTokenIsValid() {
        AdminJwtProvider provider = new AdminJwtProvider("test-admin-secret-at-least-32-bytes-long", 3600);

        String token = provider.createToken();

        assertThat(provider.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("다른 비밀키로 서명된 토큰은 검증에 실패한다")
    void tokenSignedWithDifferentSecretIsInvalid() {
        AdminJwtProvider issuer = new AdminJwtProvider("secret-a-at-least-32-bytes-long-000000", 3600);
        AdminJwtProvider verifier = new AdminJwtProvider("secret-b-at-least-32-bytes-long-000000", 3600);

        String token = issuer.createToken();

        assertThat(verifier.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다")
    void expiredTokenIsInvalid() throws InterruptedException {
        AdminJwtProvider provider = new AdminJwtProvider("test-admin-secret-at-least-32-bytes-long", 0);

        String token = provider.createToken();
        Thread.sleep(10);

        assertThat(provider.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("비밀키를 설정하지 않으면 기동마다 임의의 키로 대체된다")
    void fallsBackToRandomKeyWhenSecretMissing() {
        AdminJwtProvider a = new AdminJwtProvider("", 3600);
        AdminJwtProvider b = new AdminJwtProvider("", 3600);

        String tokenFromA = a.createToken();

        assertThat(a.isValid(tokenFromA)).isTrue();
        assertThat(b.isValid(tokenFromA)).isFalse();
    }

    @Test
    @DisplayName("엉뚱한 문자열은 토큰으로 인정되지 않는다")
    void garbageStringIsInvalid() {
        AdminJwtProvider provider = new AdminJwtProvider("test-admin-secret-at-least-32-bytes-long", 3600);

        assertThat(provider.isValid("not-a-jwt")).isFalse();
    }
}
