package com.nexters.palang.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.notification.infrastructure.fcm.FcmPushSender;
import com.nexters.palang.domain.notification.infrastructure.fcm.NoOpFcmPushSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FirebaseConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(FirebaseConfig.class);

    @Test
    @DisplayName("firebase.credentials-base64가 비어 있으면 NoOpFcmPushSender로 폴백한다")
    void fallsBackToNoOpWhenCredentialsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FcmPushSender.class);
            assertThat(context.getBean(FcmPushSender.class)).isInstanceOf(NoOpFcmPushSender.class);
        });
    }
}
