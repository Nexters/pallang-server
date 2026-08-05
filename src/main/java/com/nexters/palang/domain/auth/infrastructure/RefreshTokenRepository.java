package com.nexters.palang.domain.auth.infrastructure;

import com.nexters.palang.domain.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUserIdAndTokenHashAndRevokedFalse(Long userId, String tokenHash);

    Optional<RefreshToken> findByUserIdAndTokenHash(Long userId, String tokenHash);

    // 회원탈퇴 시 해당 유저의 모든 세션을 무효화하기 위해 개별 조회 없이 일괄 revoke한다.
    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.userId = :userId and r.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);
}
