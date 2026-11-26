package com.nexters.palang.domain.group.presentation.dto;

import com.nexters.palang.domain.group.domain.Group;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

// 방 설정 변경: 이름과 정원만 바꾼다. 책은 대상이 아니다(책 추가/책 설정 API 사용).
// version: 설정 화면을 열 때 받은 모임 상세의 version. 보내면 그사이 다른 곳에서 설정이 바뀐 경우 저장하지 않는다(S03).
// startDate/endDate: 배포된 앱 호환용(deprecated). 둘 다 보내면 대표 책(가장 먼저 등록된 책)의 기간을 바꾸고, 둘 다
// 비우면 그대로 둔다. 새 앱은 보내지 말고 책 설정 API(PATCH /api/groups/{groupId}/books/{entryId})를 쓴다.
public record UpdateGroupRequest(
        @NotBlank(message = "모임명은 필수입니다.")
        @Schema(example = "주말 독서 모임", description = "앞뒤 공백 제거 후 1~15자(이모지 등은 보이는 글자 1개를 1자로 셈)")
        String name,

        @Min(value = Group.MIN_CAPACITY, message = "인원은 최소 2명 이상이어야 합니다.")
        @Max(value = Group.MAX_CAPACITY, message = "인원은 최대 10명까지 가능합니다.")
        @Schema(example = "6") int capacity,

        @Schema(example = "2026-08-20", nullable = true, deprecated = true,
                description = "[호환용] 대표 책의 시작일. 비우면 기간을 바꾸지 않는다. 새 앱은 책 설정 API를 쓴다.")
        LocalDate startDate,

        @Schema(example = "2026-09-27", nullable = true, deprecated = true,
                description = "[호환용] 대표 책의 종료일. 비우면 기간을 바꾸지 않는다. 새 앱은 책 설정 API를 쓴다.")
        LocalDate endDate,

        @Schema(example = "3", nullable = true,
                description = "설정 화면을 열 때 받은 모임 상세의 version. 보내면 그사이 설정이 바뀐 경우 409(GROUP_409_5)로 "
                        + "거절한다. 비우면 검사하지 않는다(배포된 앱 호환).")
        Long version
) {
}
