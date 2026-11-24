package com.nexters.palang.domain.group.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.GroupInvitation;
import com.nexters.palang.domain.group.domain.GroupInvitationStatus;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.domain.GroupMemberStatus;
import com.nexters.palang.domain.group.domain.GroupStatus;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.group.domain.event.GroupJoinedEvent;
import com.nexters.palang.domain.group.domain.event.GroupScheduleChangedEvent;
import com.nexters.palang.domain.group.infrastructure.GroupBookEntryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupInvitationRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRemovalRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupQueryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.group.presentation.dto.CreateGroupRequest;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.idempotency.IdempotentExecutor;
import com.nexters.palang.global.idempotency.IdempotentOperation;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    // 한국 시간 2026-11-19 00:30 — UTC로는 아직 11-18이라, "오늘"을 서울 기준으로 계산하는지도 함께 확인된다.
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 19, 0, 30, 0, 0, SEOUL).toInstant(), SEOUL);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupBookEntryRepository groupBookEntryRepository;
    @Mock
    private GroupInvitationRepository groupInvitationRepository;
    @Mock
    private GroupMemberRemovalRepository groupMemberRemovalRepository;
    @Mock
    private GroupQueryRepository groupQueryRepository;
    @Mock
    private GroupBookEntries groupBookEntries;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IdempotentExecutor idempotentExecutor;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GroupService groupService;

    private Book book;
    private User host;
    private User other;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(
                groupRepository, groupMemberRepository, groupBookEntryRepository, groupInvitationRepository,
                groupMemberRemovalRepository, groupQueryRepository, groupBookEntries, bookRepository, userRepository, idempotentExecutor,
                eventPublisher, CLOCK);
        book = book(1L);
        host = user(1L);
        other = user(2L);
    }

    private Book book(Long id) {
        Book built = Book.builder()
                .title("채식주의자").author("한강").publisher("창비").pageCount(268).source(BookSource.API).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Group group(Long id, User host) {
        Group built = Group.create("고전 뽀개기", host, 4);
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private GroupBookEntry entry(Group group, ReadingPeriod period) {
        // 과거 기간도 "이미 저장된 값"으로 만들 수 있게 검증 기준일을 아주 이른 날짜로 둔다.
        return GroupBookEntry.create(group, book, period, LocalDate.MIN);
    }

    private CreateGroupRequest createRequest(LocalDate startDate, LocalDate endDate) {
        return new CreateGroupRequest("고전 뽀개기", book.getId(), 4, startDate, endDate);
    }

    private GroupInvitation invitation(Group group) {
        return GroupInvitation.issue(group, group.getHost().getId());
    }

    // IdempotentExecutor는 트랜잭션/요청 키 처리만 담당하므로(별도 테스트), 여기서는 넘겨받은 작업을 그대로 실행한다.
    @SuppressWarnings("unchecked")
    private void runIdempotentActionDirectly() {
        given(idempotentExecutor.execute(anyLong(), eq(IdempotentOperation.CREATE_GROUP), any(), any()))
                .willAnswer(invocation -> ((Supplier<Long>) invocation.getArgument(3)).get());
    }

    // ===== 모임 생성 =====

    @Test
    @DisplayName("모임을 생성하면 모임, 모임장 멤버십, 첫 모임 책, 초대 링크가 함께 저장된다")
    void createGroupSavesGroupMembershipEntryAndInvitation() {
        runIdempotentActionDirectly();
        given(bookRepository.findById(book.getId())).willReturn(Optional.of(book));
        given(userRepository.findById(host.getId())).willReturn(Optional.of(host));
        List<Group> savedGroups = new ArrayList<>();
        List<GroupBookEntry> savedEntries = new ArrayList<>();
        given(groupRepository.save(any(Group.class))).willAnswer(invocation -> {
            Group saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            savedGroups.add(saved);
            return saved;
        });
        given(groupBookEntryRepository.save(any(GroupBookEntry.class))).willAnswer(invocation -> {
            savedEntries.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        given(groupRepository.findByIdWithHost(10L)).willAnswer(invocation -> Optional.of(savedGroups.getFirst()));
        given(groupBookEntries.getPrimaryEntry(10L)).willAnswer(invocation -> savedEntries.getFirst());
        given(groupMemberRepository.countActiveMembers(10L)).willReturn(1L);

        GroupDetail detail = groupService.createGroup(
                host.getId(), createRequest(TODAY, TODAY.plusDays(30)), "request-key-1");

        assertThat(detail.group().getHost()).isEqualTo(host);
        assertThat(detail.memberCount()).isEqualTo(1L);
        assertThat(detail.primaryEntry().getBook()).isEqualTo(book);
        assertThat(detail.primaryEntry().getPeriod()).isEqualTo(ReadingPeriod.of(TODAY, TODAY.plusDays(30)));
        verify(groupMemberRepository).save(any(GroupMember.class));
        verify(groupInvitationRepository).save(any(GroupInvitation.class));
        verify(idempotentExecutor).execute(eq(host.getId()), eq(IdempotentOperation.CREATE_GROUP), eq("request-key-1"), any());
    }

    @Test
    @DisplayName("독서 기간 없이도 모임을 만들 수 있다")
    void createGroupWithoutPeriod() {
        runIdempotentActionDirectly();
        given(bookRepository.findById(book.getId())).willReturn(Optional.of(book));
        given(userRepository.findById(host.getId())).willReturn(Optional.of(host));
        List<GroupBookEntry> savedEntries = new ArrayList<>();
        given(groupRepository.save(any(Group.class))).willAnswer(invocation -> {
            Group saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });
        given(groupBookEntryRepository.save(any(GroupBookEntry.class))).willAnswer(invocation -> {
            savedEntries.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        given(groupRepository.findByIdWithHost(10L)).willReturn(Optional.of(group(10L, host)));
        given(groupBookEntries.getPrimaryEntry(10L)).willAnswer(invocation -> savedEntries.getFirst());

        GroupDetail detail = groupService.createGroup(host.getId(), createRequest(null, null), null);

        assertThat(detail.primaryEntry().getPeriod().isEmpty()).isTrue();
        assertThat(detail.ended()).isFalse();
    }

    @Test
    @DisplayName("오늘(한국 시간) 이전 날짜로 모임을 만들면 예외가 발생하고 아무것도 저장되지 않는다")
    void createGroupFailsWithPastDate() {
        runIdempotentActionDirectly();
        given(bookRepository.findById(book.getId())).willReturn(Optional.of(book));
        given(userRepository.findById(host.getId())).willReturn(Optional.of(host));

        assertThatThrownBy(() -> groupService.createGroup(
                host.getId(), createRequest(TODAY.minusDays(1), TODAY.plusDays(30)), null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.PAST_GROUP_DATE);
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("시작일만 입력하고 종료일을 비우면 예외가 발생한다")
    void createGroupFailsWithHalfPeriod() {
        runIdempotentActionDirectly();
        given(bookRepository.findById(book.getId())).willReturn(Optional.of(book));
        given(userRepository.findById(host.getId())).willReturn(Optional.of(host));

        assertThatThrownBy(() -> groupService.createGroup(host.getId(), createRequest(TODAY, null), null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INCOMPLETE_GROUP_PERIOD);
    }

    @Test
    @DisplayName("존재하지 않는 책으로 모임을 만들면 예외가 발생한다")
    void createGroupFailsWhenBookNotFound() {
        runIdempotentActionDirectly();
        given(bookRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(
                host.getId(), new CreateGroupRequest("고전 뽀개기", 999L, 4, null, null), null))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("같은 요청 키로 다시 요청하면 새로 만들지 않고 처음 만든 모임을 돌려준다")
    void createGroupReturnsExistingGroupForSameRequestKey() {
        Group existing = group(10L, host);
        given(idempotentExecutor.execute(eq(host.getId()), eq(IdempotentOperation.CREATE_GROUP), eq("same-key"), any()))
                .willReturn(10L);
        given(groupRepository.findByIdWithHost(10L)).willReturn(Optional.of(existing));
        given(groupBookEntries.getPrimaryEntry(10L)).willReturn(entry(existing, ReadingPeriod.NONE));
        given(groupMemberRepository.countActiveMembers(10L)).willReturn(1L);

        GroupDetail detail = groupService.createGroup(host.getId(), createRequest(null, null), "same-key");

        assertThat(detail.group()).isEqualTo(existing);
        verify(groupRepository, never()).save(any());
    }

    // ===== 모임 상세 =====

    @Test
    @DisplayName("존재하지 않는 모임을 조회하면 예외가 발생한다")
    void getGroupDetailFailsWhenNotFound() {
        given(groupRepository.findByIdWithHost(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupDetail(999L, host.getId())).isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("현재 멤버가 아니면 모임 상세를 조회할 수 없다")
    void getGroupDetailFailsWhenNotMember() {
        given(groupRepository.findByIdWithHost(1L)).willReturn(Optional.of(group(1L, host)));
        given(groupMemberRepository.existsActiveMember(1L, other.getId())).willReturn(false);

        assertThatThrownBy(() -> groupService.getGroupDetail(1L, other.getId())).isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("모임원이면 모임 상세, 대표 책, 참여 인원 수를 함께 조회한다")
    void getGroupDetailSucceeds() {
        Group group = group(1L, host);
        GroupBookEntry primaryEntry = entry(group, ReadingPeriod.of(TODAY, TODAY.plusDays(3)));
        given(groupRepository.findByIdWithHost(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsActiveMember(1L, host.getId())).willReturn(true);
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(3L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(primaryEntry);

        GroupDetail detail = groupService.getGroupDetail(1L, host.getId());

        assertThat(detail.group()).isEqualTo(group);
        assertThat(detail.primaryEntry()).isEqualTo(primaryEntry);
        assertThat(detail.memberCount()).isEqualTo(3L);
        assertThat(detail.ended()).isFalse();
    }

    @Test
    @DisplayName("대표 책의 종료일이 어제(한국 시간)였으면 ended가 true다")
    void getGroupDetailEndedWhenPrimaryBookEndedYesterday() {
        Group group = group(1L, host);
        given(groupRepository.findByIdWithHost(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsActiveMember(1L, host.getId())).willReturn(true);
        given(groupBookEntries.getPrimaryEntry(1L))
                .willReturn(entry(group, ReadingPeriod.of(TODAY.minusDays(10), TODAY.minusDays(1))));

        assertThat(groupService.getGroupDetail(1L, host.getId()).ended()).isTrue();
    }

    // ===== 방 설정 변경 =====

    @Test
    @DisplayName("모임장이 아니면 방 설정을 변경할 수 없다")
    void updateGroupFailsWhenNotHost() {
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group(1L, host)));

        assertThatThrownBy(() -> groupService.updateGroup(
                1L, other.getId(), new UpdateGroupRequest("주말 독서 모임", 6, null, null, null)))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.NOT_HOST);
    }

    @Test
    @DisplayName("합쳐진 모임은 설정을 변경할 수 없다")
    void updateGroupFailsWhenGroupMerged() {
        Group merged = group(1L, host);
        ReflectionTestUtils.setField(merged, "status", GroupStatus.MERGED);
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(merged));

        assertThatThrownBy(() -> groupService.updateGroup(
                1L, host.getId(), new UpdateGroupRequest("주말 독서 모임", 6, null, null, null)))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("현재 참여 인원보다 적은 인원으로 방 설정을 변경하면 예외가 발생한다")
    void updateGroupFailsWhenCapacityBelowMemberCount() {
        Group group = group(1L, host);
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(3L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, ReadingPeriod.NONE));

        assertThatThrownBy(() -> groupService.updateGroup(
                1L, host.getId(), new UpdateGroupRequest("고전 뽀개기", 2, null, null, null)))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.CAPACITY_BELOW_MEMBER_COUNT);
    }

    @Test
    @DisplayName("모임장은 이름/인원을 바꾸고, 기간을 보내면 대표 책 기간이 바뀌며 일정 변경 이벤트가 발행된다")
    void updateGroupSucceeds() {
        Group group = group(1L, host);
        GroupBookEntry primaryEntry = entry(group, ReadingPeriod.of(TODAY, TODAY.plusDays(10)));
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(1L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(primaryEntry);

        GroupDetail detail = groupService.updateGroup(
                1L, host.getId(), new UpdateGroupRequest("주말 독서 모임", 6, TODAY.plusDays(1), TODAY.plusDays(20), null));

        assertThat(detail.group().getName()).isEqualTo("주말 독서 모임");
        assertThat(detail.group().getCapacity()).isEqualTo(6);
        assertThat(primaryEntry.getPeriod()).isEqualTo(ReadingPeriod.of(TODAY.plusDays(1), TODAY.plusDays(20)));
        verify(eventPublisher).publishEvent(new GroupScheduleChangedEvent(1L, "주말 독서 모임", host.getId()));
    }

    @Test
    @DisplayName("기간을 비워 보내면 기간은 그대로 두고 일정 변경 이벤트도 발행하지 않는다")
    void updateGroupKeepsPeriodWhenDatesOmitted() {
        Group group = group(1L, host);
        GroupBookEntry primaryEntry = entry(group, ReadingPeriod.of(TODAY, TODAY.plusDays(10)));
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(1L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(primaryEntry);

        groupService.updateGroup(1L, host.getId(), new UpdateGroupRequest("주말 독서 모임", 6, null, null, null));

        assertThat(primaryEntry.getPeriod()).isEqualTo(ReadingPeriod.of(TODAY, TODAY.plusDays(10)));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("기존 과거 기간을 그대로 보내면 날짜 검증에 걸리지 않고 이벤트도 발행하지 않는다")
    void updateGroupAllowsUnchangedPastPeriod() {
        Group group = group(1L, host);
        ReadingPeriod pastPeriod = ReadingPeriod.of(TODAY.minusDays(30), TODAY.minusDays(1));
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(1L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, pastPeriod));

        groupService.updateGroup(1L, host.getId(),
                new UpdateGroupRequest("주말 독서 모임", 6, pastPeriod.startDate(), pastPeriod.endDate(), null));

        assertThat(group.getName()).isEqualTo("주말 독서 모임");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("기간을 과거 날짜로 바꾸려 하면 예외가 발생하고 이름/인원도 바뀌지 않는다")
    void updateGroupChangesNothingWhenNewDateIsPast() {
        Group group = group(1L, host);
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(1L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, ReadingPeriod.of(TODAY, TODAY.plusDays(10))));

        assertThatThrownBy(() -> groupService.updateGroup(1L, host.getId(),
                new UpdateGroupRequest("주말 독서 모임", 6, TODAY.minusDays(3), TODAY.plusDays(10), null)))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.PAST_GROUP_DATE);

        assertThat(group.getName()).isEqualTo("고전 뽀개기");
        assertThat(group.getCapacity()).isEqualTo(4);
    }

    @Test
    @DisplayName("설정 화면을 연 뒤 다른 곳에서 설정이 바뀌었으면(version 불일치) 저장하지 않고 예외가 발생한다")
    void updateGroupFailsWhenVersionMismatch() {
        Group group = group(1L, host);
        ReflectionTestUtils.setField(group, "version", 3L);
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.updateGroup(
                1L, host.getId(), new UpdateGroupRequest("주말 독서 모임", 6, null, null, 2L)))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.GROUP_SETTINGS_CONFLICT);
        assertThat(group.getName()).isEqualTo("고전 뽀개기");
    }

    @Test
    @DisplayName("받은 version이 현재와 같으면 설정을 저장한다")
    void updateGroupSucceedsWhenVersionMatches() {
        Group group = group(1L, host);
        ReflectionTestUtils.setField(group, "version", 3L);
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(1L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, ReadingPeriod.NONE));

        groupService.updateGroup(1L, host.getId(), new UpdateGroupRequest("주말 독서 모임", 6, null, null, 3L));

        assertThat(group.getName()).isEqualTo("주말 독서 모임");
    }

    @Test
    @DisplayName("방 설정을 변경할 때는 동시 가입과의 정원 race를 막기 위해 잠금 조회를 사용한다")
    void updateGroupUsesLockedLookup() {
        Group group = group(1L, host);
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(1L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, ReadingPeriod.NONE));

        groupService.updateGroup(1L, host.getId(), new UpdateGroupRequest("주말 독서 모임", 6, null, null, null));

        verify(groupRepository).findByIdForUpdate(1L);
        verify(groupRepository, never()).findById(any());
    }

    // ===== 모임 삭제 =====

    @Test
    @DisplayName("모임장이 아니면 모임을 삭제할 수 없다")
    void deleteGroupFailsWhenNotHost() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.deleteGroup(1L, other.getId())).isInstanceOf(GroupException.class);
        verify(groupRepository, never()).delete(any(Group.class));
    }

    @Test
    @DisplayName("모임장은 모임을 삭제할 수 있고, 초대/모임 책/모임원 레코드도 함께 삭제된다")
    void deleteGroupSucceeds() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));

        groupService.deleteGroup(1L, host.getId());

        verify(groupInvitationRepository).deleteAllByGroupIdIn(List.of(1L));
        verify(groupMemberRemovalRepository).deleteAllByGroupIdIn(List.of(1L));
        verify(groupBookEntryRepository).deleteAllByGroupIdIn(List.of(1L));
        verify(groupMemberRepository).deleteAllByGroupId(1L);
        verify(groupRepository).delete(group);
    }

    // ===== 초대 링크 =====

    @Test
    @DisplayName("모임장이 아니면 초대 링크를 조회할 수 없다")
    void getInviteLinkFailsWhenNotHost() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(group(1L, host)));

        assertThatThrownBy(() -> groupService.getInviteLink(1L, other.getId())).isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("모임장은 현재 유효한 초대 코드를 조회한다")
    void getInviteLinkReturnsActiveInvitation() {
        Group group = group(1L, host);
        GroupInvitation active = invitation(group);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupInvitationRepository.findFirstByGroupIdAndStatusOrderByIdDesc(1L, GroupInvitationStatus.ACTIVE))
                .willReturn(Optional.of(active));

        given(groupMemberRepository.countActiveMembers(1L)).willReturn(4L);

        GroupInviteLink inviteLink = groupService.getInviteLink(1L, host.getId());

        assertThat(inviteLink.inviteCode()).isEqualTo(active.getCode());
        assertThat(inviteLink.isFull()).isTrue(); // 정원 4명, 현재 4명
        verify(groupInvitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("유효한 초대가 없으면 새로 발급해 돌려준다")
    void getInviteLinkIssuesNewInvitationWhenNoneActive() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupInvitationRepository.findFirstByGroupIdAndStatusOrderByIdDesc(1L, GroupInvitationStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(groupInvitationRepository.save(any(GroupInvitation.class))).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(groupService.getInviteLink(1L, host.getId()).inviteCode()).hasSize(32);
        verify(groupInvitationRepository).save(any(GroupInvitation.class));
    }

    @Test
    @DisplayName("모임장이 아니면 초대 링크를 재발급할 수 없다")
    void regenerateInviteLinkFailsWhenNotHost() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(group(1L, host)));

        assertThatThrownBy(() -> groupService.regenerateInviteLink(1L, other.getId())).isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("초대 링크를 재발급하면 기존 유효 초대는 종료되고 다른 코드가 발급된다")
    void regenerateInviteLinkRevokesOldAndIssuesNew() {
        Group group = group(1L, host);
        GroupInvitation old = invitation(group);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupInvitationRepository.findAllByGroupIdAndStatus(1L, GroupInvitationStatus.ACTIVE)).willReturn(List.of(old));
        given(groupInvitationRepository.save(any(GroupInvitation.class))).willAnswer(invocation -> invocation.getArgument(0));

        String newCode = groupService.regenerateInviteLink(1L, host.getId()).inviteCode();

        assertThat(old.isActive()).isFalse();
        assertThat(newCode).isNotEqualTo(old.getCode());
    }

    // ===== 초대 미리보기 / 가입 =====

    @Test
    @DisplayName("유효하지 않은 초대 코드로 미리보기를 조회하면 예외가 발생한다")
    void previewInvitationFailsWhenInvalidCode() {
        given(groupInvitationRepository.findWithGroupByCode("invalid")).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.previewInvitation("invalid", null)).isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("종료된 초대 코드로 미리보기를 조회하면 예외가 발생한다")
    void previewInvitationFailsWhenRevoked() {
        GroupInvitation revoked = invitation(group(1L, host));
        revoked.revoke(LocalDateTime.now(CLOCK));
        given(groupInvitationRepository.findWithGroupByCode(revoked.getCode())).willReturn(Optional.of(revoked));

        assertThatThrownBy(() -> groupService.previewInvitation(revoked.getCode(), null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INVALID_INVITE_CODE);
    }

    @Test
    @DisplayName("합쳐진 모임의 초대 코드로는 미리보기를 조회할 수 없다")
    void previewInvitationFailsWhenGroupMerged() {
        Group merged = group(1L, host);
        ReflectionTestUtils.setField(merged, "status", GroupStatus.MERGED);
        GroupInvitation invitation = invitation(merged);
        given(groupInvitationRepository.findWithGroupByCode(invitation.getCode())).willReturn(Optional.of(invitation));

        assertThatThrownBy(() -> groupService.previewInvitation(invitation.getCode(), null))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INVALID_INVITE_CODE);
    }

    @Test
    @DisplayName("비로그인 상태로 초대 미리보기를 조회하면 alreadyJoined는 항상 false다")
    void previewInvitationForGuest() {
        Group group = group(1L, host);
        GroupInvitation invitation = invitation(group);
        given(groupInvitationRepository.findWithGroupByCode(invitation.getCode())).willReturn(Optional.of(invitation));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(2L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, ReadingPeriod.NONE));

        GroupInvitationPreview preview = groupService.previewInvitation(invitation.getCode(), null);

        assertThat(preview.memberCount()).isEqualTo(2L);
        assertThat(preview.alreadyJoined()).isFalse();
    }

    @Test
    @DisplayName("이미 가입한 사용자가 초대 미리보기를 조회하면 alreadyJoined가 true다")
    void previewInvitationForExistingMember() {
        Group group = group(1L, host);
        GroupInvitation invitation = invitation(group);
        given(groupInvitationRepository.findWithGroupByCode(invitation.getCode())).willReturn(Optional.of(invitation));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(2L);
        given(groupMemberRepository.existsActiveMember(1L, host.getId())).willReturn(true);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, ReadingPeriod.NONE));

        GroupInvitationPreview preview = groupService.previewInvitation(invitation.getCode(), host.getId());

        assertThat(preview.alreadyJoined()).isTrue();
    }

    @Test
    @DisplayName("유효하지 않은 초대 코드로 가입하면 예외가 발생한다")
    void joinGroupFailsWhenInvalidCode() {
        given(groupInvitationRepository.findWithGroupByCode("invalid")).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.joinGroup("invalid", other.getId())).isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("이미 가입한 모임에 다시 가입하면 예외가 발생한다")
    void joinGroupFailsWhenAlreadyJoined() {
        Group group = group(1L, host);
        GroupInvitation invitation = invitation(group);
        given(groupInvitationRepository.findWithGroupByCode(invitation.getCode())).willReturn(Optional.of(invitation));
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndUserId(1L, host.getId()))
                .willReturn(Optional.of(GroupMember.join(group, host, GroupMemberRole.HOST, LocalDateTime.now(CLOCK))));

        assertThatThrownBy(() -> groupService.joinGroup(invitation.getCode(), host.getId()))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.ALREADY_JOINED);
    }

    @Test
    @DisplayName("정원이 가득 찬 모임에 가입하면 예외가 발생한다")
    void joinGroupFailsWhenFull() {
        Group group = group(1L, host);
        GroupInvitation invitation = invitation(group);
        given(groupInvitationRepository.findWithGroupByCode(invitation.getCode())).willReturn(Optional.of(invitation));
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndUserId(1L, other.getId())).willReturn(Optional.empty());
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(4L);

        assertThatThrownBy(() -> groupService.joinGroup(invitation.getCode(), other.getId()))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.GROUP_FULL);
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("정원이 남아있으면 초대 링크로 모임에 가입할 수 있다")
    void joinGroupSucceeds() {
        Group group = group(1L, host);
        GroupInvitation invitation = invitation(group);
        given(groupInvitationRepository.findWithGroupByCode(invitation.getCode())).willReturn(Optional.of(invitation));
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndUserId(1L, other.getId())).willReturn(Optional.empty());
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(1L);
        given(userRepository.getReferenceById(other.getId())).willReturn(other);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, ReadingPeriod.NONE));

        GroupDetail detail = groupService.joinGroup(invitation.getCode(), other.getId());

        assertThat(detail.memberCount()).isEqualTo(2L);
        verify(groupMemberRepository).save(any(GroupMember.class));
        verify(eventPublisher).publishEvent(new GroupJoinedEvent(1L, "고전 뽀개기", other.getId(), host.getId()));
    }

    @Test
    @DisplayName("예전에 나간 멤버가 다시 가입하면 새 행을 만들지 않고 기존 멤버십을 되살린다")
    void joinGroupReactivatesPreviousMembership() {
        Group group = group(1L, host);
        GroupInvitation invitation = invitation(group);
        GroupMember previous = GroupMember.join(group, other, GroupMemberRole.MEMBER, LocalDateTime.of(2026, 1, 1, 0, 0));
        ReflectionTestUtils.setField(previous, "status", GroupMemberStatus.LEFT);
        given(groupInvitationRepository.findWithGroupByCode(invitation.getCode())).willReturn(Optional.of(invitation));
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndUserId(1L, other.getId())).willReturn(Optional.of(previous));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(1L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, ReadingPeriod.NONE));

        groupService.joinGroup(invitation.getCode(), other.getId());

        assertThat(previous.isActive()).isTrue();
        assertThat(previous.getJoinedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    private GroupMember removedMembership(Group group, LocalDateTime removedAt) {
        GroupMember membership = GroupMember.join(group, other, GroupMemberRole.MEMBER, removedAt.minusDays(30));
        membership.remove(removedAt);
        return membership;
    }

    private GroupInvitation invitationIssuedAt(Group group, LocalDateTime issuedAt) {
        GroupInvitation invitation = invitation(group);
        ReflectionTestUtils.setField(invitation, "createdAt", issuedAt);
        return invitation;
    }

    @Test
    @DisplayName("내보내진 계정이 내보내기 전에 발급된 초대로 다시 가입하면 예외가 발생하고, 정원 안내보다 먼저 알려준다")
    void joinGroupFailsWhenRemovedMemberUsesOldInvitation() {
        Group group = group(1L, host);
        LocalDateTime removedAt = LocalDateTime.of(2026, 11, 10, 12, 0);
        GroupInvitation oldInvitation = invitationIssuedAt(group, removedAt.minusDays(5));
        GroupMember removed = removedMembership(group, removedAt);
        given(groupInvitationRepository.findWithGroupByCode(oldInvitation.getCode())).willReturn(Optional.of(oldInvitation));
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndUserId(1L, other.getId())).willReturn(Optional.of(removed));

        assertThatThrownBy(() -> groupService.joinGroup(oldInvitation.getCode(), other.getId()))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.REJOIN_REQUIRES_NEW_INVITE);
        assertThat(removed.isActive()).isFalse();
        verify(groupMemberRepository, never()).countActiveMembers(any());
    }

    @Test
    @DisplayName("내보내진 계정도 내보낸 뒤 새로 발급된 초대로는 다시 가입할 수 있다")
    void joinGroupAllowsRemovedMemberWithNewInvitation() {
        Group group = group(1L, host);
        LocalDateTime removedAt = LocalDateTime.of(2026, 11, 10, 12, 0);
        GroupInvitation newInvitation = invitationIssuedAt(group, removedAt.plusMinutes(1));
        GroupMember removed = removedMembership(group, removedAt);
        given(groupInvitationRepository.findWithGroupByCode(newInvitation.getCode())).willReturn(Optional.of(newInvitation));
        given(groupRepository.findByIdForUpdate(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndUserId(1L, other.getId())).willReturn(Optional.of(removed));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(1L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(entry(group, ReadingPeriod.NONE));

        groupService.joinGroup(newInvitation.getCode(), other.getId());

        assertThat(removed.isActive()).isTrue();
    }
}
