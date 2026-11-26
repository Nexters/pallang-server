package com.nexters.palang.domain.group.presentation;

import com.nexters.palang.domain.group.application.GroupMapper;
import com.nexters.palang.domain.group.application.GroupMemberService;
import com.nexters.palang.domain.group.presentation.dto.GroupMemberListResponse;
import com.nexters.palang.domain.group.presentation.dto.RemoveGroupMemberRequest;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupMemberController implements GroupMemberApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final GroupMemberService groupMemberService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @GetMapping("/api/groups/{groupId}/members")
    public ResponseEntity<DataResponse<GroupMemberListResponse>> getGroupMembers(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toMemberListResponse(
                groupMemberService.getGroupMembers(groupId, currentUserId, pageable(page, size)))));
    }

    @Override
    @PostMapping("/api/groups/{groupId}/members/{userId}/removal")
    public ResponseEntity<DataResponse<GroupMemberListResponse>> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody RemoveGroupMemberRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(GroupMapper.toMemberListResponse(groupMemberService.removeMember(
                groupId, userId, currentUserId, request, idempotencyKey, pageable(DEFAULT_PAGE, DEFAULT_SIZE)))));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
