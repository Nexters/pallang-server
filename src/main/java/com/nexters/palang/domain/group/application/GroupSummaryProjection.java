package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.domain.GroupReadingSummary;
import java.time.LocalDate;
import java.util.List;

// 내 모임 목록(홈 "모임" 탭)에서 카드 하나를 그리는 데 필요한 값만 모은 조회 전용 뷰.
// book*/startDate/endDate/ended는 기존 앱 호환용으로 대표 책(가장 먼저 등록된 entry) 기준이다 — 기간이 없는 entry면
// 날짜는 null. 여러 권 구조의 카드 값은 readingSummary/memberPreviews를 쓴다.
public record GroupSummaryProjection(
        Long groupId,
        String name,
        Long bookId,
        String bookTitle,
        String bookCoverImageUrl,
        long memberCount,
        int capacity,
        LocalDate startDate,
        LocalDate endDate,
        boolean ended,
        boolean isHost,
        GroupReadingSummary readingSummary,
        List<GroupMemberPreview> memberPreviews
) {
}
