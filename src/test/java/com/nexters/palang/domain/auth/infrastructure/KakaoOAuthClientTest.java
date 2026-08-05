package com.nexters.palang.domain.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 네트워크(연동 해제 호출) 없이, Admin Key 미설정 시 no-op으로 건너뛰는 분기만 검증한다.
class KakaoOAuthClientTest {

    @Test
    @DisplayName("Admin Key가 설정되지 않으면 연동 해제 호출 없이 조용히 건너뛴다")
    void unlinkIsNoOpWhenAdminKeyMissing() {
        KakaoOAuthClient kakaoOAuthClient = new KakaoOAuthClient(null, "");

        assertThatCode(() -> kakaoOAuthClient.unlink("sns-1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Admin Key가 null이어도 연동 해제 호출 없이 조용히 건너뛴다")
    void unlinkIsNoOpWhenAdminKeyNull() {
        KakaoOAuthClient kakaoOAuthClient = new KakaoOAuthClient(null, null);

        assertThatCode(() -> kakaoOAuthClient.unlink("sns-1")).doesNotThrowAnyException();
    }
}
