package com.nexters.palang.domain.opinion.domain.event;

public record OpinionCreatedEvent(Long opinionId, Long bookId, Long actorUserId) {
}
