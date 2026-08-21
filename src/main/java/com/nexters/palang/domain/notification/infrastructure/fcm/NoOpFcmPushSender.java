package com.nexters.palang.domain.notification.infrastructure.fcm;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Firebase 서비스 계정 키(firebase.credentials-base64)가 설정되지 않은 환경(로컬/CI 기본값)에서
 * 앱 부팅이 깨지지 않도록 하는 폴백 구현체. 실제 푸시는 보내지 않고 로그만 남긴다.
 */
@Slf4j
public class NoOpFcmPushSender implements FcmPushSender {

    @Override
    public List<String> sendMulticast(List<String> tokens, String title, String body, Map<String, String> data) {
        log.info("[FCM 비활성화] 푸시 발송 스킵: tokens={}, title={}", tokens.size(), title);
        return List.of();
    }
}
