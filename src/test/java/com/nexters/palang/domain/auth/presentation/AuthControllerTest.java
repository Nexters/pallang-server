package com.nexters.palang.domain.auth.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.auth.application.AuthResult;
import com.nexters.palang.domain.auth.application.AuthService;
import com.nexters.palang.domain.auth.presentation.dto.KakaoLoginRequest;
import com.nexters.palang.domain.auth.presentation.dto.RefreshTokenRequest;
import com.nexters.palang.global.security.AuthErrorCode;
import com.nexters.palang.global.security.AuthException;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    @DisplayName("카카오 액세스 토큰으로 로그인하면 서비스 자체 JWT를 발급받는다")
    void loginWithKakao() throws Exception {
        given(authService.loginWithKakao("kakao-token"))
                .willReturn(new AuthResult("access", "refresh", true, false, false));

        mockMvc.perform(post("/api/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KakaoLoginRequest("kakao-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.data.isNewUser").value(true));
    }

    @Test
    @DisplayName("카카오 액세스 토큰 없이 로그인을 요청하면 400 에러가 발생한다")
    void loginWithKakaoFailsWhenTokenBlank() throws Exception {
        mockMvc.perform(post("/api/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KakaoLoginRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("카카오 인증에 실패하면 401 에러가 발생한다")
    void loginWithKakaoFailsWhenKakaoAuthFails() throws Exception {
        given(authService.loginWithKakao("invalid-token"))
                .willThrow(new AuthException(AuthErrorCode.KAKAO_AUTH_FAILED));

        mockMvc.perform(post("/api/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KakaoLoginRequest("invalid-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_4"));
    }

    @Test
    @DisplayName("로그인한 사용자가 약관에 동의하면 성공한다")
    void agreeToTerms() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/auth/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.termsAgreed").value(true));

        verify(authService).agreeToTerms(1L);
    }

    @Test
    @DisplayName("인증 없이 약관 동의를 요청하면 401 에러가 발생한다")
    void agreeToTermsFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(post("/api/auth/terms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("리프레시 토큰으로 재발급하면 새 토큰 쌍을 반환한다")
    void refresh() throws Exception {
        given(authService.refresh("old-refresh"))
                .willReturn(new AuthResult("new-access", "new-refresh", false, true, true));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("old-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급을 요청하면 401 에러가 발생한다")
    void refreshFailsWhenTokenInvalid() throws Exception {
        given(authService.refresh("invalid"))
                .willThrow(new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("invalid"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_5"));
    }

    @Test
    @DisplayName("로그인한 사용자가 로그아웃하면 서비스에 위임한다")
    void logout() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("refresh"))))
                .andExpect(status().isOk());

        verify(authService).logout(eq(1L), eq("refresh"));
    }

    @Test
    @DisplayName("인증 없이 로그아웃을 요청하면 401 에러가 발생한다")
    void logoutFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("refresh"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }
}
