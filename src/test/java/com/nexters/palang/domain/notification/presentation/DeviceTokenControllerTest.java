package com.nexters.palang.domain.notification.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.notification.application.DeviceTokenService;
import com.nexters.palang.domain.notification.domain.DevicePlatform;
import com.nexters.palang.domain.notification.presentation.dto.RegisterDeviceTokenRequest;
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

@WebMvcTest(DeviceTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeviceTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private DeviceTokenService deviceTokenService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    @DisplayName("디바이스 토큰을 등록하면 서비스에 위임한다")
    void registerDeviceToken() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(put("/api/notifications/device-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterDeviceTokenRequest("token-a", DevicePlatform.ANDROID))))
                .andExpect(status().isOk());

        verify(deviceTokenService).registerOrRefresh(1L, "token-a", DevicePlatform.ANDROID);
    }

    @Test
    @DisplayName("토큰 없이 등록을 요청하면 400 에러가 발생한다")
    void registerDeviceTokenFailsWhenTokenBlank() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(put("/api/notifications/device-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterDeviceTokenRequest(" ", DevicePlatform.ANDROID))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("인증 없이 등록을 요청하면 401 에러가 발생한다")
    void registerDeviceTokenFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(put("/api/notifications/device-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterDeviceTokenRequest("token-a", DevicePlatform.ANDROID))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("디바이스 토큰을 삭제하면 서비스에 위임한다")
    void removeDeviceToken() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(delete("/api/notifications/device-tokens").param("token", "token-a"))
                .andExpect(status().isOk());

        verify(deviceTokenService).remove(eq(1L), eq("token-a"));
    }
}
