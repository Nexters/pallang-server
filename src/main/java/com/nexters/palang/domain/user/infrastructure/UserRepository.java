package com.nexters.palang.domain.user.infrastructure;

import com.nexters.palang.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNicknameAndIdNot(String nickname, Long id);
}
