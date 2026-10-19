package com.nexters.palang.domain.comment.domain.event;

// groupId/groupName: 댓글이 달린 흔적이 모임 전용 대목에 달린 것이면 그 모임, 전역 공개 대목이면 null.
// quotedText: 알림 본문(Figma 기준 "“{발췌 문장}”") 구성에 필요한 그 흔적의 발췌 문장.
public record CommentCreatedEvent(
        Long commentId, Long opinionId, Long opinionOwnerId, Long actorUserId, Long groupId, String groupName,
        String quotedText) {
}
