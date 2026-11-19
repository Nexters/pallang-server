package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.domain.group.domain.Group;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

// 방 설정 변경: 책은 대상이 아니다(등록 후 판본 교체 불가).
// startDate/endDate는 기존 앱 호환용이다 — 둘 다 보내면 대표 책(가장 먼저 등록된 책)의 기간을 바꾸고, 둘 다
// 비우면 기간은 그대로 둔다. 책별 기간은 책 설정 API(#225)로 옮겨가며, 그때 이 두 필드는 제거한다.
public record UpdateGroupRequest(
        @NotBlank(message = "모임명은 필수입니다.")
        @Schema(example = "주말 독서 모임", description = "앞뒤 공백 제거 후 1~15자(이모지 등은 보이는 글자 1개를 1자로 셈)")
        String name,

        @Min(value = Group.MIN_CAPACITY, message = "인원은 최소 2명 이상이어야 합니다.")
        @Max(value = Group.MAX_CAPACITY, message = "인원은 최대 10명까지 가능합니다.")
        @Schema(example = "6") int capacity,

        @Schema(example = "2026-08-20", nullable = true, description = "대표 책의 시작일(호환용). 비우면 기간을 바꾸지 않는다.")
        LocalDate startDate,

        @Schema(example = "2026-09-27", nullable = true, description = "대표 책의 종료일(호환용). 비우면 기간을 바꾸지 않는다.")
        LocalDate endDate
) {
}
