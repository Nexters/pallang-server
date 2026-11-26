package com.nexters.palang.domain.group.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.group.application.GroupMemberPage;
import com.nexters.palang.domain.group.application.GroupMemberService;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.presentation.dto.RemoveGroupMemberRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.security.CurrentUserProvider;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GroupMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupMemberService groupMemberService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    // 모임장 1번, 멤버 2번, 정원 10명
    private GroupMemberPage roster(Long viewerId) {
        User host = user(1L);
        Group group = Group.create("고전 뽀개기", host, 10);
        List<GroupMember> members = List.of(
                GroupMember.join(group, host, GroupMemberRole.HOST, LocalDateTime.of(2026, 8, 1, 12, 0)),
                GroupMember.join(group, user(2L), GroupMemberRole.MEMBER, LocalDateTime.of(2026, 8, 2, 12, 0)));
        return new GroupMemberPage(group, new PageImpl<>(members, PageRequest.of(0, 20), 2), viewerId);
    }

    @Test
    @DisplayName("명단에는 현재 인원/정원과 행마다 모임장·본인 표시가 담긴다")
    void getGroupMembers() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupMemberService.getGroupMembers(eq(1L), eq(1L), any())).willReturn(roster(1L));

        mockMvc.perform(get("/api/groups/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(2))
                .andExpect(jsonPath("$.data.capacity").value(10))
                .andExpect(jsonPath("$.data.members[0].role").value("HOST"))
                .andExpect(jsonPath("$.data.members[0].joinedAt").value("2026-08-01T12:00:00"))
                .andExpect(jsonPath("$.data.members[0].isHost").value(true))
                .andExpect(jsonPath("$.data.members[0].isMe").value(true))
                .andExpect(jsonPath("$.data.members[1].isHost").value(false))
                .andExpect(jsonPath("$.data.members[1].isMe").value(false));
    }

    @Test
    @DisplayName("일반 멤버가 조회하면 본인 행에만 isMe가 켜진다")
    void getGroupMembersMarksViewerRow() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(2L);
        given(groupMemberService.getGroupMembers(eq(1L), eq(2L), any())).willReturn(roster(2L));

        mockMvc.perform(get("/api/groups/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members[0].isMe").value(false))
                .andExpect(jsonPath("$.data.members[1].isMe").value(true));
    }

    @Test
    @DisplayName("내보내기를 요청하면 사유와 요청 키가 서비스로 전달되고 최신 명단을 반환한다")
    void removeMember() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupMemberService.removeMember(eq(1L), eq(2L), eq(1L), any(RemoveGroupMemberRequest.class),
                eq("remove-key"), any())).willReturn(roster(1L));

        mockMvc.perform(post("/api/groups/1/members/2/removal")
                        .header("Idempotency-Key", "remove-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"OTHER\",\"otherText\":\"규칙 위반\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(2));
    }

    @Test
    @DisplayName("사유 없이 내보내기를 요청하면 400 에러가 발생한다")
    void removeMemberFailsWithoutReason() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/groups/1/members/2/removal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("없는 사유 코드로 요청하면 400 에러가 발생한다")
    void removeMemberFailsWithUnknownReason() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/groups/1/members/2/removal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("모임장 자신을 내보내려 하면 409 에러가 발생한다")
    void removeMemberFailsForHost() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupMemberService.removeMember(eq(1L), eq(1L), eq(1L), any(RemoveGroupMemberRequest.class),
                isNull(), any())).willThrow(new GroupException(GroupErrorCode.CANNOT_REMOVE_HOST));

        mockMvc.perform(post("/api/groups/1/members/1/removal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"SPAM_OR_OFF_TOPIC\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("GROUP_409_6"));
    }
}
