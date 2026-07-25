package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.domain.UserBookStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBookStatusRepository extends JpaRepository<UserBookStatus, Long> {

    Optional<UserBookStatus> findByUserIdAndBookId(Long userId, Long bookId);
}
