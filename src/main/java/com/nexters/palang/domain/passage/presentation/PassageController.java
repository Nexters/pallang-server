package com.nexters.palang.domain.passage.presentation;

import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
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
import org.springframework.data.domain.Page;
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
        // 조회 결과는 유저 무관하게 동일하지만, 인증 필요 엔드포인트이므로 로그인 여부만 확인한다.
        currentUserProvider.getCurrentUserId();
        List<SimilarPassageProjection> candidates = similarPassageFinder.find(
                request.bookId(), request.pageNumber(), request.quotedText());
        return DataResponse.from(PassageResponse.SimilarCandidates.from(candidates));
    }

    @Override
    @GetMapping("/api/books/{bookId}/passages")
    public DataResponse<PassageResponse.PageNumbers> getPageNumbers(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        Long currentUserId = currentUserProvider.findCurrentUserId().orElse(null);
        Page<Integer> pageNumbers = passageService.getVisiblePageNumbers(bookId, currentUserId, pageable(page, size));
        return DataResponse.from(PassageResponse.PageNumbers.from(pageNumbers));
    }

    @Override
    @GetMapping("/api/books/{bookId}/pages/{page}/passages")
    public DataResponse<PassageResponse.PassagesByPage> getPassagesByPage(
            @PathVariable Long bookId,
            @PathVariable("page") int pageNumber) {
        Long currentUserId = currentUserProvider.findCurrentUserId().orElse(null);
        List<Passage> passages = passageService.getVisiblePassagesByPage(bookId, pageNumber, currentUserId);
        Map<Long, List<DecorationMergeCandidate>> merged = passageService.getMergedDecorationsByPassageId(passages);
        return DataResponse.from(PassageResponse.PassagesByPage.from(passages, merged));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
