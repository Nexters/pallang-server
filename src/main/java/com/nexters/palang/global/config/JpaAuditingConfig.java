package com.nexters.palang.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// created_at/updated_at도 서비스가 쓰는 Clock(Asia/Seoul)과 같은 기준으로 찍는다. 그래야 감사 시각과 서비스가 직접 기록한
// 시각(예: 초대 발급 시각 vs 멤버를 내보낸 시각, #226)을 비교할 때 JVM 기본 시간대에 따라 몇 시간씩 어긋나지 않는다.
// 운영 JVM은 이미 -Duser.timezone=Asia/Seoul이라 값은 그대로이고, UTC로 도는 CI/로컬에서만 차이가 사라진다.
// Clock 빈이 없는 슬라이스 테스트(@DataJpaTest 등)에서는 같은 시간대의 시스템 Clock을 쓴다.
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(ObjectProvider<Clock> clockProvider) {
        return () -> Optional.of(LocalDateTime.now(
                clockProvider.getIfAvailable(() -> Clock.system(ClockConfig.SERVICE_ZONE))));
    }
}
