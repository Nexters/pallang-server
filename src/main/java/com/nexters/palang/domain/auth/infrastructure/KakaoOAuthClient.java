package com.nexters.palang.domain.auth.infrastructure;

import com.nexters.palang.domain.auth.infrastructure.dto.KakaoUserInfoResponse;
import com.nexters.palang.global.security.AuthErrorCode;
import com.nexters.palang.global.security.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String USER_INFO_URI = "/v2/user/me";
    private static final String UNLINK_URI = "/v1/user/unlink";

    private final WebClient kakaoWebClient;
    private final String adminKey;

    public KakaoOAuthClient(WebClient kakaoWebClient, @Value("${kakao.admin-key}") String adminKey) {
        this.kakaoWebClient = kakaoWebClient;
        this.adminKey = adminKey;
        // 배포 환경에서 설정을 빠뜨리면 회원탈퇴가 카카오 연동은 풀지 않은 채 조용히 성공해버릴 수 있어,
        // 매 요청 로그(debug)와 별개로 기동 시점에 한 번 눈에 띄게 남긴다.
        if (adminKey == null || adminKey.isBlank()) {
            log.warn("kakao.admin-key가 설정되지 않았습니다. 회원탈퇴 시 카카오 연동 해제(unlink)가 동작하지 않습니다.");
        }
    }

    // 모바일 앱이 카카오 SDK로 로그인 후 전달한 액세스 토큰을 카카오 서버에 직접 검증한다.
    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        KakaoUserInfoResponse response;
        try {
            response = kakaoWebClient.get()
                    .uri(USER_INFO_URI)
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
        String email = response.kakaoAccount() == null ? null : response.kakaoAccount().email();
        return new KakaoUserInfo(String.valueOf(response.id()), email);
    }

    // 회원탈퇴 시 카카오 연동을 서버 대 서버(Admin Key)로 해제한다. 사용자의 access token을
    // 다시 받을 필요가 없어 탈퇴 API 계약이 단순해진다. Admin Key가 설정되지 않은 환경(로컬 등)에서는
    // no-op으로 건너뛴다.
    public void unlink(String snsId) {
        if (adminKey == null || adminKey.isBlank()) {
            log.debug("카카오 Admin Key가 설정되지 않아 연동 해제를 건너뜁니다.");
            return;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("target_id_type", "user_id");
            form.add("target_id", snsId);

            kakaoWebClient.post()
                    .uri(UNLINK_URI)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (RuntimeException e) {
            throw new AuthException(AuthErrorCode.KAKAO_UNLINK_FAILED);
        }
    }

    // 이메일은 카카오계정(이메일) 동의항목을 거부했거나 미인증 상태면 null일 수 있다.
    public record KakaoUserInfo(String snsId, String email) {
    }
}
