package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.notification.domain.DevicePlatform;
import com.nexters.palang.domain.notification.domain.DeviceToken;
import com.nexters.palang.domain.notification.infrastructure.DeviceTokenRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    // FCM 토큰은 기기 단위로 유일하다. 같은 토큰이 다른 사용자로 재등록되면(재설치/재로그인) 소유자를 교체한다.
    @Transactional
    public void registerOrRefresh(Long userId, String token, DevicePlatform platform) {
        DeviceToken existing = deviceTokenRepository.findByToken(token).orElse(null);
        if (existing != null) {
            User user = userRepository.getReferenceById(userId);
            existing.reassign(user, platform);
            return;
        }

        User user = userRepository.getReferenceById(userId);
        deviceTokenRepository.save(DeviceToken.builder().user(user).token(token).platform(platform).build());
    }

    @Transactional
    public void remove(Long userId, String token) {
        deviceTokenRepository.deleteByTokenAndUserId(token, userId);
    }
}
