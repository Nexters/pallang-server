package com.nexters.palang.domain.notification.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.notification.application.NotificationService;
import com.nexters.palang.domain.notification.common.error.NotificationErrorCode;
import com.nexters.palang.domain.notification.common.error.NotificationException;
import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Notification notification(Long id) {
        Notification notification = Notification.builder()
                .receiver(user(1L))
                .type(NotificationType.OPINION_LIKED)
                .title("새로운 좋아요").body("책읽는고양이님이 회원님의 흔적을 좋아합니다.")
                .opinionId(3L)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    @Test
    @DisplayName("알림 목록을 조회하면 알림을 반환한다")
    void getNotifications() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(notificationService.getNotifications(eq(1L), any())).willReturn(
                new PageImpl<>(List.of(notification(1L)), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications[0].notificationId").value(1))
                .andExpect(jsonPath("$.data.notifications[0].type").value("OPINION_LIKED"));
    }

    @Test
    @DisplayName("인증 없이 알림 목록을 조회하면 401 에러가 발생한다")
    void getNotificationsFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("알림을 단건 읽음 처리하면 서비스에 위임한다")
    void readNotification() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(1L, 1L);
    }

    @Test
    @DisplayName("본인이 받은 알림이 아니면 읽음 처리 시 404 에러가 발생한다")
    void readNotificationFailsWhenNotOwner() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        org.mockito.Mockito.doThrow(new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND))
                .when(notificationService).markAsRead(1L, 1L);

        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("NOTIFICATION_404_1"));
    }

    @Test
    @DisplayName("전체 읽음 처리를 요청하면 서비스에 위임한다")
    void readAllNotifications() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk());

        verify(notificationService).markAllAsRead(1L);
    }
}
