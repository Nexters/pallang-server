package com.nexters.palang.domain.user.presentation;

import com.nexters.palang.domain.opinion.application.OpinionService;
import com.nexters.palang.domain.user.application.UserMapper;
import com.nexters.palang.domain.user.application.UserService;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.presentation.dto.LikedOpinionListResponse;
import com.nexters.palang.domain.user.presentation.dto.MeResponse;
import com.nexters.palang.domain.user.presentation.dto.MyOpinionListResponse;
import com.nexters.palang.domain.user.presentation.dto.UpdateBackgroundColorRequest;
import com.nexters.palang.domain.user.presentation.dto.UpdateNicknameRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserService userService;
    private final OpinionService opinionService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @GetMapping("/api/users/me")
    public ResponseEntity<DataResponse<MeResponse>> getMe() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        User user = userService.getMe(currentUserId);
        return ResponseEntity.ok(DataResponse.from(toMeResponse(currentUserId, user)));
    }

    @Override
    @PatchMapping("/api/users/me/nickname")
    public ResponseEntity<DataResponse<MeResponse>> modifyNickname(@RequestBody @Valid UpdateNicknameRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        User user = userService.modifyNickname(currentUserId, request.nickname());
        return ResponseEntity.ok(DataResponse.from(toMeResponse(currentUserId, user)));
    }

    @Override
    @PatchMapping("/api/users/me/background-color")
    public ResponseEntity<DataResponse<MeResponse>> modifyBackgroundColor(
            @RequestBody @Valid UpdateBackgroundColorRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        User user = userService.modifyBackgroundColor(currentUserId, request.backgroundColor());
        return ResponseEntity.ok(DataResponse.from(toMeResponse(currentUserId, user)));
    }

    @Override
    @DeleteMapping("/api/users/me")
    public ResponseEntity<DataResponse<Void>> withdraw() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        userService.withdraw(currentUserId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    @Override
    @GetMapping("/api/users/me/opinions")
    public ResponseEntity<DataResponse<MyOpinionListResponse>> getMyOpinions(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(UserMapper.toMyOpinionListResponse(
                opinionService.getMyOpinions(currentUserId, pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/users/me/likes")
    public ResponseEntity<DataResponse<LikedOpinionListResponse>> getLikedOpinions(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(UserMapper.toLikedOpinionListResponse(
                opinionService.getLikedOpinions(currentUserId, pageable(page, size)))));
    }

    private MeResponse toMeResponse(Long userId, User user) {
        return UserMapper.toMeResponse(user, opinionService.getMyOpinionCount(userId));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
