package com.nexters.palang.domain.group.domain;

// 모임 자체의 상태. MERGED는 합치기(#227)로 다른 모임(keeper)에 흡수된 source 모임이다 — 행은 지우지 않고
// 옛 링크를 keeper로 연결하는 데 쓴다. 일반 목록/가입/쓰기는 ACTIVE 모임만 대상으로 한다.
public enum GroupStatus {
    ACTIVE,
    MERGED,
}
