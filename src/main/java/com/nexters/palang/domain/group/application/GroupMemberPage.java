package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import org.springframework.data.domain.Page;

// 멤버 명단 조회 결과. 명단 제목의 "현재 인원/정원"은 페이지 크기와 무관하게 전체 현재 멤버 수(members의 전체 건수)와
// 모임 정원이다. viewerId는 각 행의 "본인" 표시에 쓴다.
public record GroupMemberPage(Group group, Page<GroupMember> members, Long viewerId) {
}
