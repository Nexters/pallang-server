package com.nexters.palang.domain.group.domain.event;

// 모임장이 멤버를 내보냈을 때 발행된다. 내보내진 사람에게 "모임 참여가 종료되었다"는 사실만 알리는 데 쓰며,
// 사유는 담지 않는다(대상에게 사유를 공개하지 않음, 5.2).
public record GroupMemberRemovedEvent(Long groupId, String groupName, Long removedUserId) {
}
