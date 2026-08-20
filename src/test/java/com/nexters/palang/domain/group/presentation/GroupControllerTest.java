package com.nexters.palang.domain.group.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.application.GroupDetail;
import com.nexters.palang.domain.group.application.GroupInvitationPreview;
import com.nexters.palang.domain.group.application.GroupService;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupMember;
import com.nexters.palang.domain.group.domain.GroupMemberRole;
import com.nexters.palang.domain.group.presentation.dto.CreateGroupRequest;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private Book book() {
        Book built = Book.builder()
                .title("채식주의자").author("한강").publisher("창비").pageCount(268).source(BookSource.API).build();
        ReflectionTestUtils.setField(built, "id", 1L);
        return built;
    }

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Group group(Long id, User host) {
        Group built = Group.create("고전 뽀개기", book(), host, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private CreateGroupRequest createRequest() {
        return new CreateGroupRequest("고전 뽀개기", 1L, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
    }

    private UpdateGroupRequest updateRequest() {
        return new UpdateGroupRequest("주말 독서 모임", 6, LocalDate.of(2026, 8, 21), LocalDate.of(2026, 9, 27));
    }

    @Test
    @DisplayName("모임을 생성하면 생성된 모임 정보를 반환한다")
    void createGroup() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        Group group = group(1L, user(1L));
        given(groupService.createGroup(eq(1L), any(CreateGroupRequest.class))).willReturn(new GroupDetail(group, 1L));

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(1))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.isHost").value(true));
    }

    @Test
    @DisplayName("인증 없이 모임을 생성하면 401 에러가 발생한다")
    void createGroupFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("모임명 없이 모임을 생성하면 400 에러가 발생한다")
    void createGroupFailsWhenNameMissing() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"capacity\":4,\"startDate\":\"2026-08-20\",\"endDate\":\"2026-09-20\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("내 모임 목록을 조회한다")
    void getMyGroups() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupService.getMyGroups(eq(1L), any())).willReturn(new PageImpl<>(List.of(), DEFAULT_PAGEABLE, 0));

        mockMvc.perform(get("/api/groups")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("모임 상세를 조회한다")
    void getGroupDetail() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        Group group = group(1L, user(1L));
        given(groupService.getGroupDetail(1L, 1L)).willReturn(new GroupDetail(group, 2L));

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(2))
                .andExpect(jsonPath("$.data.isHost").value(true));
    }

    @Test
    @DisplayName("모임장이 아닌 모임원이 상세 조회하면 isHost는 false다")
    void getGroupDetail_whenNotHost_thenIsHostFalse() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(2L);
        Group group = group(1L, user(1L));
        given(groupService.getGroupDetail(1L, 2L)).willReturn(new GroupDetail(group, 2L));

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isHost").value(false));
    }

    @Test
    @DisplayName("모임원이 아니면 모임 상세 조회 시 403 에러가 발생한다")
    void getGroupDetailFailsWhenNotMember() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupService.getGroupDetail(1L, 1L)).willThrow(new GroupException(GroupErrorCode.NOT_MEMBER));

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("GROUP_403_2"));
    }

    @Test
    @DisplayName("존재하지 않는 모임을 조회하면 404 에러가 발생한다")
    void getGroupDetailFailsWhenNotFound() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupService.getGroupDetail(1L, 1L)).willThrow(new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("GROUP_404_1"));
    }

    @Test
    @DisplayName("방 설정을 변경하면 변경된 모임 정보를 반환한다")
    void updateGroup() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        Group group = group(1L, user(1L));
        group.updateSettings("주말 독서 모임", 6, LocalDate.of(2026, 8, 21), LocalDate.of(2026, 9, 27), 1L);
        given(groupService.updateGroup(eq(1L), eq(1L), any(UpdateGroupRequest.class))).willReturn(new GroupDetail(group, 1L));

        mockMvc.perform(patch("/api/groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("주말 독서 모임"))
                .andExpect(jsonPath("$.data.isHost").value(true));
    }

    @Test
    @DisplayName("모임장이 아니면 방 설정 변경 시 403 에러가 발생한다")
    void updateGroupFailsWhenNotHost() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(2L);
        given(groupService.updateGroup(eq(1L), eq(2L), any(UpdateGroupRequest.class)))
                .willThrow(new GroupException(GroupErrorCode.NOT_HOST));

        mockMvc.perform(patch("/api/groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("GROUP_403_1"));
    }

    @Test
    @DisplayName("현재 참여 인원보다 적은 인원으로 변경하면 409 에러가 발생한다")
    void updateGroupFailsWhenCapacityBelowMemberCount() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupService.updateGroup(eq(1L), eq(1L), any(UpdateGroupRequest.class)))
                .willThrow(new GroupException(GroupErrorCode.CAPACITY_BELOW_MEMBER_COUNT));

        mockMvc.perform(patch("/api/groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("GROUP_409_1"));
    }

    @Test
    @DisplayName("모임을 삭제한다")
    void deleteGroup() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(delete("/api/groups/1")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("모임장이 아니면 모임 삭제 시 403 에러가 발생한다")
    void deleteGroupFailsWhenNotHost() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(2L);
        org.mockito.Mockito.doThrow(new GroupException(GroupErrorCode.NOT_HOST))
                .when(groupService).deleteGroup(1L, 2L);

        mockMvc.perform(delete("/api/groups/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("GROUP_403_1"));
    }

    @Test
    @DisplayName("모임 멤버 목록을 조회한다")
    void getGroupMembers() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        Group group = group(1L, user(1L));
        GroupMember member = GroupMember.of(group, user(1L), GroupMemberRole.HOST);
        given(groupService.getGroupMembers(eq(1L), eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(member), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/groups/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members[0].role").value("HOST"));
    }

    @Test
    @DisplayName("모임장은 초대 링크를 조회할 수 있다")
    void getInviteLink() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupService.getInviteLink(1L, 1L)).willReturn("invitecode123");

        mockMvc.perform(get("/api/groups/1/invite-link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(1))
                .andExpect(jsonPath("$.data.inviteCode").value("invitecode123"));
    }

    @Test
    @DisplayName("모임장이 아니면 초대 링크 조회 시 403 에러가 발생한다")
    void getInviteLinkFailsWhenNotHost() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(2L);
        given(groupService.getInviteLink(1L, 2L)).willThrow(new GroupException(GroupErrorCode.NOT_HOST));

        mockMvc.perform(get("/api/groups/1/invite-link"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("GROUP_403_1"));
    }

    @Test
    @DisplayName("모임장은 초대 링크를 재발급할 수 있다")
    void regenerateInviteLink() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupService.regenerateInviteLink(1L, 1L)).willReturn("newinvitecode456");

        mockMvc.perform(post("/api/groups/1/invite-link/regenerate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteCode").value("newinvitecode456"));
    }

    @Test
    @DisplayName("초대 링크를 미리보기하면 모임 정보를 반환한다")
    void previewInvitation() throws Exception {
        Group group = group(1L, user(1L));
        given(groupService.previewInvitation(group.getInviteCode(), null))
                .willReturn(new GroupInvitationPreview(group, 2L, false));

        mockMvc.perform(get("/api/groups/invitations/" + group.getInviteCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(2))
                .andExpect(jsonPath("$.data.full").value(false))
                .andExpect(jsonPath("$.data.alreadyJoined").value(false));
    }

    @Test
    @DisplayName("유효하지 않은 초대 코드를 미리보기하면 404 에러가 발생한다")
    void previewInvitationFailsWhenInvalidCode() throws Exception {
        given(groupService.previewInvitation("invalid", null))
                .willThrow(new GroupException(GroupErrorCode.INVALID_INVITE_CODE));

        mockMvc.perform(get("/api/groups/invitations/invalid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("GROUP_404_2"));
    }

    @Test
    @DisplayName("초대 코드로 모임에 가입하면 가입된 모임 정보를 반환한다")
    void joinGroup() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(2L);
        Group group = group(1L, user(1L));
        given(groupService.joinGroup(group.getInviteCode(), 2L)).willReturn(new GroupDetail(group, 2L));

        mockMvc.perform(post("/api/groups/invitations/" + group.getInviteCode() + "/join"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(2))
                .andExpect(jsonPath("$.data.isHost").value(false));
    }

    @Test
    @DisplayName("인증 없이 모임에 가입하면 401 에러가 발생한다")
    void joinGroupFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        mockMvc.perform(post("/api/groups/invitations/somecode/join"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("정원이 가득 찬 모임에 가입하면 409 에러가 발생한다")
    void joinGroupFailsWhenFull() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(2L);
        given(groupService.joinGroup("somecode", 2L)).willThrow(new GroupException(GroupErrorCode.GROUP_FULL));

        mockMvc.perform(post("/api/groups/invitations/somecode/join"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("GROUP_409_3"));
    }
}
