package com.nexters.palang.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.storage.FileStorageService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, fileStorageService);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("존재하는 사용자를 조회하면 그대로 반환한다")
    void getMeReturnsUser() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        User result = userService.getMe(1L);

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 조회하면 예외가 발생한다")
    void getMeFailsWhenNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(999L)).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("탈퇴한 사용자를 조회하면 예외가 발생한다")
    void getMeFailsWhenWithdrawn() {
        User user = user(1L);
        user.withdraw();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getMe(1L)).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("중복되지 않은 닉네임으로 변경하면 반영된다")
    void modifyNicknameSucceeds() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndIdNot("새닉네임", 1L)).willReturn(false);

        User result = userService.modifyNickname(1L, "새닉네임");

        assertThat(result.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 변경하려 하면 예외가 발생한다")
    void modifyNicknameFailsWhenAlreadyInUse() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndIdNot("중복닉네임", 1L)).willReturn(true);

        assertThatThrownBy(() -> userService.modifyNickname(1L, "중복닉네임")).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("오늘 이미 닉네임을 변경했다면 다시 변경하려 할 때 예외가 발생한다")
    void modifyNicknameFailsWhenChangedTwiceOnSameDay() {
        User user = user(1L);
        user.changeNickname("첫변경");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndIdNot("두번째변경", 1L)).willReturn(false);

        assertThatThrownBy(() -> userService.modifyNickname(1L, "두번째변경")).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 닉네임을 변경하려 하면 예외가 발생한다")
    void modifyNicknameFailsWhenUserNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.modifyNickname(999L, "새닉네임")).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("배경색을 변경하면 반영된다")
    void modifyBackgroundColorSucceeds() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        User result = userService.modifyBackgroundColor(1L, "#000000");

        assertThat(result.getBackgroundColor()).isEqualTo("#000000");
    }

    @Test
    @DisplayName("프로필 이미지를 업로드하면 저장소 URL로 반영된다")
    void modifyProfileImageSucceeds() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        MockMultipartFile image = new MockMultipartFile("image", "profile.png", "image/png", "data".getBytes());
        given(fileStorageService.store(image, "profile-images"))
                .willReturn("https://storage.example.com/profile-images/uuid.png");

        User result = userService.modifyProfileImage(1L, image);

        assertThat(result.getProfileImageUrl()).isEqualTo("https://storage.example.com/profile-images/uuid.png");
    }

    @Test
    @DisplayName("이미지가 아닌 파일로 프로필 이미지를 변경하려 하면 예외가 발생한다")
    void modifyProfileImageFailsWhenNotImage() {
        MockMultipartFile file = new MockMultipartFile("image", "profile.txt", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> userService.modifyProfileImage(1L, file)).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("회원 탈퇴를 요청하면 소프트 삭제된다")
    void withdrawSoftDeletesUser() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.withdraw(1L);

        assertThat(user.isWithdrawn()).isTrue();
    }

    @Test
    @DisplayName("이미 탈퇴한 사용자를 다시 탈퇴시키려 하면 예외가 발생한다")
    void withdrawFailsWhenAlreadyWithdrawn() {
        User user = user(1L);
        user.withdraw();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.withdraw(1L)).isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("온보딩 완료를 요청하면 반영된다")
    void completeOnboardingMarksUserCompleted() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.completeOnboarding(1L);

        assertThat(user.isHasCompletedOnboarding()).isTrue();
    }
}
