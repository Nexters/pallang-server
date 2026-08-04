package com.nexters.palang.domain.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Apple 토큰 교환(POST /auth/token) 응답. authorizationCode를 refresh token으로 바꾸는 데만 쓰므로
// refreshToken 외 나머지 필드는 참고용으로만 갖고 있는다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("id_token") String idToken
) {
}
