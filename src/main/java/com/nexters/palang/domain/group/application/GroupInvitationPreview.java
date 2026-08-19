package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.domain.Group;

// 초대 링크 미리보기 조회용 뷰. alreadyJoined는 비로그인 요청이면 항상 false다.
public record GroupInvitationPreview(Group group, long memberCount, boolean alreadyJoined) {
}
