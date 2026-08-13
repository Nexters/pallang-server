package com.nexters.palang.domain.decoration.application;

import com.nexters.palang.domain.decoration.domain.EffectType;
import java.time.LocalDateTime;

// 꾸밈 병합 노출(FR-VIEW-03) 정렬/겹침 판정에 필요한 필드만 담은 조회 전용 프로젝션.
// likeCount/opinionCreatedAt은 Decoration 자신의 값이 아니라 소속 Opinion의 값이다.
public record DecorationMergeCandidate(
        Long decorationId,
        int startOffset,
        int endOffset,
        EffectType effectType,
        String color,
        int likeCount,
        LocalDateTime opinionCreatedAt
) {
}
