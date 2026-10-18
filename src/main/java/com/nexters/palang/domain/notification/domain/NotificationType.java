package com.nexters.palang.domain.notification.domain;

public enum NotificationType {
    OPINION_LIKED,
    OPINION_COMMENTED,
    // 더 이상 새로 발송하지 않는다(이슈 #193). 과거에 쌓인 알림을 그대로 보여주기 위해서만 남겨둔다.
    BOOK_NEW_OPINIONS,
    // 모임 가입 완료(가입자 본인에게).
    GROUP_JOINED,
    // 새 멤버 가입(호스트에게).
    GROUP_MEMBER_JOINED,
    // 모임 시작일/종료일 변경(호스트 제외 멤버 전원에게).
    GROUP_SCHEDULE_CHANGED,
    // 모임 종료 하루 전(멤버 전원에게).
    GROUP_ENDING_SOON,
}
