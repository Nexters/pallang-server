package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.nexters.palang.domain.group.domain.ReadingStatusMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GroupBookResponse(
        @Schema(example = "10", description = "모임 책 ID. 책 카드에서 기록 화면/책 설정으로 이동할 때 이 값을 쓴다.",
                requiredMode = Schema.RequiredMode.REQUIRED) Long entryId,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "모순", requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(example = "양귀자", requiredMode = Schema.RequiredMode.REQUIRED) String author,
        @Schema(example = "https://image.example.com/cover.jpg", nullable = true) String coverImageUrl,
        @Schema(example = "2026-11-20", nullable = true, description = "독서 시작일. 기간이 없으면 null.") LocalDate startDate,
        @Schema(example = "2026-12-20", nullable = true, description = "독서 종료일. 기간이 없으면 null.") LocalDate endDate,
        @Schema(example = "READING", description = "조회 시점 기준 독서 상태(서버 계산)",
                requiredMode = Schema.RequiredMode.REQUIRED) ReadingStatus status,
        @Schema(example = "AUTO", description = "AUTO: 종료일 다음 날 자동으로 완독 / MANUAL: 모임장이 직접 고른 상태 유지",
                requiredMode = Schema.RequiredMode.REQUIRED) ReadingStatusMode statusMode,
        @Schema(example = "6", description = "지워지지 않은 문장 수", requiredMode = Schema.RequiredMode.REQUIRED) long passageCount,
        @Schema(example = "17", description = "문장에 달린 의견 수(답글/좋아요 제외, 내가 차단한 작성자 제외)",
                requiredMode = Schema.RequiredMode.REQUIRED) long opinionCount,
        @Schema(example = "2026-11-20T12:00:00", description = "모임에 책을 등록한 시각(목록 정렬 기준)",
                requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime registeredAt
) {
}
