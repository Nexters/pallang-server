package com.nexters.palang.domain.notification.domain;

public enum NotificationType {
    OPINION_LIKED,
    OPINION_COMMENTED,
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
