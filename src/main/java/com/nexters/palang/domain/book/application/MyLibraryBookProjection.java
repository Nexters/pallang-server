package com.nexters.palang.domain.book.application;

// 내 서재 항목은 도서 단위가 아니라 (도서, 모임) 단위다(이슈 #177). 같은 책이라도 전역 공개 대목에서의
// 활동과 모임 전용 대목에서의 활동은 서로 다른 항목으로 내려가며, groupId/groupName이 null이면 전역 항목이다.
// passageCount/opinionCount도 그 항목의 범위(전역이면 group_id IS NULL, 모임이면 그 모임) 안에서만 센다.
public record MyLibraryBookProjection(
        Long bookId,
        String title,
        String author,
        String publisher,
        String coverImageUrl,
        Long groupId,
        String groupName,
        long passageCount,
        long opinionCount
) {
}
