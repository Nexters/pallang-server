package com.nexters.palang.domain.notification.infrastructure;

import com.nexters.palang.domain.notification.domain.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findAllByUserId(Long userId);

    void deleteByTokenAndUserId(String token, Long userId);
}
