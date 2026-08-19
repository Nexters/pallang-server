package com.nexters.palang.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.nexters.palang.domain.notification.infrastructure.fcm.FcmPushSender;
import com.nexters.palang.domain.notification.infrastructure.fcm.FirebaseFcmPushSender;
import com.nexters.palang.domain.notification.infrastructure.fcm.NoOpFcmPushSender;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Firebase 서비스 계정 키(firebase.credentials-base64)가 아직 발급되지 않은 상태에서도
 * 로컬/CI 빌드·구동이 깨지지 않도록, 값이 비어 있으면 FirebaseApp을 아예 초기화하지 않고
 * NoOpFcmPushSender로 폴백한다.
 * (프로퍼티 자체는 항상 존재하고 빈 문자열만 비어있는 상태이므로 @ConditionalOnProperty로는
 * "설정 안 됨"을 정확히 구분할 수 없어, 팩토리 메서드 안에서 직접 분기한다.)
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FcmPushSender fcmPushSender(@Value("${firebase.credentials-base64:}") String credentialsBase64) throws Exception {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            log.warn("firebase.credentials-base64가 설정되지 않아 FCM 푸시가 비활성화됩니다 (NoOpFcmPushSender 사용).");
            return new NoOpFcmPushSender();
        }

        byte[] decoded = Base64.getDecoder().decode(credentialsBase64);
        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
        FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
        FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getApps().get(0);
        return new FirebaseFcmPushSender(app);
    }
}
