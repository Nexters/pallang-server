package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;

// primaryEntry: 모임의 대표 책(가장 먼저 등록된 entry). 관리자 목록의 책/기간 칸은 이 값으로 채운다.
public record AdminGroupSearchResult(Group group, GroupBookEntry primaryEntry, long memberCount) {
}
