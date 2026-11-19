package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.group.application.GroupBookEntries;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminGroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupBookEntries groupBookEntries;
    @Mock
    private AdminCascadeDeleter cascadeDeleter;

    private AdminGroupService adminGroupService;

    @BeforeEach
    void setUp() {
        adminGroupService = new AdminGroupService(groupRepository, groupMemberRepository, groupBookEntries, cascadeDeleter);
    }

    @Test
    @DisplayName("존재하지 않는 모임을 수정하려 하면 예외가 발생한다")
    void updateFailsWhenGroupNotFound() {
        given(groupRepository.findWithAssociationsById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminGroupService.updateGroup(1L, "이름", 4, LocalDate.now(), LocalDate.now().plusDays(1)))
                .isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("모임을 수정하면 현재 참여 인원을 기준으로 설정이 바뀌고, 과거 날짜여도 대표 책 기간이 정정된다")
    void updateGroupDelegatesToEntity() {
        Group group = mock(Group.class);
        GroupBookEntry primaryEntry = mock(GroupBookEntry.class);
        given(groupRepository.findWithAssociationsById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.countActiveMembers(1L)).willReturn(3L);
        given(groupBookEntries.getPrimaryEntry(1L)).willReturn(primaryEntry);
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.of(2020, 2, 1);

        AdminGroupSearchResult result = adminGroupService.updateGroup(1L, "새 이름", 5, start, end);

        verify(group).updateSettings("새 이름", 5, 3L);
        verify(primaryEntry).correctPeriod(ReadingPeriod.of(start, end));
        assertThat(result.memberCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("존재하지 않는 모임을 삭제하려 하면 예외가 발생한다")
    void deleteFailsWhenGroupNotFound() {
        given(groupRepository.findWithAssociationsById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminGroupService.deleteGroup(1L)).isInstanceOf(GroupException.class);
    }

    @Test
    @DisplayName("모임을 삭제하면 cascade 삭제기가 호출된다")
    void deleteGroupDelegatesToCascadeDeleter() {
        given(groupRepository.findWithAssociationsById(1L)).willReturn(Optional.of(mock(Group.class)));

        adminGroupService.deleteGroup(1L);

        verify(cascadeDeleter).deleteGroups(List.of(1L));
    }
}
