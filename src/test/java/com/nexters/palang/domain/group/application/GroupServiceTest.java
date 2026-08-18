package com.nexters.palang.domain.group.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupQueryRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.group.presentation.dto.CreateGroupRequest;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupQueryRepository groupQueryRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    private GroupService groupService;

    private Book book;
    private User host;
    private User other;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(groupRepository, groupMemberRepository, groupQueryRepository, bookRepository, userRepository);
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

    private CreateGroupRequest createRequest() {
        return new CreateGroupRequest("고전 뽀개기", book.getId(), 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
    }

    private Group group(Long id, User host) {
        Group built = Group.create(
                "고전 뽀개기", book, host, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("모임을 생성하면 host가 모임원으로 함께 저장된다")
    void createGroupSucceeds() {
        given(bookRepository.findById(book.getId())).willReturn(Optional.of(book));
        given(userRepository.getReferenceById(host.getId())).willReturn(host);

        GroupDetail detail = groupService.createGroup(host.getId(), createRequest());

        assertThat(detail.group().getHost()).isEqualTo(host);
        assertThat(detail.memberCount()).isEqualTo(1L);
        verify(groupRepository).save(any(Group.class));
        verify(groupMemberRepository).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("존재하지 않는 책으로 모임을 만들면 예외가 발생한다")
    void createGroupFailsWhenBookNotFound() {
        given(bookRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(
                host.getId(), new CreateGroupRequest("고전 뽀개기", 999L, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20))))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("존재하지 않는 모임을 조회하면 예외가 발생한다")
    void getGroupDetailFailsWhenNotFound() {
        given(groupRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupDetail(999L, host.getId())).isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("모임원이 아니면 모임 상세를 조회할 수 없다")
    void getGroupDetailFailsWhenNotMember() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(1L, other.getId())).willReturn(false);

        assertThatThrownBy(() -> groupService.getGroupDetail(1L, other.getId())).isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("모임원이면 모임 상세와 참여 인원 수를 함께 조회한다")
    void getGroupDetailSucceeds() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(1L, host.getId())).willReturn(true);
        given(groupMemberRepository.countByGroupId(1L)).willReturn(3L);

        GroupDetail detail = groupService.getGroupDetail(1L, host.getId());

        assertThat(detail.group()).isEqualTo(group);
        assertThat(detail.memberCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("모임장이 아니면 방 설정을 변경할 수 없다")
    void updateGroupFailsWhenNotHost() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.updateGroup(
                1L, other.getId(), new UpdateGroupRequest("주말 독서 모임", 6, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20))))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("현재 참여 인원보다 적은 인원으로 방 설정을 변경하면 예외가 발생한다")
    void updateGroupFailsWhenCapacityBelowMemberCount() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countByGroupId(1L)).willReturn(3L);

        assertThatThrownBy(() -> groupService.updateGroup(
                1L, host.getId(), new UpdateGroupRequest("고전 뽀개기", 2, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20))))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("모임장은 방 설정을 변경할 수 있다")
    void updateGroupSucceeds() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countByGroupId(1L)).willReturn(1L);

        GroupDetail detail = groupService.updateGroup(
                1L, host.getId(), new UpdateGroupRequest("주말 독서 모임", 6, LocalDate.of(2026, 8, 21), LocalDate.of(2026, 9, 27)));

        assertThat(detail.group().getName()).isEqualTo("주말 독서 모임");
        assertThat(detail.group().getCapacity()).isEqualTo(6);
    }

    @Test
    @DisplayName("모임장이 아니면 모임을 삭제할 수 없다")
    void deleteGroupFailsWhenNotHost() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.deleteGroup(1L, other.getId())).isInstanceOf(GroupException.class);
        verify(groupRepository, never()).delete(any(Group.class));
    }

    @Test
    @DisplayName("모임장은 모임을 삭제할 수 있고, 모임원 레코드도 함께 삭제된다")
    void deleteGroupSucceeds() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));

        groupService.deleteGroup(1L, host.getId());

        verify(groupMemberRepository, times(1)).deleteAllByGroupId(1L);
        verify(groupRepository).delete(group);
    }

    @Test
    @DisplayName("모임원이 아니면 모임 멤버 목록을 조회할 수 없다")
    void getGroupMembersFailsWhenNotMember() {
        Group group = group(1L, host);
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupIdAndUserId(1L, other.getId())).willReturn(false);

        assertThatThrownBy(() -> groupService.getGroupMembers(1L, other.getId(), null)).isInstanceOf(GroupException.class);
    }
}
