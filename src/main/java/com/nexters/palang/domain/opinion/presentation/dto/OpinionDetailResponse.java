package com.nexters.palang.domain.opinion.presentation.dto;

import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionResponse.DecorationResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

// 흔적 상세(FR-OPINION-05): 해당 흔적 작성자가 기록한 꾸밈을 그대로 보여준다 (병합된 3개가 아니라 이 Opinion 자신의 전부).
public record OpinionDetailResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long opinionId,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
        @Schema(example = "7", requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(example = "책읽는고양이", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(example = "이 문장에서 작가의 의도가 느껴져서 좋았어요.", requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(example = "5") int likeCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<DecorationResponse> decorations,
        @Schema(example = "2026-07-20T14:32:00", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
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
