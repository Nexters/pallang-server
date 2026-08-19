package com.nexters.palang.domain.group.presentation;

import com.nexters.palang.domain.group.application.GroupDetail;
import com.nexters.palang.domain.group.application.GroupInvitationPreview;
import com.nexters.palang.domain.group.application.GroupMapper;
import com.nexters.palang.domain.group.application.GroupService;
import com.nexters.palang.domain.group.presentation.dto.CreateGroupRequest;
import com.nexters.palang.domain.group.presentation.dto.GroupDetailResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupInvitationPreviewResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupInviteLinkResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupListResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupMemberListResponse;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupRequest;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupController implements GroupApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final GroupService groupService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PostMapping("/api/groups")
    public ResponseEntity<DataResponse<GroupDetailResponse>> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        GroupDetail detail = groupService.createGroup(currentUserId, request);
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toDetailResponse(detail)));
    }

    @Override
    @GetMapping("/api/groups")
    public ResponseEntity<DataResponse<GroupListResponse>> getMyGroups(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(
                GroupMapper.toListResponse(groupService.getMyGroups(currentUserId, pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/groups/{groupId}")
    public ResponseEntity<DataResponse<GroupDetailResponse>> getGroupDetail(@PathVariable Long groupId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        GroupDetail detail = groupService.getGroupDetail(groupId, currentUserId);
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toDetailResponse(detail)));
    }

    @Override
    @PatchMapping("/api/groups/{groupId}")
    public ResponseEntity<DataResponse<GroupDetailResponse>> updateGroup(
            @PathVariable Long groupId, @RequestBody @Valid UpdateGroupRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        GroupDetail detail = groupService.updateGroup(groupId, currentUserId, request);
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toDetailResponse(detail)));
    }

    @Override
    @DeleteMapping("/api/groups/{groupId}")
    public ResponseEntity<DataResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        groupService.deleteGroup(groupId, currentUserId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    @Override
    @GetMapping("/api/groups/{groupId}/members")
    public ResponseEntity<DataResponse<GroupMemberListResponse>> getGroupMembers(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toMemberListResponse(
                groupService.getGroupMembers(groupId, currentUserId, pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/groups/{groupId}/invite-link")
    public ResponseEntity<DataResponse<GroupInviteLinkResponse>> getInviteLink(@PathVariable Long groupId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        String inviteCode = groupService.getInviteLink(groupId, currentUserId);
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toInviteLinkResponse(groupId, inviteCode)));
    }

    @Override
    @PostMapping("/api/groups/{groupId}/invite-link/regenerate")
    public ResponseEntity<DataResponse<GroupInviteLinkResponse>> regenerateInviteLink(@PathVariable Long groupId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        String inviteCode = groupService.regenerateInviteLink(groupId, currentUserId);
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toInviteLinkResponse(groupId, inviteCode)));
    }

    @Override
    @GetMapping("/api/groups/invitations/{inviteCode}")
    public ResponseEntity<DataResponse<GroupInvitationPreviewResponse>> previewInvitation(@PathVariable String inviteCode) {
        Long currentUserId = currentUserProvider.findCurrentUserId().orElse(null);
        GroupInvitationPreview preview = groupService.previewInvitation(inviteCode, currentUserId);
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toInvitationPreviewResponse(preview)));
    }

    @Override
    @PostMapping("/api/groups/invitations/{inviteCode}/join")
    public ResponseEntity<DataResponse<GroupDetailResponse>> joinGroup(@PathVariable String inviteCode) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        GroupDetail detail = groupService.joinGroup(inviteCode, currentUserId);
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toDetailResponse(detail)));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
