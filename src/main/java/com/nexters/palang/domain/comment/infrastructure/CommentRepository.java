package com.nexters.palang.domain.comment.infrastructure;

import com.nexters.palang.domain.comment.domain.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // open-in-view: false 환경에서 컨트롤러 단 Mapper가 comment.getUser()를 참조하므로
    // 트랜잭션 안에서 user를 미리 로딩해야 LazyInitializationException을 피할 수 있다.
    @Query("select c from Comment c join fetch c.user where c.id = :id")
    Optional<Comment> findByIdWithUser(@Param("id") Long id);

    // 관리자 유저 삭제(AdminUserService): 이 유저 본인의 댓글/답글 id 전부(삭제 순서 계산용).
    @Query("select c.id from Comment c where c.user.id = :userId")
    List<Long> findIdsByUserId(@Param("userId") Long userId);

    // 아래 세 메서드는 comments.parent_comment_id(자기참조 FK)를 위반하지 않도록 자식(답글)부터 지우기
    // 위한 것이다. 삭제 순서: ①이 유저의 댓글에 달린 답글(작성자 무관) → ②삭제 대상 의견에 달린 댓글/답글
    // 전부(한 쿼리 안에서 부모/자식이 함께 지워지므로 순서 문제가 없다) → ③이 유저의 나머지 댓글.
    void deleteAllByParentCommentIdIn(List<Long> parentCommentIds);

    void deleteAllByOpinionIdIn(List<Long> opinionIds);

    void deleteAllByUserId(Long userId);
}
