package com.nexters.palang.domain.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 카카오 사용자 정보 조회 API(GET /v2/user/me) 응답 중 식별자만 사용한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(Long id) {
}
