package com.nexters.palang.domain.admin.presentation.dto;

import com.nexters.palang.domain.group.domain.Group;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

// 관리자 데이터 정정용. 기간은 모임의 대표 책(가장 먼저 등록된 책)에 적용하며, 일반 모임장 수정과 달리
// 과거 날짜도 그대로 넣을 수 있다(GroupBookEntry.correctPeriod). 둘 다 비우면 기간 없음으로 저장한다.
public record AdminUpdateGroupRequest(
        @NotBlank(message = "모임명은 필수입니다.")
        @Schema(example = "고전 뽀개기", description = "앞뒤 공백 제거 후 1~15자(이모지 등은 보이는 글자 1개를 1자로 셈)")
        String name,

        @Min(value = Group.MIN_CAPACITY, message = "인원은 최소 2명 이상이어야 합니다.")
        @Max(value = Group.MAX_CAPACITY, message = "인원은 최대 10명까지 가능합니다.")
        @Schema(example = "4") int capacity,

        @Schema(example = "2026-08-20", nullable = true, description = "대표 책의 시작일. 종료일과 함께 비우면 기간 없음.")
        LocalDate startDate,

        @Schema(example = "2026-09-20", nullable = true, description = "대표 책의 종료일. 시작일과 함께 비우면 기간 없음.")
        LocalDate endDate
) {
}
