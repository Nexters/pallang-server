package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupQueryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.group.presentation.dto.CreateGroupRequest;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    // 모임 생성 직후에는 host 1명만 참여 중임이 보장된다.
    private static final long INITIAL_MEMBER_COUNT = 1L;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupQueryRepository groupQueryRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupDetail createGroup(Long hostUserId, CreateGroupRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
        User host = userRepository.getReferenceById(hostUserId);

        Group group = Group.create(
                request.name(), book, host, request.capacity(), request.startDate(), request.endDate());
        groupRepository.save(group);
        groupMemberRepository.save(GroupMember.of(group, host, GroupMemberRole.HOST));

        return new GroupDetail(group, INITIAL_MEMBER_COUNT);
    }

    public Page<GroupSummaryProjection> getMyGroups(Long userId, Pageable pageable) {
        return groupQueryRepository.findMyGroups(userId, pageable);
    }

    public GroupDetail getGroupDetail(Long groupId, Long userId) {
        Group group = getExistingGroup(groupId);
        validateMember(groupId, userId);
        return new GroupDetail(group, groupMemberRepository.countByGroupId(groupId));
    }

    // joinGroup(초대 가입)과 같은 Group row 잠금을 사용한다 — 정원을 줄이는 것과 동시 가입이 겹치면
    // "참여 인원 > 정원"이 될 수 있어(TOCTOU), 두 경로 모두 같은 락으로 직렬화해야 한다.
    @Transactional
    public GroupDetail updateGroup(Long groupId, Long userId, UpdateGroupRequest request) {
        Group group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        validateHost(group, userId);
        long memberCount = groupMemberRepository.countByGroupId(groupId);
        group.updateSettings(request.name(), request.capacity(), request.startDate(), request.endDate(), memberCount);
        return new GroupDetail(group, memberCount);
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = getExistingGroup(groupId);
        validateHost(group, userId);
        groupMemberRepository.deleteAllByGroupId(groupId);
        groupRepository.delete(group);
    }

    public Page<GroupMember> getGroupMembers(Long groupId, Long userId, Pageable pageable) {
        getExistingGroup(groupId);
        validateMember(groupId, userId);
        return groupQueryRepository.findMembers(groupId, pageable);
    }

    public String getInviteLink(Long groupId, Long userId) {
        Group group = getExistingGroup(groupId);
        validateHost(group, userId);
        return group.getInviteCode();
    }

    @Transactional
    public String regenerateInviteLink(Long groupId, Long userId) {
        Group group = getExistingGroup(groupId);
        validateHost(group, userId);
        group.regenerateInviteCode();
        return group.getInviteCode();
    }

    // 초대 링크 미리보기: 비로그인 요청도 허용해야 하므로 userId는 nullable이다(Soft Authentication).
    public GroupInvitationPreview previewInvitation(String inviteCode, Long userId) {
        Group group = getExistingGroupByInviteCode(inviteCode);
        long memberCount = groupMemberRepository.countByGroupId(group.getId());
        boolean alreadyJoined = userId != null && groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId);
        return new GroupInvitationPreview(group, memberCount, alreadyJoined);
    }

    // 초대 링크로 가입. 정원 초과(TOCTOU)를 막기 위해 Group row에 비관적 락을 건 채로 인원 수를
    // 확인하고 GroupMember를 추가한다 (GroupRepository#findByInviteCodeForUpdate 참고).
    @Transactional
    public GroupDetail joinGroup(String inviteCode, Long userId) {
        Group group = groupRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new GroupException(GroupErrorCode.INVALID_INVITE_CODE));

        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
            throw new GroupException(GroupErrorCode.ALREADY_JOINED);
        }
        long memberCount = groupMemberRepository.countByGroupId(group.getId());
        if (memberCount >= group.getCapacity()) {
            throw new GroupException(GroupErrorCode.GROUP_FULL);
        }

        User user = userRepository.getReferenceById(userId);
        groupMemberRepository.save(GroupMember.of(group, user, GroupMemberRole.MEMBER));
        return new GroupDetail(group, memberCount + 1);
    }

    private Group getExistingGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
    }

    private Group getExistingGroupByInviteCode(String inviteCode) {
        return groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new GroupException(GroupErrorCode.INVALID_INVITE_CODE));
    }

    private void validateHost(Group group, Long userId) {
        if (!group.getHost().getId().equals(userId)) {
            throw new GroupException(GroupErrorCode.NOT_HOST);
        }
    }

    private void validateMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new GroupException(GroupErrorCode.NOT_MEMBER);
        }
    }
}
