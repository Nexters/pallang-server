package com.nexters.palang.domain.group.domain;

// 멤버십 상태. 탈퇴/내보내기를 행 삭제가 아니라 상태 변경으로 남겨야 "내보내진 계정은 removedAt 이전에 발급된
// 초대로 재가입 불가"(#226) 같은 이력 기반 정책을 판단할 수 있다. 인원 수와 권한 검사는 ACTIVE만 센다.
public enum GroupMemberStatus {
    ACTIVE,
    LEFT,
    REMOVED,
}
