package com.nexters.palang.domain.opinion.domain.event;

// groupId/groupName: 좋아요가 눌린 흔적이 모임 전용 대목에 달린 것이면 그 모임, 전역 공개 대목이면 null.
public record OpinionLikedEvent(Long opinionId, Long opinionOwnerId, Long actorUserId, Long groupId, String groupName) {
}
