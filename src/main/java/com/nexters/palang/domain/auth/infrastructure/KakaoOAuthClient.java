package com.nexters.palang.domain.auth.infrastructure;

import com.nexters.palang.domain.auth.infrastructure.dto.KakaoUserInfoResponse;
import com.nexters.palang.global.security.AuthErrorCode;
import com.nexters.palang.global.security.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final WebClient kakaoWebClient;

    // 모바일 앱이 카카오 SDK로 로그인 후 전달한 액세스 토큰을 카카오 서버에 직접 검증한다.
    public String getSnsId(String kakaoAccessToken) {
        KakaoUserInfoResponse response;
        try {
            response = kakaoWebClient.get()
                    .uri("/v2/user/me")
                    .headers(headers -> headers.setBearerAuth(kakaoAccessToken))
                    .retrieve()
                    .bodyToMono(KakaoUserInfoResponse.class)
                    .block();
        } catch (RuntimeException e) {
            throw new AuthException(AuthErrorCode.KAKAO_AUTH_FAILED);
        }

        if (response == null || response.id() == null) {
            throw new AuthException(AuthErrorCode.KAKAO_AUTH_FAILED);
        }
        return String.valueOf(response.id());
    }
}
