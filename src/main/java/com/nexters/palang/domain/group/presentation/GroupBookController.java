package com.nexters.palang.domain.group.presentation;

import com.nexters.palang.domain.group.application.GroupBookMapper;
import com.nexters.palang.domain.group.application.GroupBookService;
import com.nexters.palang.domain.group.application.GroupBookStatusFilter;
import com.nexters.palang.domain.group.presentation.dto.AddGroupBookRequest;
import com.nexters.palang.domain.group.presentation.dto.GroupBookListResponse;
import com.nexters.palang.domain.group.presentation.dto.GroupBookResponse;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupBookRequest;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupBookController implements GroupBookApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final GroupBookService groupBookService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @GetMapping("/api/groups/{groupId}/books")
    public ResponseEntity<DataResponse<GroupBookListResponse>> getGroupBooks(
            @PathVariable Long groupId,
            @RequestParam(required = false) GroupBookStatusFilter status,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
        return ResponseEntity.ok(DataResponse.from(GroupBookMapper.toListResponse(
                groupBookService.getGroupBooks(groupId, currentUserId, status, pageable))));
    }

    @Override
    @PostMapping("/api/groups/{groupId}/books")
    public ResponseEntity<DataResponse<GroupBookResponse>> addGroupBook(
            @PathVariable Long groupId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody AddGroupBookRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(GroupBookMapper.toResponse(
                groupBookService.addGroupBook(groupId, currentUserId, request, idempotencyKey))));
    }

    @Override
    @PatchMapping("/api/groups/{groupId}/books/{entryId}")
    public ResponseEntity<DataResponse<GroupBookResponse>> updateGroupBook(
            @PathVariable Long groupId,
            @PathVariable Long entryId,
            @Valid @RequestBody UpdateGroupBookRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(GroupBookMapper.toResponse(
                groupBookService.updateGroupBook(groupId, entryId, currentUserId, request))));
    }
}
