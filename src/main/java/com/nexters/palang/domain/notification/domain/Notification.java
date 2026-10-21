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
                // 더 이상 조회에 쓰이지 않지만(이슈 #193으로 BOOK_NEW_OPINIONS 트리거 제거), 기존 인덱스를
                // DB에서 임의로 지우지 않기 위해 엔티티 선언은 그대로 둔다.
                @Index(name = "idx_notifications_receiver_book", columnList = "receiver_id, book_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    public static final int REMOVED_CONTENT_EXCERPT_MAX_LENGTH = 100;
    public static final String CONTENT_REMOVED_TITLE = "작성하신 게시물이 삭제되었습니다";

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

    // 모임 전용 알림(또는 모임 전용 흔적에 달린 알림)이면 그 모임, 전역 알림이면 null.
    // 조인 없이 바로 렌더링하도록 title/body와 같은 방식으로 생성 시점에 스냅샷한다.
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_name")
    private String groupName;

    // BOOK_NEW_OPINIONS 전용: 알림 생성 시점의 책 전체(살아있는) 의견 수. 다음 알림 발송 여부 판단 기준값.
    @Column(name = "opinion_count_snapshot")
    private Integer opinionCountSnapshot;

    // CONTENT_REMOVED 전용: 삭제된 게시물을 작성자가 알아볼 수 있도록 남기는 식별 정보 스냅샷.
    // 원본 의견/대목은 삭제되므로 여기 보관된 값이 유일한 기록이다. 다른 알림 타입에서는 전부 null.
    // 사유는 enum 이름을 문자열로 저장한다. enum 타입으로 매핑하면 Hibernate가 MySQL ENUM이나 값 목록 CHECK 제약을
    // 만들어 사유가 늘 때마다 운영 DB를 수동 ALTER해야 한다(ddl-auto: update는 이를 갱신하지 않는다).
    // 바깥에서는 getRemovalReason()으로 enum으로만 다룬다.
    @Column(name = "removal_reason", length = 40)
    private String removalReason;

    @Column(name = "removed_book_title")
    private String removedBookTitle;

    @Column(name = "removed_written_at")
    private LocalDateTime removedWrittenAt;

    @Column(name = "removed_content_excerpt", length = REMOVED_CONTENT_EXCERPT_MAX_LENGTH)
    private String removedContentExcerpt;

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
            Long groupId,
            String groupName,
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
        this.groupId = groupId;
        this.groupName = groupName;
        this.opinionCountSnapshot = opinionCountSnapshot;
        this.read = false;
    }

    // 운영 안내(게시물 삭제) 알림. 모임 게시물이어도 groupId를 두지 않는다 — 운영 안내는 "전체"와 "운영" 탭에만
    // 보여야 해서(NotificationScope 참고). 사유가 내용 미리보기를 허용하지 않으면 내용 일부를 보관하지 않는다.
    public static Notification contentRemoved(
            User receiver, RemovalReason reason, String bookTitle, LocalDateTime writtenAt, String content) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(NotificationType.CONTENT_REMOVED)
                .title(CONTENT_REMOVED_TITLE)
                .body(reason.getMessage())
                .build();
        notification.removalReason = reason.name();
        notification.removedBookTitle = bookTitle;
        notification.removedWrittenAt = writtenAt;
        notification.removedContentExcerpt = reason.isShowsContentPreview() ? excerpt(content) : null;
        return notification;
    }

    public RemovalReason getRemovalReason() {
        return removalReason != null ? RemovalReason.valueOf(removalReason) : null;
    }

    private static String excerpt(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.strip();
        return trimmed.length() > REMOVED_CONTENT_EXCERPT_MAX_LENGTH
                ? trimmed.substring(0, REMOVED_CONTENT_EXCERPT_MAX_LENGTH - 1) + "…"
                : trimmed;
    }

    public void markAsRead() {
        if (this.read) {
            return;
        }
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    // OPINION_LIKED 전용: 좋아요는 Figma 기준 "N명이 좋아해요"로 집계 표시하므로, 아직 안 읽은 좋아요
    // 알림이 있으면 새로 만들지 않고 이 알림의 제목(인원수)/행위자만 최신 값으로 덮어쓴다.
    public void refreshLikeAggregation(Long actorId, String title, String body) {
        this.actorId = actorId;
        this.title = title;
        this.body = body;
    }
}
