package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.domain.UserBookStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBookStatusRepository extends JpaRepository<UserBookStatus, Long> {

    Optional<UserBookStatus> findByUserIdAndBookId(Long userId, Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);

    // 관리자 유저 삭제(AdminUserService): 이 유저의 읽기 상태 전부.
    void deleteAllByUserId(Long userId);

    // 관리자 도서 삭제(AdminBookService): 이 책에 대한 읽기 상태 전부.
    void deleteAllByBookId(Long bookId);
}
