package com.nexters.palang.domain.notification.domain;

// 알림함 상단 [전체 | 활동 | 모임] 탭. ACTIVITY는 group_id가 없는(전역) 알림, GROUP은 group_id가 있는 알림.
public enum NotificationScope {
    ALL,
    ACTIVITY,
    GROUP,
}
