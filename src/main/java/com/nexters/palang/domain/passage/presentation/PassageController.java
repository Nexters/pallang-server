package com.nexters.palang.domain.passage.presentation;

import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
import com.nexters.palang.domain.passage.application.PageNumbersResult;
import com.nexters.palang.domain.passage.application.PassageOcrService;
import com.nexters.palang.domain.passage.application.PassageService;
import com.nexters.palang.domain.passage.application.SimilarPassageFinder;
import com.nexters.palang.domain.passage.application.SimilarPassageProjection;
import com.nexters.palang.domain.passage.application.dto.OcrResultDto;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.presentation.docs.PassageControllerDocs;
import com.nexters.palang.domain.passage.presentation.request.PassageRequest;
import com.nexters.palang.domain.passage.presentation.response.PassageResponse;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PassageController implements PassageControllerDocs {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final PassageOcrService passageOcrService;
    private final SimilarPassageFinder similarPassageFinder;
    private final PassageService passageService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PostMapping(path = "/api/passages/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataResponse<PassageResponse.OcrRecognize> createOcrResult(@RequestPart("image") MultipartFile image) {
        List<OcrResultDto> blocks = passageOcrService.recognizeText(image);
        return DataResponse.from(PassageResponse.OcrRecognize.from(blocks));
    }

    @Override
    @PostMapping("/api/passages/similar-check")
    public DataResponse<PassageResponse.SimilarCandidates> checkSimilarPassages(
            @Valid @RequestBody PassageRequest.SimilarCheck request) {
        // 로그인 필요 엔드포인트: groupId가 있으면 그 모임 멤버인지까지 함께 확인한다(SimilarPassageFinder).
        Long currentUserId = currentUserProvider.getCurrentUserId();
        List<SimilarPassageProjection> candidates = similarPassageFinder.find(
                request.bookId(), request.pageNumber(), request.quotedText(), request.groupId(), currentUserId);
        return DataResponse.from(PassageResponse.SimilarCandidates.from(candidates));
    }

    @Override
    @GetMapping("/api/books/{bookId}/passages")
    public DataResponse<PassageResponse.PageNumbers> getPageNumbers(
            @PathVariable Long bookId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.findCurrentUserId().orElse(null);
        PageNumbersResult result = passageService.getPageNumbers(bookId, groupId, currentUserId, pageable(page, size));
        return DataResponse.from(PassageResponse.PageNumbers.from(result));
    }

    @Override
    @GetMapping("/api/books/{bookId}/pages/{page}/passages")
    public DataResponse<PassageResponse.PassagesByPage> getPassagesByPage(
            @PathVariable Long bookId,
            @PathVariable("page") int pageNumber,
            @RequestParam(required = false) Long groupId) {
        Long currentUserId = currentUserProvider.findCurrentUserId().orElse(null);
        List<Passage> passages = passageService.getPassagesByPage(bookId, groupId, currentUserId, pageNumber);
        Map<Long, List<DecorationMergeCandidate>> merged = passageService.getMergedDecorationsByPassageId(passages);
        return DataResponse.from(PassageResponse.PassagesByPage.from(passages, merged));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
