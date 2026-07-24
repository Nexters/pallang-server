package com.nexters.palang.domain.comment.domain;

import com.nexters.palang.domain.comment.common.NestedReplyNotAllowedException;
import com.nexters.palang.global.common.entity.BaseEntity;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.NoArgsConstructor;

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

    public static final int CONTENT_MAX_LENGTH = 500;
    public static final int CONTENT_COLUMN_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opinion_id", nullable = false)
    private Opinion opinion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 1-depth 제약: 생성 시점에 정적 팩토리(root/reply)가 차단하므로 대댓글의 parentComment는 항상 원댓글이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(name = "content", length = CONTENT_COLUMN_LENGTH, nullable = false)
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Comment(Opinion opinion, User user, Comment parentComment, String content) {
        this.opinion = opinion;
        this.user = user;
        this.parentComment = parentComment;
        this.content = content;
    }

    public static Comment root(Opinion opinion, User user, String content) {
        return new Comment(opinion, user, null, content);
    }

    public static Comment reply(Comment parent, User user, String content) {
        if (parent.isReply()) {
            throw new NestedReplyNotAllowedException();
        }
        return new Comment(parent.opinion, user, parent, content);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    private boolean isReply() {
        return parentComment != null;
    }
}
