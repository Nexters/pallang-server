package com.nexters.palang.domain.opinion.presentation;

import com.nexters.palang.domain.opinion.application.OpinionLikeResult;
import com.nexters.palang.domain.opinion.application.OpinionLikeService;
import com.nexters.palang.domain.opinion.application.OpinionService;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionLikeResponse;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionResponse;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OpinionController implements OpinionApi {

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
    @PostMapping("/api/opinions/{opinionId}/like")
    public ResponseEntity<DataResponse<OpinionLikeResponse>> toggleOpinionLike(@PathVariable Long opinionId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        OpinionLikeResult result = opinionLikeService.toggleLike(currentUserId, opinionId);
        return ResponseEntity.ok(DataResponse.from(OpinionLikeResponse.from(result)));
    }
}
