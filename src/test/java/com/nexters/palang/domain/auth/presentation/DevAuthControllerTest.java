package com.nexters.palang.domain.auth.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.auth.application.AuthResult;
import com.nexters.palang.domain.auth.application.AuthService;
import com.nexters.palang.domain.auth.presentation.dto.DevLoginRequest;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// DevAuthController는 @Profile("local")이라 local 프로파일을 활성화해야 빈이 등록된다.
@WebMvcTest(DevAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
class DevAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("userId 없이 요청하면 새 테스트 유저로 로그인 처리한다")
    void devLoginWithoutUserId() throws Exception {
        given(authService.devLogin(isNull()))
                .willReturn(new AuthResult("access", "refresh", true, false, false));

        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DevLoginRequest(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.isNewUser").value(true));
    }

    @Test
    @DisplayName("본문 없이 요청해도 새 테스트 유저로 로그인 처리한다")
    void devLoginWithoutBody() throws Exception {
        given(authService.devLogin(isNull()))
                .willReturn(new AuthResult("access", "refresh", true, false, false));

        mockMvc.perform(post("/api/auth/dev-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }

    @Test
    @DisplayName("userId를 주면 해당 유저로 로그인 처리한다")
    void devLoginWithUserId() throws Exception {
        given(authService.devLogin(eq(7L)))
                .willReturn(new AuthResult("access-7", "refresh-7", false, true, true));

        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DevLoginRequest(7L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-7"))
                .andExpect(jsonPath("$.data.isNewUser").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 userId면 404 에러가 발생한다")
    void devLoginFailsWhenUserNotFound() throws Exception {
        given(authService.devLogin(eq(999L))).willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DevLoginRequest(999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("USER_404_1"));
    }
}
