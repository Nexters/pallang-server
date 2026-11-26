package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.domain.group.domain.Group;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// 이름 길이(앞뒤 공백 제거 후 1~15 grapheme)는 이모지/결합 문자 때문에 @Size로 셀 수 없어 도메인(GroupName)에서
// 검증한다. 기간은 선택이며, 넣는다면 시작일·종료일을 함께 넣어야 하고 둘 다 오늘(Asia/Seoul) 이후여야 한다.
public record CreateGroupRequest(
        @NotBlank(message = "모임명은 필수입니다.")
        @Schema(example = "고전 뽀개기", description = "앞뒤 공백 제거 후 1~15자(이모지 등은 보이는 글자 1개를 1자로 셈)")
        String name,

        @NotNull(message = "책 선택은 필수입니다.")
        @Schema(example = "1") Long bookId,

        @Min(value = Group.MIN_CAPACITY, message = "인원은 최소 2명 이상이어야 합니다.")
        @Max(value = Group.MAX_CAPACITY, message = "인원은 최대 10명까지 가능합니다.")
        @Schema(example = "4") int capacity,

        @Schema(example = "2026-08-20", nullable = true, description = "독서 시작일. 종료일과 함께 넣거나 함께 비운다.")
        LocalDate startDate,

        @Schema(example = "2026-09-20", nullable = true, description = "독서 종료일. 시작일과 함께 넣거나 함께 비운다.")
        LocalDate endDate
) {
}
