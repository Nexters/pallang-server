package com.nexters.palang.domain.notification.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.notification.application.NotificationPreferenceService;
import com.nexters.palang.domain.notification.presentation.dto.NotificationPreferenceResponse;
import com.nexters.palang.domain.notification.presentation.dto.UpdateNotificationPreferenceRequest;
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

@WebMvcTest(NotificationPreferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private NotificationPreferenceService notificationPreferenceService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    @DisplayName("알림 설정을 조회하면 활동/모임 수신 여부를 반환한다")
    void getSettings() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(notificationPreferenceService.getSettings(1L)).willReturn(new NotificationPreferenceResponse(false, true));

        mockMvc.perform(get("/api/notifications/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityEnabled").value(false))
                .andExpect(jsonPath("$.data.groupEnabled").value(true));
    }

    @Test
    @DisplayName("인증 없이 알림 설정을 조회하면 401 에러가 발생한다")
    void getSettingsFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(get("/api/notifications/settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("알림 설정을 변경하면 서비스에 위임한다")
    void updateSettings() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(put("/api/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNotificationPreferenceRequest(false, true))))
                .andExpect(status().isOk());

        verify(notificationPreferenceService).updateSettings(1L, false, true);
    }

    @Test
    @DisplayName("필수 값이 없으면 400 에러가 발생한다")
    void updateSettingsFailsWhenValueMissing() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(put("/api/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityEnabled\": true}"))
                .andExpect(status().isBadRequest());
    }
}
