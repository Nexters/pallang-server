package com.nexters.palang.domain.notification.infrastructure.fcm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoOpFcmPushSenderTest {

    @Test
    @DisplayName("발송을 시도해도 예외 없이 빈 무효 토큰 목록을 반환한다")
    void sendMulticastReturnsEmptyList() {
        NoOpFcmPushSender sender = new NoOpFcmPushSender();

        List<String> invalidTokens = sender.sendMulticast(List.of("token-a"), "제목", "내용", Map.of());

        assertThat(invalidTokens).isEmpty();
    }
}
