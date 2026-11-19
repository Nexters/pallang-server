package com.nexters.palang.domain.group.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record GroupSummaryResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long groupId,
        @Schema(example = "고전 뽀개기", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(example = "1", description = "대표 책(가장 먼저 등록된 책) ID", requiredMode = Schema.RequiredMode.REQUIRED) Long bookId,
        @Schema(example = "채식주의자", requiredMode = Schema.RequiredMode.REQUIRED) String bookTitle,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg", nullable = true) String bookCoverImageUrl,
        @Schema(example = "2", requiredMode = Schema.RequiredMode.REQUIRED) long memberCount,
        @Schema(example = "4", requiredMode = Schema.RequiredMode.REQUIRED) int capacity,
        @Schema(example = "2026-08-20", nullable = true, description = "대표 책(가장 먼저 등록된 책)의 시작일. 기간이 없으면 null.") LocalDate startDate,
        @Schema(example = "2026-09-20", nullable = true, description = "대표 책(가장 먼저 등록된 책)의 종료일. 기간이 없으면 null.") LocalDate endDate,
        @Schema(example = "false", description = "대표 책이 완독 상태인지 여부(종료일 다음 날부터 true, 기간이 없으면 false). 안내용 배지 표시에만 쓰이며, "
                + "이 값이 true여도 모임 목록 노출/흔적·의견 작성 등은 계속 가능합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean ended,
        @Schema(example = "true", description = "현재 로그인한 유저가 이 모임의 모임장인지 여부. "
                + "모임 카드의 '더보기' 메뉴에서 모임장 전용 항목(방 설정 변경 등) 노출 여부를 "
                + "프론트에서 판단하는 데 사용합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean isHost
) {
}
