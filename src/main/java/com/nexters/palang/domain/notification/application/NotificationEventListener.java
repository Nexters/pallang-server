package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.comment.domain.event.CommentCreatedEvent;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.opinion.domain.event.OpinionCreatedEvent;
import com.nexters.palang.domain.opinion.domain.event.OpinionLikedEvent;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 좋아요/댓글 API 응답 지연을 막기 위해 커밋 이후 비동기로 처리한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationCreationService notificationCreationService;
    private final BookNewOpinionsNotifier bookNewOpinionsNotifier;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOpinionLiked(OpinionLikedEvent event) {
        User actor = userRepository.findById(event.actorUserId()).orElse(null);
        if (actor == null) {
            log.warn("좋아요 알림 생성 스킵: actor {}를 찾을 수 없음", event.actorUserId());
            return;
        }

        String title = "새로운 좋아요";
        String body = "%s님이 회원님의 흔적을 좋아합니다.".formatted(actor.getNickname());
        notificationCreationService.create(event.opinionOwnerId(), event.actorUserId(), NotificationType.OPINION_LIKED,
                title, body, event.opinionId(), null, null, null);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        User actor = userRepository.findById(event.actorUserId()).orElse(null);
        if (actor == null) {
            log.warn("댓글 알림 생성 스킵: actor {}를 찾을 수 없음", event.actorUserId());
            return;
        }

        String title = "새로운 댓글";
        String body = "%s님이 회원님의 흔적에 댓글을 남겼습니다.".formatted(actor.getNickname());
        notificationCreationService.create(
                event.opinionOwnerId(), event.actorUserId(), NotificationType.OPINION_COMMENTED,
                title, body, event.opinionId(), null, event.commentId(), null);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOpinionCreated(OpinionCreatedEvent event) {
        bookNewOpinionsNotifier.notifyIfThresholdReached(event.bookId(), event.actorUserId());
    }
}
