package com.nexters.palang.domain.passage.application;

import java.time.LocalDateTime;

public record MyPassageProjection(
        Long passageId,
        Long bookId,
        Long opinionId,
        int pageNumber,
        String quotedText,
        boolean isSpoiler,
        LocalDateTime createdAt,
        // 대목이 모임 전용이면 그 모임, 전역 공개 대목이면 null (이슈 #177: 프론트가 groupId를 붙여 이동해야 조회된다)
        Long groupId,
        String groupName
) {
}
