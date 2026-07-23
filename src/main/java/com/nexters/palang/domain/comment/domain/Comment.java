package com.nexters.palang.domain.comment.domain;

import com.nexters.palang.domain.common.BaseEntity;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comments_opinion", columnList = "opinion_id"),
                @Index(name = "idx_comments_parent", columnList = "parent_comment_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opinion_id", nullable = false)
    private Opinion opinion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 대댓글에는 답글 불가(DB 트리거로 강제) - 부모가 이미 대댓글이면 안 됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(name = "content", length = 1000, nullable = false)
    private String content;

    @Builder
    private Comment(Opinion opinion, User user, Comment parentComment, String content) {
        this.opinion = opinion;
        this.user = user;
        this.parentComment = parentComment;
        this.content = content;
    }
}
