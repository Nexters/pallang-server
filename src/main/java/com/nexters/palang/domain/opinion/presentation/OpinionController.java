package com.nexters.palang.domain.opinion.presentation;

import com.nexters.palang.domain.opinion.application.OpinionLikeResult;
import com.nexters.palang.domain.opinion.application.OpinionLikeService;
import com.nexters.palang.domain.opinion.application.OpinionService;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionSortType;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionDetailResponse;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionLikeResponse;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionListResponse;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionResponse;
import com.nexters.palang.domain.opinion.presentation.dto.UpdateOpinionRequest;
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
public class OpinionController implements OpinionApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final OpinionService opinionService;
    private final OpinionLikeService opinionLikeService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PostMapping("/api/opinions")
    public ResponseEntity<DataResponse<OpinionResponse>> createOpinion(@Valid @RequestBody CreateOpinionRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Opinion opinion = opinionService.createOpinion(currentUserId, request);
        boolean merged = request.passageId() != null;
        return ResponseEntity.ok(DataResponse.from(OpinionResponse.from(opinion, merged)));
    }

    @Override
    @GetMapping("/api/passages/{passageId}/opinions")
    public ResponseEntity<DataResponse<OpinionListResponse>> getOpinions(
            @PathVariable Long passageId,
            @RequestParam(defaultValue = "LATEST") OpinionSortType sortType,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(DataResponse.from(
                OpinionListResponse.from(opinionService.getOpinions(passageId, sortType, pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/opinions/{opinionId}")
    public ResponseEntity<DataResponse<OpinionDetailResponse>> getOpinion(@PathVariable Long opinionId) {
        Opinion opinion = opinionService.getOpinion(opinionId);
        return ResponseEntity.ok(DataResponse.from(OpinionDetailResponse.from(opinion)));
    }

    @Override
    @PatchMapping("/api/opinions/{opinionId}")
    public ResponseEntity<DataResponse<OpinionDetailResponse>> modifyOpinion(
            @PathVariable Long opinionId,
            @RequestBody @Valid UpdateOpinionRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Opinion opinion = opinionService.modifyOpinion(opinionId, currentUserId, request);
        return ResponseEntity.ok(DataResponse.from(OpinionDetailResponse.from(opinion)));
    }

    @Override
    @DeleteMapping("/api/opinions/{opinionId}")
    public ResponseEntity<DataResponse<Void>> removeOpinion(@PathVariable Long opinionId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        opinionService.removeOpinion(opinionId, currentUserId);
        return ResponseEntity.ok(DataResponse.from(null));
    }

    @Override
    @PostMapping("/api/opinions/{opinionId}/like")
    public ResponseEntity<DataResponse<OpinionLikeResponse>> toggleOpinionLike(@PathVariable Long opinionId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        OpinionLikeResult result = opinionLikeService.toggleLike(currentUserId, opinionId);
        return ResponseEntity.ok(DataResponse.from(OpinionLikeResponse.from(result)));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
