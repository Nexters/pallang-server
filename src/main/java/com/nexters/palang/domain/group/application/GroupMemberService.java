package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRemoval;
import com.nexters.palang.domain.group.domain.event.GroupMemberRemovedEvent;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRemovalRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupQueryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.group.presentation.dto.RemoveGroupMemberRequest;
import com.nexters.palang.global.idempotency.IdempotentExecutor;
import com.nexters.palang.global.idempotency.IdempotentOperation;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 모임 멤버 명단과 내보내기(F03/F04).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupMemberService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMemberRemovalRepository groupMemberRemovalRepository;
    private final GroupQueryRepository groupQueryRepository;
    private final IdempotentExecutor idempotentExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // 현재 멤버 명단. 현재 멤버만 볼 수 있고, 모임장 → 참여일 → userId 순이다.
    public GroupMemberPage getGroupMembers(Long groupId, Long userId, Pageable pageable) {
        Group group = groupRepository.findById(groupId)
                .filter(Group::isActive)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        if (!groupMemberRepository.existsActiveMember(groupId, userId)) {
            throw new GroupException(GroupErrorCode.NOT_MEMBER);
        }
        return new GroupMemberPage(group, groupQueryRepository.findMembers(groupId, pageable), userId);
    }

    // 모임장이 멤버를 내보낸다(5.2). 실행 후 최신 명단을 돌려준다.
    // - 모임 행을 잠근 채 현재 모임장 권한과 대상의 현재 멤버십을 다시 확인한다(가입/정원 변경과 직렬화).
    // - 모임장 자신(=자기 자신)은 내보낼 수 없다. 모임장 변경은 지원하지 않는다.
    // - 대상이 이미 나갔거나 내보내졌으면 멤버십과 이력을 바꾸지 않고 최신 명단만 돌려준다.
    // - 성공하면 대상 멤버십을 REMOVED로 바꾸고(인원/명단/접근권이 함께 바뀜), 감사 이력을 남기고, 대상에게 참여 종료
    //   안내를 보낸다. 대상이 남긴 기록·개인 서재는 지우지 않는다.
    // 요청 키가 같으면 다시 실행하지 않는다. 트랜잭션은 IdempotentExecutor가 연다(GroupService.createGroup 참고).
    @Transactional(propagation = Propagation.SUPPORTS)
    public GroupMemberPage removeMember(Long groupId, Long targetUserId, Long actorUserId,
            RemoveGroupMemberRequest request, String requestKey, Pageable pageable) {
        idempotentExecutor.execute(actorUserId, IdempotentOperation.REMOVE_GROUP_MEMBER, requestKey,
                () -> remove(groupId, targetUserId, actorUserId, request, requestKey));
        return getGroupMembers(groupId, actorUserId, pageable);
    }

    private Long remove(Long groupId, Long targetUserId, Long actorUserId, RemoveGroupMemberRequest request,
            String requestKey) {
        Group group = groupRepository.findByIdForUpdate(groupId)
                .filter(Group::isActive)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        if (!group.isHost(actorUserId)) {
            throw new GroupException(GroupErrorCode.NOT_HOST);
        }
        if (group.isHost(targetUserId)) {
            throw new GroupException(GroupErrorCode.CANNOT_REMOVE_HOST);
        }
        // 사유 검증은 대상 상태와 무관하게 먼저 한다 — 잘못된 입력은 언제나 400이다.
        GroupMemberRemoval removal = GroupMemberRemoval.record(
                groupId, actorUserId, targetUserId, request.reasonCode(), request.otherText(), requestKey);
        // 모임 행을 잠근 뒤 잠금 읽기로 최신 커밋 상태를 본다 — 일반 조회는 요청 키 확인 시점의 스냅샷을 읽어, 잠금을
        // 기다리는 사이 다른 요청이 먼저 내보낸 것을 못 보고 이력/알림을 중복으로 만들 수 있다(MySQL REPEATABLE READ).
        GroupMember membership = groupMemberRepository.findByGroupIdAndUserIdForUpdate(groupId, targetUserId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_MEMBER_NOT_FOUND));
        if (!membership.isActive()) {
            return membership.getId();
        }
        membership.remove(LocalDateTime.now(clock));
        groupMemberRemovalRepository.save(removal);
        eventPublisher.publishEvent(new GroupMemberRemovedEvent(groupId, group.getName(), targetUserId));
        return membership.getId();
    }
}
