package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 페이지에서 모임(Group)을 조회/수정/삭제하기 위한 서비스(issue #155 후속). GroupService의
// updateGroup/deleteGroup은 호스트 본인만 호출할 수 있고(validateHost), deleteGroup은 그 모임의
// 대목/의견/댓글까지는 지우지 않아(멤버십만 정리) 관리자의 "완전히 정리" 요구에는 맞지 않는다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminGroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AdminCascadeDeleter cascadeDeleter;

    public Page<AdminGroupSearchResult> searchGroups(String keyword, Pageable pageable) {
        Page<Group> groups = groupRepository.findByNameContaining(keyword, pageable);
        return groups.map(group -> new AdminGroupSearchResult(group, groupMemberRepository.countByGroupId(group.getId())));
    }

    // Group.updateSettings는 GroupService.updateGroup(호스트 전용)에서 쓰는 것과 같은 엔티티 메서드다.
    // 정원을 현재 참여 인원보다 적게 줄이려 하면 그대로 GroupException(CAPACITY_BELOW_MEMBER_COUNT, 409)이
    // 던져진다. 책은 모임 생성 후 바꿀 수 없어(updatable=false) 여기서도 수정 대상이 아니다.
    @Transactional
    public AdminGroupSearchResult updateGroup(Long groupId, String name, int capacity, LocalDate startDate, LocalDate endDate) {
        Group group = getExistingGroup(groupId);
        long memberCount = groupMemberRepository.countByGroupId(groupId);
        group.updateSettings(name, capacity, startDate, endDate, memberCount);
        return new AdminGroupSearchResult(group, memberCount);
    }

    // 모임과 그 안의 대목/의견/댓글/데코/좋아요, 모임원까지 전부 하드 삭제한다. 되돌릴 수 없다.
    @Transactional
    public void deleteGroup(Long groupId) {
        getExistingGroup(groupId);
        cascadeDeleter.deleteGroups(List.of(groupId));
    }

    private Group getExistingGroup(Long groupId) {
        return groupRepository.findWithAssociationsById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
    }
}
