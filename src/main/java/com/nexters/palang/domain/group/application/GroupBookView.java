package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.ReadingStatus;

// 책 카드 하나: 모임 책과 조회 시점 상태, 그 책에 쌓인 기록 수.
// passageCount: 지워지지 않은 문장(대목) 수. opinionCount: 그 문장들에 달린 지워지지 않은 의견(흔적) 수 — 의견의
// 답글(댓글)과 좋아요는 세지 않고, 조회자가 차단한 작성자의 의견은 피드와 같은 기준으로 뺀다(L10).
public record GroupBookView(GroupBookEntry entry, ReadingStatus status, long passageCount, long opinionCount) {
}
