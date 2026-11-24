package com.nexters.palang.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// "오늘"은 서버 시각 기준 Asia/Seoul의 날짜다(모임 책 기간 검증, 완독 자동 판정). JVM 기본 시간대에 암묵적으로
// 기대지 않도록 시간대를 고정한 Clock을 주입하고, 테스트에서는 고정 시각 Clock으로 바꿔 끼운다.
@Configuration
public class ClockConfig {

    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(SERVICE_ZONE);
    }
}
