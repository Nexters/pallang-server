package com.nexters.palang.domain.group.domain.event;

// 가입자 본인에게는 GROUP_JOINED, 호스트에게는 GROUP_MEMBER_JOINED 알림을 만드는 데 쓰인다.
public record GroupJoinedEvent(Long groupId, String groupName, Long newMemberUserId, Long hostUserId) {
}
