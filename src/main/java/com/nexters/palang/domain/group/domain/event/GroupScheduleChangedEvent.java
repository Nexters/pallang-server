package com.nexters.palang.domain.group.domain.event;

// 시작일/종료일이 실제로 바뀐 경우에만 발행된다(GroupService.updateGroup 참고). 변경한 호스트
// 본인을 제외한 멤버 전원에게 GROUP_SCHEDULE_CHANGED 알림을 만드는 데 쓰인다.
public record GroupScheduleChangedEvent(Long groupId, String groupName, Long actorUserId) {
}
