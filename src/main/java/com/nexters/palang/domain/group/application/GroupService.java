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

    @Transactional
    public GroupDetail updateGroup(Long groupId, Long userId, UpdateGroupRequest request) {
        Group group = getExistingGroup(groupId);
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

    private Group getExistingGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
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
