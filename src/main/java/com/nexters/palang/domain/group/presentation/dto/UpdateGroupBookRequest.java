package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.domain.group.domain.ReadingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

// 책 설정 변경. 판본은 바꿀 수 없다.
// - 기간: 저장할 기간 전체를 보낸다. 둘 다 비우면 기간 없음으로 저장한다. 바꾸지 않은 기존 과거 날짜는 그대로 보내도
//   되지만, 새로 바꾼 날짜는 오늘(한국 시간) 이후여야 한다.
// - 상태: 사용자가 "읽는 중/완독"을 직접 바꾼 경우에만 보낸다. 보내면 그 값으로 고정되고(날짜에 따른 자동 이동 중단),
//   보내지 않으면 기존 상태 방식이 유지된다.
public record UpdateGroupBookRequest(
        @Schema(example = "2026-11-20", nullable = true, description = "독서 시작일. 종료일과 함께 넣거나 함께 비운다.")
        LocalDate startDate,

        @Schema(example = "2026-12-20", nullable = true, description = "독서 종료일. 시작일과 함께 넣거나 함께 비운다.")
        LocalDate endDate,

        @Schema(example = "FINISHED", nullable = true,
                description = "사용자가 직접 고른 독서 상태(READING/FINISHED). 상태를 건드리지 않았으면 보내지 않는다.")
        ReadingStatus status
) {
}
