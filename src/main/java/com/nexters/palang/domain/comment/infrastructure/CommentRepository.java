package com.nexters.palang.domain.comment.infrastructure;

import com.nexters.palang.domain.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
