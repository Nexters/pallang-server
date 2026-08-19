package com.nexters.palang.domain.comment.domain.event;

public record CommentCreatedEvent(Long commentId, Long opinionId, Long opinionOwnerId, Long actorUserId) {
}
