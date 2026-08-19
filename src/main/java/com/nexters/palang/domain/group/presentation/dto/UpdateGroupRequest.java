package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.domain.group.domain.Group;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// 방 설정 변경: 책은 대상이 아니다(생성 후 불변).
public record UpdateGroupRequest(
        @NotBlank(message = "모임명은 필수입니다.")
        @Size(max = Group.NAME_MAX_LENGTH, message = "모임명은 최대 15자까지 가능합니다.")
        @Schema(example = "주말 독서 모임") String name,

        @Min(value = Group.MIN_CAPACITY, message = "인원은 최소 2명 이상이어야 합니다.")
        @Max(value = Group.MAX_CAPACITY, message = "인원은 최대 10명까지 가능합니다.")
        @Schema(example = "6") int capacity,

        @NotNull(message = "시작일은 필수입니다.")
        @Schema(example = "2026-08-20") LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        @Schema(example = "2026-09-27") LocalDate endDate
) {
}
