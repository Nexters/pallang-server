package com.nexters.palang.domain.opinion.domain.event;

public record OpinionLikedEvent(Long opinionId, Long opinionOwnerId, Long actorUserId) {
}
