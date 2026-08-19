package com.nexters.palang.domain.notification.infrastructure.fcm;

import java.util.List;
import java.util.Map;

public interface FcmPushSender {

    /**
     * 주어진 토큰들로 푸시를 발송하고, 더 이상 유효하지 않은(재설치/삭제 등으로 무효화된) 토큰 목록을 반환한다.
     * 호출부(NotificationPushDispatcher)는 이 목록을 DeviceToken 테이블에서 정리한다.
     */
    List<String> sendMulticast(List<String> tokens, String title, String body, Map<String, String> data);
}
