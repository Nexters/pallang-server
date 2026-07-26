package com.nexters.palang.domain.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// local 프로파일 전용 개발용 요청 — userId를 생략하면 새 테스트 유저를 만든다.
public record DevLoginRequest(
        @Schema(example = "1", description = "생략하면 새 테스트 유저를 만들어 로그인 처리한다.")
        Long userId
) {
}
