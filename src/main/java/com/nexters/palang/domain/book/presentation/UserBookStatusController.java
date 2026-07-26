package com.nexters.palang.domain.book.presentation;

import com.nexters.palang.domain.book.application.UserBookStatusService;
import com.nexters.palang.domain.book.domain.UserBookStatus;
import com.nexters.palang.domain.book.presentation.dto.UpdateUserBookStatusRequest;
import com.nexters.palang.domain.book.presentation.dto.UserBookStatusResponse;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserBookStatusController implements UserBookStatusApi {

    private final UserBookStatusService userBookStatusService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PutMapping("/api/users/me/book-status")
    public ResponseEntity<DataResponse<UserBookStatusResponse>> updateBookStatus(
            @Valid @RequestBody UpdateUserBookStatusRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        UserBookStatus userBookStatus = userBookStatusService.updateBookStatus(currentUserId, request);
        return ResponseEntity.ok(DataResponse.from(UserBookStatusResponse.from(userBookStatus)));
    }
}
