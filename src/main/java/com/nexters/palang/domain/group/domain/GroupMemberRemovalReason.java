package com.nexters.palang.domain.group.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 모임장이 멤버를 내보낼 때 고르는 사유(5.2). 감사 이력에만 남기고 일반 멤버/대상에게는 공개하지 않는다.
@Getter
@AllArgsConstructor
public enum GroupMemberRemovalReason {
    ABUSIVE_BEHAVIOR("욕설·비방 등 다른 멤버에게 불편을 주는 행동"),
    SPAM_OR_OFF_TOPIC("광고·홍보 또는 모임과 무관한 내용 반복 게시"),
    MEMBER_REQUEST_OR_MISTAKE("잘못 참여했거나 본인이 나가기를 요청함"),
    OTHER("기타(직접입력)");

    private final String description;
}
