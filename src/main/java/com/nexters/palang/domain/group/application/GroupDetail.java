package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.domain.Group;

// 모임 상세 조회용 뷰. memberCount는 Group 엔티티가 스스로 알 수 없어(다른 애그리거트인 GroupMember의
// 집계값) 서비스가 조회해서 함께 묶어 내려준다.
public record GroupDetail(Group group, long memberCount) {
}
