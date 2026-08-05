package com.nexters.palang.domain.block.presentation;

import com.nexters.palang.domain.block.application.BlockService;
import com.nexters.palang.domain.block.presentation.dto.BlockedUserListResponse;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BlockController implements BlockApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final BlockService blockService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PostMapping("/api/users/{userId}/block")
    public ResponseEntity<DataResponse<Void>> block(@PathVariable Long userId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        blockService.block(currentUserId, userId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    @Override
    @DeleteMapping("/api/users/{userId}/block")
    public ResponseEntity<DataResponse<Void>> unblock(@PathVariable Long userId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        blockService.unblock(currentUserId, userId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    @Override
    @GetMapping("/api/users/me/blocks")
    public ResponseEntity<DataResponse<BlockedUserListResponse>> getBlockedUsers(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(
                BlockedUserListResponse.from(blockService.getBlockedUsers(currentUserId, pageable(page, size)))));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
