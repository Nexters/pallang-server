package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionResponse.DecorationResponse;
import java.time.LocalDateTime;
import java.util.List;

// 흔적 상세(FR-OPINION-05): 해당 흔적 작성자가 기록한 꾸밈을 그대로 보여준다 (병합된 3개가 아니라 이 Opinion 자신의 전부).
public record OpinionDetailResponse(
        Long opinionId,
        Long passageId,
        Long userId,
        String nickname,
        String content,
        int likeCount,
        List<DecorationResponse> decorations,
        LocalDateTime createdAt
) {
    public static OpinionDetailResponse from(Opinion opinion) {
        List<DecorationResponse> decorations = opinion.getDecorations().stream()
                .map(DecorationResponse::from)
                .toList();
        return new OpinionDetailResponse(
                opinion.getId(),
                opinion.getPassage().getId(),
                opinion.getUser().getId(),
                opinion.getUser().getNickname(),
                opinion.getContent(),
                opinion.getLikeCount(),
                decorations,
                opinion.getCreatedAt()
        );
    }
}
