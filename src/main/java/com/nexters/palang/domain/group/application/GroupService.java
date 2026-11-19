package com.nexters.palang.domain.group.application;

import com.nexters.palang.domain.book.common.error.BookErrorCode;
import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupInvitation;
import com.nexters.palang.domain.group.domain.GroupInvitationStatus;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.nexters.palang.domain.group.domain.event.GroupJoinedEvent;
import com.nexters.palang.domain.group.domain.event.GroupScheduleChangedEvent;
import com.nexters.palang.domain.group.infrastructure.GroupBookEntryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupInvitationRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupQueryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.group.presentation.dto.CreateGroupRequest;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupRequest;
import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.idempotency.IdempotentExecutor;
import com.nexters.palang.global.idempotency.IdempotentOperation;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    // 모임 생성 직후에는 host 1명만 참여 중임이 보장된다.
    private static final long INITIAL_MEMBER_COUNT = 1L;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupBookEntryRepository groupBookEntryRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupQueryRepository groupQueryRepository;
    private final GroupBookEntries groupBookEntries;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final IdempotentExecutor idempotentExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // 모임, 생성자 멤버십, 첫 모임 책, 초대 링크를 한 트랜잭션(IdempotentExecutor)으로 저장한다 — 하나라도
    // 실패하면 전부 미반영(C05). requestKey가 있으면 같은 키의 재요청은 새 모임을 만들지 않고 처음 만든 모임을
    // 돌려준다(C07). 같은 키가 동시에 들어왔을 때 진 쪽이 새 트랜잭션에서 결과를 다시 조회하려면 IdempotentExecutor가
    // 트랜잭션을 직접 열고 닫아야 하므로, 여기서는 트랜잭션을 새로 열지 않는다(SUPPORTS: 클래스 기본값인 readOnly
    // 트랜잭션을 피하고, 이미 바깥 트랜잭션이 있으면 그대로 참여).
    @Transactional(propagation = Propagation.SUPPORTS)
    public GroupDetail createGroup(Long hostUserId, CreateGroupRequest request, String requestKey) {
        Long groupId = idempotentExecutor.execute(
                hostUserId, IdempotentOperation.CREATE_GROUP, requestKey, () -> insertGroup(hostUserId, request));
        Group group = getExistingGroupWithHost(groupId);
        return toDetail(group, groupMemberRepository.countActiveMembers(groupId));
    }

    private Long insertGroup(Long hostUserId, CreateGroupRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
        // 탈퇴 등 경합을 고려해 인증된 사용자라도 존재 검증을 한다.
        User host = userRepository.findById(hostUserId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        ReadingPeriod period = ReadingPeriod.of(request.startDate(), request.endDate());

        Group group = Group.create(request.name(), host, request.capacity());
        GroupBookEntry entry = GroupBookEntry.create(group, book, period, today());
        groupRepository.save(group);
        groupMemberRepository.save(GroupMember.join(group, host, GroupMemberRole.HOST, now()));
        groupBookEntryRepository.save(entry);
        groupInvitationRepository.save(GroupInvitation.issue(group, hostUserId));
        return group.getId();
    }

    public Page<GroupSummaryProjection> getMyGroups(Long userId, Pageable pageable) {
        return groupQueryRepository.findMyGroups(userId, today(), pageable);
    }

    public GroupDetail getGroupDetail(Long groupId, Long userId) {
        Group group = getExistingGroupWithHost(groupId);
        validateMember(groupId, userId);
        return toDetail(group, groupMemberRepository.countActiveMembers(groupId));
    }

    // joinGroup(초대 가입)과 같은 Group row 잠금을 사용한다 — 정원을 줄이는 것과 동시 가입이 겹치면
    // "참여 인원 > 정원"이 될 수 있어(TOCTOU), 두 경로 모두 같은 락으로 직렬화해야 한다.
    @Transactional
    public GroupDetail updateGroup(Long groupId, Long userId, UpdateGroupRequest request) {
        Group group = groupRepository.findByIdForUpdate(groupId)
                .filter(Group::isActive)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        validateHost(group, userId);
        long memberCount = groupMemberRepository.countActiveMembers(groupId);
        GroupBookEntry primaryEntry = groupBookEntries.getPrimaryEntry(groupId);

        // 기간 검증까지 모두 통과한 뒤에 엔티티를 바꾼다 — 한 필드라도 잘못되면 아무것도 반영하지 않는다(S01).
        Optional<ReadingPeriod> requestedPeriod = requestedPeriod(request);
        requestedPeriod.ifPresent(period -> period.validateChangedDatesNotPast(primaryEntry.getPeriod(), today()));
        group.updateSettings(request.name(), request.capacity(), memberCount);

        boolean scheduleChanged = requestedPeriod.filter(period -> !period.equals(primaryEntry.getPeriod())).isPresent();
        requestedPeriod.ifPresent(period -> primaryEntry.changePeriod(period, today()));
        if (scheduleChanged) {
            eventPublisher.publishEvent(new GroupScheduleChangedEvent(group.getId(), group.getName(), userId));
        }
        return new GroupDetail(group, primaryEntry, memberCount, isFinished(primaryEntry));
    }

    // 기존 앱 호환: 기간을 둘 다 비우면 "기간은 바꾸지 않음"이다(UpdateGroupRequest 참고).
    private Optional<ReadingPeriod> requestedPeriod(UpdateGroupRequest request) {
        if (request.startDate() == null && request.endDate() == null) {
            return Optional.empty();
        }
        return Optional.of(ReadingPeriod.of(request.startDate(), request.endDate()));
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = getExistingGroup(groupId);
        validateHost(group, userId);
        groupInvitationRepository.deleteAllByGroupIdIn(List.of(groupId));
        groupBookEntryRepository.deleteAllByGroupIdIn(List.of(groupId));
        groupMemberRepository.deleteAllByGroupId(groupId);
        groupRepository.delete(group);
    }

    public Page<GroupMember> getGroupMembers(Long groupId, Long userId, Pageable pageable) {
        getExistingGroup(groupId);
        validateMember(groupId, userId);
        return groupQueryRepository.findMembers(groupId, pageable);
    }

    // 모임장이 공유할 현재 유효 초대 코드. 유효한 초대가 하나도 없으면(예: 합치기로 기존 초대가 종료된 경우)
    // 새로 발급한다 — 그래서 읽기 전용 트랜잭션이 아니다.
    @Transactional
    public String getInviteLink(Long groupId, Long userId) {
        Group group = getExistingActiveGroup(groupId);
        validateHost(group, userId);
        return groupInvitationRepository
                .findFirstByGroupIdAndStatusOrderByIdDesc(groupId, GroupInvitationStatus.ACTIVE)
                .orElseGet(() -> groupInvitationRepository.save(GroupInvitation.issue(group, userId)))
                .getCode();
    }

    // 초대 링크 재발급(host 전용): 기존 유효 초대는 즉시 종료되고, 그 코드로 들어온 이후 요청은 전부
    // INVALID_INVITE_CODE로 거절된다.
    @Transactional
    public String regenerateInviteLink(Long groupId, Long userId) {
        Group group = getExistingActiveGroup(groupId);
        validateHost(group, userId);
        LocalDateTime now = now();
        groupInvitationRepository.findAllByGroupIdAndStatus(groupId, GroupInvitationStatus.ACTIVE)
                .forEach(invitation -> invitation.revoke(now));
        return groupInvitationRepository.save(GroupInvitation.issue(group, userId)).getCode();
    }

    // 초대 링크 미리보기: 비로그인 요청도 허용해야 하므로 userId는 nullable이다(Soft Authentication).
    public GroupInvitationPreview previewInvitation(String inviteCode, Long userId) {
        Group group = getValidInvitation(inviteCode).getGroup();
        long memberCount = groupMemberRepository.countActiveMembers(group.getId());
        boolean alreadyJoined = userId != null && groupMemberRepository.existsActiveMember(group.getId(), userId);
        return new GroupInvitationPreview(
                group, groupBookEntries.getPrimaryEntry(group.getId()), memberCount, alreadyJoined);
    }

    // 초대 링크로 가입. 정원 초과(TOCTOU)를 막기 위해 Group row에 비관적 락을 건 채로 인원 수를
    // 확인하고 멤버십을 추가한다(GroupRepository#findByIdForUpdate 참고). 락을 잡은 뒤 초대/모임 상태를 한 번 더
    // 확인해, 그 사이 재발급·합치기로 초대가 종료됐으면 거절한다.
    @Transactional
    public GroupDetail joinGroup(String inviteCode, Long userId) {
        Long groupId = getValidInvitation(inviteCode).getGroup().getId();
        Group group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.INVALID_INVITE_CODE));
        getValidInvitation(inviteCode);

        Optional<GroupMember> existingMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
        if (existingMembership.filter(GroupMember::isActive).isPresent()) {
            throw new GroupException(GroupErrorCode.ALREADY_JOINED);
        }
        long memberCount = groupMemberRepository.countActiveMembers(groupId);
        if (memberCount >= group.getCapacity()) {
            throw new GroupException(GroupErrorCode.GROUP_FULL);
        }

        // 탈퇴/내보내기 이력이 있으면 (group_id, user_id) 유니크 제약 때문에 같은 행을 되살린다.
        // 내보내진 계정의 재가입 제한(removedAt 이전 초대 거절)은 #226에서 추가한다.
        existingMembership.ifPresentOrElse(
                membership -> membership.rejoin(now()),
                () -> groupMemberRepository.save(
                        GroupMember.join(group, userRepository.getReferenceById(userId), GroupMemberRole.MEMBER, now())));
        eventPublisher.publishEvent(
                new GroupJoinedEvent(group.getId(), group.getName(), userId, group.getHost().getId()));
        return toDetail(group, memberCount + 1);
    }

    private GroupDetail toDetail(Group group, long memberCount) {
        GroupBookEntry primaryEntry = groupBookEntries.getPrimaryEntry(group.getId());
        return new GroupDetail(group, primaryEntry, memberCount, isFinished(primaryEntry));
    }

    private boolean isFinished(GroupBookEntry entry) {
        return entry.currentStatus(today()) == ReadingStatus.FINISHED;
    }

    private GroupInvitation getValidInvitation(String inviteCode) {
        return groupInvitationRepository.findWithGroupByCode(inviteCode)
                .filter(invitation -> invitation.isActive() && invitation.getGroup().isActive())
                .orElseThrow(() -> new GroupException(GroupErrorCode.INVALID_INVITE_CODE));
    }

    private Group getExistingGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
    }

    private Group getExistingActiveGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .filter(Group::isActive)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
    }

    private Group getExistingGroupWithHost(Long groupId) {
        return groupRepository.findByIdWithHost(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
    }

    private void validateHost(Group group, Long userId) {
        if (!group.isHost(userId)) {
            throw new GroupException(GroupErrorCode.NOT_HOST);
        }
    }

    // 탈퇴/내보내진 계정은 ACTIVE가 아니므로 여기서 걸러진다.
    private void validateMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsActiveMember(groupId, userId)) {
            throw new GroupException(GroupErrorCode.NOT_MEMBER);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
