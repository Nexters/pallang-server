package com.nexters.palang.domain.user.presentation;

import com.nexters.palang.domain.auth.application.AuthService;
import com.nexters.palang.domain.book.application.BookOptionProjection;
import com.nexters.palang.domain.opinion.application.OpinionService;
import com.nexters.palang.domain.passage.application.PassageService;
import com.nexters.palang.domain.user.application.UserMapper;
import com.nexters.palang.domain.user.application.UserService;
import com.nexters.palang.domain.user.domain.BookFilterType;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.presentation.dto.FilterBooksResponse;
import com.nexters.palang.domain.user.presentation.dto.LikedOpinionListResponse;
import com.nexters.palang.domain.user.presentation.dto.MeResponse;
import com.nexters.palang.domain.user.presentation.dto.MyOpinionListResponse;
import com.nexters.palang.domain.user.presentation.dto.MyPassageListResponse;
import com.nexters.palang.domain.user.presentation.dto.OnboardingCompleteResponse;
import com.nexters.palang.domain.user.presentation.dto.UpdateBackgroundColorRequest;
import com.nexters.palang.domain.user.presentation.dto.UpdateNicknameRequest;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserService userService;
    private final AuthService authService;
    private final OpinionService opinionService;
    private final PassageService passageService;
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
    @PatchMapping(path = "/api/users/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<MeResponse>> modifyProfileImage(
            @RequestPart("image") MultipartFile image) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        User user = userService.modifyProfileImage(currentUserId, image);
        return ResponseEntity.ok(DataResponse.from(toMeResponse(currentUserId, user)));
    }

    @Override
    @DeleteMapping("/api/users/me")
    public ResponseEntity<DataResponse<Void>> withdraw() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        authService.withdraw(currentUserId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    @Override
    @PatchMapping("/api/users/me/onboarding-complete")
    public ResponseEntity<DataResponse<OnboardingCompleteResponse>> completeOnboarding() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        userService.completeOnboarding(currentUserId);
        return ResponseEntity.ok(DataResponse.from(new OnboardingCompleteResponse(true)));
    }

    @Override
    @GetMapping("/api/users/me/opinions")
    public ResponseEntity<DataResponse<MyOpinionListResponse>> getMyOpinions(
            @RequestParam(required = false) Long bookId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.findCurrentUserId().orElse(null);
        return ResponseEntity.ok(DataResponse.from(UserMapper.toMyOpinionListResponse(
                opinionService.getMyOpinions(currentUserId, bookId, pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/users/me/likes")
    public ResponseEntity<DataResponse<LikedOpinionListResponse>> getLikedOpinions(
            @RequestParam(required = false) Long bookId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(UserMapper.toLikedOpinionListResponse(
                opinionService.getLikedOpinions(currentUserId, bookId, pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/users/me/filter-books")
    public ResponseEntity<DataResponse<FilterBooksResponse>> getFilterBooks(@RequestParam BookFilterType type) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        List<BookOptionProjection> books = type == BookFilterType.SPOILER
                ? passageService.getSpoilerBookOptions(currentUserId)
                : opinionService.getLikedBookOptions(currentUserId);
        return ResponseEntity.ok(DataResponse.from(UserMapper.toFilterBooksResponse(books)));
    }

    @Override
    @GetMapping("/api/users/me/passages")
    public ResponseEntity<DataResponse<MyPassageListResponse>> getMyPassages(
            @RequestParam(required = false) Long bookId,
            @RequestParam(defaultValue = "false") boolean spoilerOnly,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(DataResponse.from(UserMapper.toMyPassageListResponse(
                passageService.getMyPassages(currentUserId, bookId, spoilerOnly, pageable(page, size)))));
    }

    private MeResponse toMeResponse(Long userId, User user) {
        return UserMapper.toMeResponse(user, opinionService.getMyOpinionCount(userId));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
