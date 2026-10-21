package com.nexters.palang.domain.notification.domain;

// 알림함 상단 [전체 | 활동 | 모임 | 운영] 탭. ACTIVITY는 group_id가 없는(전역) 알림, GROUP은 group_id가 있는 알림,
// OPERATION은 운영 안내(CONTENT_REMOVED). 운영 안내는 모임 게시물에 대한 것이어도 group_id를 두지 않고
// ACTIVITY에서도 제외해, "전체"와 "운영" 탭에만 보이게 한다.
public enum NotificationScope {
    ALL,
    ACTIVITY,
    GROUP,
    OPERATION,
}
