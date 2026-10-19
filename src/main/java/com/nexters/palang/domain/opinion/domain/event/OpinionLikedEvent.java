package com.nexters.palang.domain.opinion.domain.event;

// groupId/groupName: 좋아요가 눌린 흔적이 모임 전용 대목에 달린 것이면 그 모임, 전역 공개 대목이면 null.
// likeCount/bookTitle/pageNumber: 알림 문구(Figma 기준 "회원님의 의견을 N명이 좋아해요" / "{책 제목} · {페이지}p")
// 구성에 필요한 값을 발행 시점에 그대로 실어 보낸다 — 리스너가 다시 조회하지 않도록.
public record OpinionLikedEvent(
        Long opinionId, Long opinionOwnerId, Long actorUserId, Long groupId, String groupName,
        int likeCount, String bookTitle, int pageNumber) {
}
