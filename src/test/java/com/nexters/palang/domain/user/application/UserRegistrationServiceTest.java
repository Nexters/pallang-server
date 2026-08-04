package com.nexters.palang.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NicknameGenerator nicknameGenerator;

    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        userRegistrationService = new UserRegistrationService(userRepository, nicknameGenerator);
    }

    @Test
    @DisplayName("닉네임이 충돌하지 않으면 처음 생성한 닉네임 그대로 저장된다")
    void registerViaSnsSucceedsOnFirstAttempt() {
        given(nicknameGenerator.generateBase()).willReturn("고요한책갈피");
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        User user = userRegistrationService.registerViaSns(SnsProvider.KAKAO, "sns-1", "user1@example.com", null);

        assertThat(user.getNickname()).isEqualTo("고요한책갈피");
        assertThat(user.getSnsProvider()).isEqualTo(SnsProvider.KAKAO);
        assertThat(user.getSnsId()).isEqualTo("sns-1");
        assertThat(user.getEmail()).isEqualTo("user1@example.com");
        verify(userRepository, times(1)).saveAndFlush(any());
    }

    @Test
    @DisplayName("닉네임이 충돌하면 숫자 접미사를 붙여 재시도한다")
    void registerViaSnsRetriesWithSuffixOnConflict() {
        given(nicknameGenerator.generateBase()).willReturn("고요한책갈피");
        given(nicknameGenerator.withSuffix("고요한책갈피", 1)).willReturn("고요한책갈피1");
        given(userRepository.saveAndFlush(argThat(u -> u != null && u.getNickname().equals("고요한책갈피"))))
                .willThrow(new DataIntegrityViolationException("Duplicate entry for key 'users.uq_users_nickname'"));
        given(userRepository.saveAndFlush(argThat(u -> u != null && u.getNickname().equals("고요한책갈피1"))))
                .willAnswer(invocation -> invocation.getArgument(0));

        User user = userRegistrationService.registerViaSns(SnsProvider.KAKAO, "sns-2", null, null);

        assertThat(user.getNickname()).isEqualTo("고요한책갈피1");
    }

    @Test
    @DisplayName("100번 재시도까지 모두 충돌하면 닉네임 생성 실패 예외가 발생한다")
    void registerViaSnsFailsWhenAllAttemptsConflict() {
        given(nicknameGenerator.generateBase()).willReturn("고요한책갈피");
        given(nicknameGenerator.withSuffix(eq("고요한책갈피"), anyInt()))
                .willAnswer(invocation -> "고요한책갈피" + invocation.<Integer>getArgument(1));
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("Duplicate entry for key 'users.uq_users_nickname'"));

        assertThatThrownBy(() -> userRegistrationService.registerViaSns(SnsProvider.KAKAO, "sns-3", null, null))
                .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("닉네임 충돌이 아닌 다른 무결성 위반은 재시도하지 않고 그대로 던진다")
    void registerViaSnsRethrowsNonNicknameViolation() {
        given(nicknameGenerator.generateBase()).willReturn("고요한책갈피");
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException(
                        "Column 'terms_agreed_at' cannot be null"));

        assertThatThrownBy(() -> userRegistrationService.registerViaSns(SnsProvider.KAKAO, "sns-4", null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(userRepository, times(1)).saveAndFlush(any());
    }
}
