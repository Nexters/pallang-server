package com.nexters.palang.domain.opinion.application;

import java.time.LocalDateTime;

public record MyOpinionProjection(
        Long opinionId,
        Long bookId,
        String bookTitle,
        String author,
        String bookCoverImageUrl,
        Long passageId,
        String quotedText,
        int pageNumber,
        String content,
        int likeCount,
        LocalDateTime createdAt,
        // 대목이 모임 전용이면 그 모임, 전역 공개 대목이면 null (이슈 #177: 프론트가 groupId를 붙여 이동해야 조회된다)
        Long groupId,
        String groupName
) {
}
