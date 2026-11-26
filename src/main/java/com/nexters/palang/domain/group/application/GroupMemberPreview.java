package com.nexters.palang.domain.group.application;

// 모임 카드 아바타 하나. 현재 멤버 계정 ID당 한 번만 나온다(L06). 프로필 사진이 없으면 null이고, 앱이 기존 대체
// 이미지를 쓴다.
public record GroupMemberPreview(Long userId, String nickname, String profileImageUrl) {
}
