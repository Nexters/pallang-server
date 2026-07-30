package com.nexters.palang.domain.comment.infrastructure;

import com.nexters.palang.domain.comment.domain.Comment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // open-in-view: false 환경에서 컨트롤러 단 Mapper가 comment.getUser()를 참조하므로
    // 트랜잭션 안에서 user를 미리 로딩해야 LazyInitializationException을 피할 수 있다.
    @Query("select c from Comment c join fetch c.user where c.id = :id")
    Optional<Comment> findByIdWithUser(@Param("id") Long id);
}
