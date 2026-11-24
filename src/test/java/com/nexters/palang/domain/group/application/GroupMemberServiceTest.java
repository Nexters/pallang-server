package com.nexters.palang.domain.group.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRemoval;
import com.nexters.palang.domain.group.domain.GroupMemberRemovalReason;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.domain.GroupMemberStatus;
import com.nexters.palang.domain.group.domain.GroupStatus;
import com.nexters.palang.domain.group.domain.event.GroupMemberRemovedEvent;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRemovalRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupQueryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.group.presentation.dto.RemoveGroupMemberRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.idempotency.IdempotentExecutor;
import com.nexters.palang.global.idempotency.IdempotentOperation;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupMemberServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(ZonedDateTime.of(2026, 11, 20, 12, 0, 0, 0, SEOUL).toInstant(), SEOUL);
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);
    private static final RemoveGroupMemberRequest SPAM = new RemoveGroupMemberRequest(GroupMemberRemovalReason.SPAM_OR_OFF_TOPIC, null);

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupMemberRemovalRepository groupMemberRemovalRepository;
    @Mock
    private GroupQueryRepository groupQueryRepository;
    @Mock
    private IdempotentExecutor idempotentExecutor;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GroupMemberService service;
    private User host;
    private User member;
    private Group group;

    @BeforeEach
    void setUp() {
        service = new GroupMemberService(groupRepository, groupMemberRepository, groupMemberRemovalRepository,
                groupQueryRepository, idempotentExecutor, eventPublisher, CLOCK);
        host = user(1L);
        member = user(2L);
        group = Group.create("고전 뽀개기", host, 4);
        ReflectionTestUtils.setField(group, "id", 9L);
    }

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private GroupMember membership(User user, GroupMemberRole role) {
        GroupMember built = GroupMember.join(group, user, role, LocalDateTime.of(2026, 11, 1, 9, 0));
        ReflectionTestUtils.setField(built, "id", user.getId() * 100);
        return built;
    }

    @SuppressWarnings("unchecked")
    private void runIdempotentActionDirectly() {
        given(idempotentExecutor.execute(anyLong(), eq(IdempotentOperation.REMOVE_GROUP_MEMBER), any(), any()))
                .willAnswer(invocation -> ((Supplier<Long>) invocation.getArgument(3)).get());
    }

    // 내보내기 후 최신 명단 조회(모임장이 조회)
    private void rosterAvailableForHost() {
        given(groupRepository.findById(9L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsActiveMember(9L, 1L)).willReturn(true);
        given(groupQueryRepository.findMembers(9L, PAGEABLE))
                .willReturn(new PageImpl<>(List.of(membership(host, GroupMemberRole.HOST)), PAGEABLE, 1));
    }

    // ===== 명단 =====

    @Test
    @DisplayName("현재 멤버가 아니면 명단을 볼 수 없다")
    void getGroupMembersFailsWhenNotMember() {
        given(groupRepository.findById(9L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsActiveMember(9L, 3L)).willReturn(false);

        assertThatThrownBy(() -> service.getGroupMembers(9L, 3L, PAGEABLE))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.NOT_MEMBER);
    }

    @Test
    @DisplayName("합쳐진 모임의 명단은 볼 수 없다")
    void getGroupMembersFailsWhenGroupMerged() {
        ReflectionTestUtils.setField(group, "status", GroupStatus.MERGED);
        given(groupRepository.findById(9L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> service.getGroupMembers(9L, 1L, PAGEABLE))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("명단에는 모임과 조회자가 함께 담긴다")
    void getGroupMembersReturnsGroupAndViewer() {
        rosterAvailableForHost();

        GroupMemberPage page = service.getGroupMembers(9L, 1L, PAGEABLE);

        assertThat(page.group()).isEqualTo(group);
        assertThat(page.viewerId()).isEqualTo(1L);
        assertThat(page.members().getTotalElements()).isEqualTo(1);
    }

    // ===== 내보내기 =====

    @Test
    @DisplayName("모임장이 멤버를 내보내면 멤버십이 REMOVED가 되고, 감사 이력이 남고, 대상에게 알릴 이벤트가 발행된다")
    void removeMemberSucceeds() {
        runIdempotentActionDirectly();
        GroupMember target = membership(member, GroupMemberRole.MEMBER);
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndUserId(9L, 2L)).willReturn(Optional.of(target));
        rosterAvailableForHost();

        GroupMemberPage roster = service.removeMember(9L, 2L, 1L,
                new RemoveGroupMemberRequest(GroupMemberRemovalReason.OTHER, " 규칙 위반 "), "remove-key", PAGEABLE);

        assertThat(target.getStatus()).isEqualTo(GroupMemberStatus.REMOVED);
        assertThat(target.getRemovedAt()).isEqualTo(LocalDateTime.of(2026, 11, 20, 12, 0));
        ArgumentCaptor<GroupMemberRemoval> captor = ArgumentCaptor.forClass(GroupMemberRemoval.class);
        verify(groupMemberRemovalRepository).save(captor.capture());
        GroupMemberRemoval removal = captor.getValue();
        assertThat(removal.getGroupId()).isEqualTo(9L);
        assertThat(removal.getActorUserId()).isEqualTo(1L);
        assertThat(removal.getTargetUserId()).isEqualTo(2L);
        assertThat(removal.getReason()).isEqualTo(GroupMemberRemovalReason.OTHER);
        assertThat(removal.getOtherText()).isEqualTo("규칙 위반");
        assertThat(removal.getRequestKey()).isEqualTo("remove-key");
        verify(eventPublisher).publishEvent(new GroupMemberRemovedEvent(9L, "고전 뽀개기", 2L));
        assertThat(roster.viewerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("모임장이 아니면 멤버를 내보낼 수 없다")
    void removeMemberFailsWhenNotHost() {
        runIdempotentActionDirectly();
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> service.removeMember(9L, 3L, 2L, SPAM, null, PAGEABLE))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.NOT_HOST);
        verify(groupMemberRemovalRepository, never()).save(any());
    }

    @Test
    @DisplayName("모임장 자신은 내보낼 수 없다")
    void removeMemberFailsForHostSelf() {
        runIdempotentActionDirectly();
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> service.removeMember(9L, 1L, 1L, SPAM, null, PAGEABLE))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.CANNOT_REMOVE_HOST);
    }

    @Test
    @DisplayName("사유 형식이 잘못되면 대상 상태와 무관하게 예외가 발생하고 아무것도 바뀌지 않는다")
    void removeMemberFailsWithInvalidReason() {
        runIdempotentActionDirectly();
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> service.removeMember(9L, 2L, 1L,
                new RemoveGroupMemberRequest(GroupMemberRemovalReason.OTHER, "  "), null, PAGEABLE))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.INVALID_REMOVAL_REASON);
        verify(groupMemberRepository, never()).findByGroupIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("이 모임의 멤버였던 적이 없으면 예외가 발생한다")
    void removeMemberFailsWhenNeverMember() {
        runIdempotentActionDirectly();
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndUserId(9L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember(9L, 7L, 1L, SPAM, null, PAGEABLE))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.GROUP_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 나간 멤버를 내보내면 멤버십과 이력을 바꾸지 않고 최신 명단만 돌려준다")
    void removeMemberIsNoOpForAlreadyLeftMember() {
        runIdempotentActionDirectly();
        GroupMember left = membership(member, GroupMemberRole.MEMBER);
        ReflectionTestUtils.setField(left, "status", GroupMemberStatus.LEFT);
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndUserId(9L, 2L)).willReturn(Optional.of(left));
        rosterAvailableForHost();

        GroupMemberPage roster = service.removeMember(9L, 2L, 1L, SPAM, null, PAGEABLE);

        assertThat(left.getStatus()).isEqualTo(GroupMemberStatus.LEFT);
        verify(groupMemberRemovalRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        assertThat(roster.members().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("합쳐진 모임에서는 멤버를 내보낼 수 없다")
    void removeMemberFailsWhenGroupMerged() {
        runIdempotentActionDirectly();
        ReflectionTestUtils.setField(group, "status", GroupStatus.MERGED);
        given(groupRepository.findByIdForUpdate(9L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> service.removeMember(9L, 2L, 1L, SPAM, null, PAGEABLE))
                .isInstanceOf(GroupException.class)
                .extracting("errorCode").isEqualTo(GroupErrorCode.GROUP_NOT_FOUND);
    }
}
