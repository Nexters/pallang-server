package com.nexters.palang.domain.user.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.auth.application.AuthService;
import com.nexters.palang.domain.opinion.application.LikedOpinionProjection;
import com.nexters.palang.domain.opinion.application.MyOpinionProjection;
import com.nexters.palang.domain.opinion.application.OpinionService;
import com.nexters.palang.domain.user.application.UserService;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.presentation.dto.UpdateBackgroundColorRequest;
import com.nexters.palang.domain.user.presentation.dto.UpdateNicknameRequest;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private OpinionService opinionService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    private User user(Long id) {
        User user = User.builder()
                .nickname("닉네임")
                .email("user@example.com")
                .profileImageUrl("cover")
                .backgroundColor("#FFFFFF")
                .snsProvider(SnsProvider.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("내 프로필을 조회하면 흔적 수와 함께 반환한다")
    void getMe() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(userService.getMe(1L)).willReturn(user(1L));
        given(opinionService.getMyOpinionCount(1L)).willReturn(5L);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.nickname").value("닉네임"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.opinionCount").value(5));
    }

    @Test
    @DisplayName("인증 없이 내 프로필을 조회하면 401 에러가 발생한다")
    void getMeFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 조회하면 404 에러가 발생한다")
    void getMeFailsWhenNotFound() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(999L);
        given(userService.getMe(999L)).willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("USER_404_1"));
    }

    @Test
    @DisplayName("닉네임을 변경하면 변경된 프로필을 반환한다")
    void modifyNickname() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(userService.modifyNickname(eq(1L), eq("새닉네임"))).willReturn(user(1L));
        given(opinionService.getMyOpinionCount(1L)).willReturn(0L);

        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNicknameRequest("새닉네임"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    @DisplayName("빈 닉네임으로 변경을 요청하면 400 에러가 발생한다")
    void modifyNicknameFailsWhenBlank() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNicknameRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("오늘 이미 변경했다면 400 에러가 발생한다")
    void modifyNicknameFailsWhenLimited() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(userService.modifyNickname(eq(1L), any()))
                .willThrow(new UserException(UserErrorCode.NICKNAME_CHANGE_LIMITED));

        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNicknameRequest("새닉네임"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("USER_400_1"));
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 409 에러가 발생한다")
    void modifyNicknameFailsWhenAlreadyInUse() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(userService.modifyNickname(eq(1L), any()))
                .willThrow(new UserException(UserErrorCode.NICKNAME_ALREADY_IN_USE));

        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNicknameRequest("중복닉네임"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("USER_409_1"));
    }

    @Test
    @DisplayName("배경색을 변경하면 변경된 프로필을 반환한다")
    void modifyBackgroundColor() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(userService.modifyBackgroundColor(eq(1L), eq("#000000"))).willReturn(user(1L));
        given(opinionService.getMyOpinionCount(1L)).willReturn(0L);

        mockMvc.perform(patch("/api/users/me/background-color")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBackgroundColorRequest("#000000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    @DisplayName("빈 배경색으로 변경을 요청하면 400 에러가 발생한다")
    void modifyBackgroundColorFailsWhenBlank() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(patch("/api/users/me/background-color")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBackgroundColorRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("프로필 이미지를 변경하면 변경된 프로필을 반환한다")
    void modifyProfileImage() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(userService.modifyProfileImage(eq(1L), any())).willReturn(user(1L));
        given(opinionService.getMyOpinionCount(1L)).willReturn(0L);
        MockMultipartFile image = new MockMultipartFile("image", "profile.png", "image/png", "data".getBytes());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me/profile-image").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    @DisplayName("이미지가 아닌 파일로 프로필 이미지를 변경하려 하면 400 에러가 발생한다")
    void modifyProfileImageFailsWhenNotImage() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(userService.modifyProfileImage(eq(1L), any()))
                .willThrow(new UserException(UserErrorCode.INVALID_IMAGE_FILE));
        MockMultipartFile file = new MockMultipartFile("image", "profile.txt", "text/plain", "data".getBytes());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me/profile-image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("USER_400_2"));
    }

    @Test
    @DisplayName("회원 탈퇴를 요청하면 서비스에 위임한다")
    void withdraw() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isOk());

        verify(authService).withdraw(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 탈퇴를 요청하면 404 에러가 발생한다")
    void withdrawFailsWhenNotFound() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        doThrow(new UserException(UserErrorCode.USER_NOT_FOUND)).when(authService).withdraw(1L);

        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("USER_404_1"));
    }

    @Test
    @DisplayName("온보딩 완료를 요청하면 완료 상태를 반환한다")
    void completeOnboarding() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(patch("/api/users/me/onboarding-complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasCompletedOnboarding").value(true));

        verify(userService).completeOnboarding(1L);
    }

    @Test
    @DisplayName("인증 없이 온보딩 완료를 요청하면 401 에러가 발생한다")
    void completeOnboardingFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(patch("/api/users/me/onboarding-complete"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("내가 남긴 흔적 목록을 조회한다")
    void getMyOpinions() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        MyOpinionProjection projection = new MyOpinionProjection(
                1L, 10L, "책 제목", "cover", 100L, "발췌 문장", 5, "흔적 내용", 0, LocalDateTime.now());
        given(opinionService.getMyOpinions(eq(1L), any())).willReturn(
                new PageImpl<>(List.of(projection), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/users/me/opinions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinions[0].opinionId").value(1))
                .andExpect(jsonPath("$.data.opinions[0].bookTitle").value("책 제목"));
    }

    @Test
    @DisplayName("좋아요 누른 흔적 목록을 조회한다")
    void getLikedOpinions() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        LikedOpinionProjection projection = new LikedOpinionProjection(
                1L, 10L, "책 제목", "cover", 100L, "발췌 문장", 5, "흔적 내용", 0,
                LocalDateTime.now(), LocalDateTime.now());
        given(opinionService.getLikedOpinions(eq(1L), any())).willReturn(
                new PageImpl<>(List.of(projection), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/users/me/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinions[0].opinionId").value(1));
    }
}
