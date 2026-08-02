package com.nexters.palang.domain.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 카카오 사용자 정보 조회 API(GET /v2/user/me) 응답 중 식별자와 이메일만 사용한다.
// 이메일은 카카오계정(이메일) 동의항목을 거부했거나 미인증 상태면 kakao_account 자체가 없거나 email이 null일 수 있다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(String email) {
    }
}
