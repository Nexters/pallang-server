package com.nexters.palang.domain.notification.application;

import com.nexters.palang.domain.comment.domain.event.CommentCreatedEvent;
import com.nexters.palang.domain.group.domain.event.GroupJoinedEvent;
import com.nexters.palang.domain.group.domain.event.GroupScheduleChangedEvent;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.notification.domain.NotificationType;
import com.nexters.palang.domain.opinion.domain.event.OpinionLikedEvent;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 좋아요/댓글/모임 가입·일정변경 API 응답 지연을 막기 위해 커밋 이후 비동기로 처리한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationCreationService notificationCreationService;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOpinionLiked(OpinionLikedEvent event) {
        User actor = userRepository.findById(event.actorUserId()).orElse(null);
        if (actor == null) {
            log.warn("좋아요 알림 생성 스킵: actor {}를 찾을 수 없음", event.actorUserId());
            return;
        }

        // Figma 기준: 좋아요가 눌릴 때마다 새 알림을 만들지 않고 "N명이 좋아해요"로 집계한다.
        String title = "회원님의 의견을 %d명이 좋아해요".formatted(event.likeCount());
        String body = "%s · %dp".formatted(event.bookTitle(), event.pageNumber());
        notificationCreationService.createOrUpdateOpinionLiked(
                event.opinionOwnerId(), event.actorUserId(), title, body,
                event.opinionId(), event.groupId(), event.groupName());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        User actor = userRepository.findById(event.actorUserId()).orElse(null);
        if (actor == null) {
            log.warn("댓글 알림 생성 스킵: actor {}를 찾을 수 없음", event.actorUserId());
            return;
        }

        // Figma 기준: 제목에는 행위자 닉네임을 넣지 않고, 본문에 발췌 문장을 그대로 보여준다.
        String title = "회원님의 의견에 댓글이 달렸어요";
        String body = "“%s”".formatted(event.quotedText());
        notificationCreationService.create(
                event.opinionOwnerId(), event.actorUserId(), NotificationType.OPINION_COMMENTED,
                title, body, event.opinionId(), null, event.commentId(), event.groupId(), event.groupName(), null);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupJoined(GroupJoinedEvent event) {
        String joinedBody = "'%s' 모임에 가입했어요.".formatted(event.groupName());
        notificationCreationService.create(event.newMemberUserId(), null, NotificationType.GROUP_JOINED,
                "모임 가입 완료", joinedBody, null, null, null, event.groupId(), event.groupName(), null);

        User newMember = userRepository.findById(event.newMemberUserId()).orElse(null);
        if (newMember == null) {
            log.warn("모임 가입 알림(호스트) 생성 스킵: 가입자 {}를 찾을 수 없음", event.newMemberUserId());
            return;
        }
        String memberJoinedBody = "%s님이 '%s' 모임에 가입했어요.".formatted(newMember.getNickname(), event.groupName());
        notificationCreationService.create(event.hostUserId(), event.newMemberUserId(), NotificationType.GROUP_MEMBER_JOINED,
                "새 멤버 가입", memberJoinedBody, null, null, null, event.groupId(), event.groupName(), null);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupScheduleChanged(GroupScheduleChangedEvent event) {
        List<Long> memberIds = groupMemberRepository.findUserIdsByGroupId(event.groupId());
        String title = "모임 일정 변경";
        String body = "'%s' 모임의 일정이 변경됐어요.".formatted(event.groupName());
        for (Long memberId : memberIds) {
            if (memberId.equals(event.actorUserId())) {
                continue;
            }
            notificationCreationService.create(memberId, event.actorUserId(), NotificationType.GROUP_SCHEDULE_CHANGED,
                    title, body, null, null, null, event.groupId(), event.groupName(), null);
        }
    }
}
