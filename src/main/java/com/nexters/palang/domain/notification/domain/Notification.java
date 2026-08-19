package com.nexters.palang.domain.notification.domain;

import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_receiver", columnList = "receiver_id, created_at"),
                // BookNewOpinionsNotifier가 (receiver, book) 기준 가장 최근 알림의 snapshot을 조회하는 데 사용.
                @Index(name = "idx_notifications_receiver_book", columnList = "receiver_id, book_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    // 알림 리스트는 조인 없이 바로 렌더링할 수 있도록 생성 시점에 title/body를 스냅샷으로 저장한다.
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false)
    private String body;

    // 알림을 유발한 사용자(좋아요/댓글 작성자). BOOK_NEW_OPINIONS는 특정 한 명이 아니라 null.
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "opinion_id")
    private Long opinionId;

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "comment_id")
    private Long commentId;

    // BOOK_NEW_OPINIONS 전용: 알림 생성 시점의 책 전체(살아있는) 의견 수. 다음 알림 발송 여부 판단 기준값.
    @Column(name = "opinion_count_snapshot")
    private Integer opinionCountSnapshot;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    private Notification(
            User receiver,
            NotificationType type,
            String title,
            String body,
            Long actorId,
            Long opinionId,
            Long bookId,
            Long commentId,
            Integer opinionCountSnapshot
    ) {
        this.receiver = receiver;
        this.type = type;
        this.title = title;
        this.body = body;
        this.actorId = actorId;
        this.opinionId = opinionId;
        this.bookId = bookId;
        this.commentId = commentId;
        this.opinionCountSnapshot = opinionCountSnapshot;
        this.read = false;
    }

    public void markAsRead() {
        if (this.read) {
            return;
        }
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
