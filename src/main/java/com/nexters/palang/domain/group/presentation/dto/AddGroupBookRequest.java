package com.nexters.palang.domain.group.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// 모임에 함께 읽을 책 추가. 기간은 선택이며, 넣는다면 시작일·종료일을 함께 넣고 둘 다 오늘(한국 시간) 이후여야 한다.
public record AddGroupBookRequest(
        @NotNull(message = "책 선택은 필수입니다.")
        @Schema(example = "1", description = "추가할 도서(판본) ID") Long bookId,

        @Schema(example = "2026-11-20", nullable = true, description = "독서 시작일. 종료일과 함께 넣거나 함께 비운다.")
        LocalDate startDate,

        @Schema(example = "2026-12-20", nullable = true, description = "독서 종료일. 시작일과 함께 넣거나 함께 비운다.")
        LocalDate endDate
) {
}
