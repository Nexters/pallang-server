package com.nexters.palang.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 이 코드베이스 최초의 @Scheduled 작업(GroupEndingSoonNotifier)을 위해 스케줄링을 활성화한다.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
