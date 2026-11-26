package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.domain.Group;

// 모임장이 공유할 초대 링크와, 공유 전에 정원 안내를 보여주기 위한 현재 인원.
public record GroupInviteLink(Group group, String inviteCode, long memberCount) {

    public boolean isFull() {
        return memberCount >= group.getCapacity();
    }
}
