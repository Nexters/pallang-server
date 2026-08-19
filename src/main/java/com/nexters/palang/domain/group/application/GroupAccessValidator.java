package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 모임 스코프 리소스(모임 자체는 물론, 모임에 속한 흔적/의견 등)에 대한 "이 사용자가 모임원인가" 검증을
// 한 곳에 모은다. group 도메인 밖(passage/opinion 등)에서도 재사용하므로 별도 컴포넌트로 뺐다.
@Component
@RequiredArgsConstructor
public class GroupAccessValidator {

    private final GroupMemberRepository groupMemberRepository;

    // userId가 null이면(비로그인) 애초에 모임원일 수 없으므로 곧바로 거절한다(Soft Authentication 경로에서
    // groupId가 있는 리소스에 접근할 때 유용 — 로그인 여부와 무관하게 "모임원 아님" 하나로 처리).
    public void validateMember(Long groupId, Long userId) {
        if (userId == null || !groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new GroupException(GroupErrorCode.NOT_MEMBER);
        }
    }
}
