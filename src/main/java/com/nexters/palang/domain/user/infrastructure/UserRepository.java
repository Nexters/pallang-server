package com.nexters.palang.domain.user.infrastructure;

import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    Optional<User> findBySnsProviderAndSnsId(SnsProvider snsProvider, String snsId);
}
