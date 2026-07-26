package com.nexters.palang.domain.auth.infrastructure;

import com.nexters.palang.domain.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUserIdAndTokenHashAndRevokedFalse(Long userId, String tokenHash);

    Optional<RefreshToken> findByUserIdAndTokenHash(Long userId, String tokenHash);
}
