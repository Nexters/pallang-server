package com.nexters.palang.domain.user.infrastructure;

import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    Optional<User> findBySnsProviderAndSnsId(SnsProvider snsProvider, String snsId);

    // 관리자 페이지 유저 검색: 닉네임 또는 이메일에 키워드가 포함된 유저(관리자 화면이라 소프트 삭제
    // 여부와 무관하게 전부 대상으로 한다).
    Page<User> findByNicknameContainingOrEmailContaining(String nickname, String email, Pageable pageable);
}
