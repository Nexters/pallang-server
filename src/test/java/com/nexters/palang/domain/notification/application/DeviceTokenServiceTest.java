package com.nexters.palang.domain.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.notification.domain.DevicePlatform;
import com.nexters.palang.domain.notification.domain.DeviceToken;
import com.nexters.palang.domain.notification.infrastructure.DeviceTokenRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    private DeviceTokenService deviceTokenService;

    @BeforeEach
    void setUp() {
        deviceTokenService = new DeviceTokenService(deviceTokenRepository, userRepository);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("처음 등록하는 토큰이면 새 DeviceToken을 저장한다")
    void registerOrRefreshSavesNewTokenWhenNotExists() {
        given(deviceTokenRepository.findByToken("token-a")).willReturn(Optional.empty());
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));

        deviceTokenService.registerOrRefresh(1L, "token-a", DevicePlatform.ANDROID);

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("token-a");
        assertThat(captor.getValue().getPlatform()).isEqualTo(DevicePlatform.ANDROID);
    }

    @Test
    @DisplayName("이미 등록된 토큰이면 저장 대신 소유자/플랫폼을 갱신한다")
    void registerOrRefreshReassignsExistingToken() {
        User previousOwner = user(1L);
        DeviceToken existing = DeviceToken.builder().user(previousOwner).token("token-a").platform(DevicePlatform.IOS).build();
        given(deviceTokenRepository.findByToken("token-a")).willReturn(Optional.of(existing));
        given(userRepository.getReferenceById(2L)).willReturn(user(2L));

        deviceTokenService.registerOrRefresh(2L, "token-a", DevicePlatform.ANDROID);

        assertThat(existing.getUser().getId()).isEqualTo(2L);
        assertThat(existing.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        verify(deviceTokenRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("토큰 삭제를 요청하면 본인 소유 토큰만 삭제한다")
    void removeDelegatesToRepository() {
        deviceTokenService.remove(1L, "token-a");

        verify(deviceTokenRepository).deleteByTokenAndUserId("token-a", 1L);
    }
}
