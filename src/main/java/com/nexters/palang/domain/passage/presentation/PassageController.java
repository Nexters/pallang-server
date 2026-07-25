package com.nexters.palang.domain.passage.presentation;

import com.nexters.palang.domain.passage.application.PassageOcrService;
import com.nexters.palang.domain.passage.application.SimilarPassageFinder;
import com.nexters.palang.domain.passage.application.SimilarPassageProjection;
import com.nexters.palang.domain.passage.application.dto.OcrResultDto;
import com.nexters.palang.domain.passage.presentation.docs.PassageControllerDocs;
import com.nexters.palang.domain.passage.presentation.request.PassageRequest;
import com.nexters.palang.domain.passage.presentation.response.PassageResponse;
import com.nexters.palang.global.common.response.DataResponse;
import com.nexters.palang.global.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/passages")
@RequiredArgsConstructor
public class PassageController implements PassageControllerDocs {

    private final PassageOcrService passageOcrService;
    private final SimilarPassageFinder similarPassageFinder;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(path = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataResponse<PassageResponse.OcrRecognize> createOcrResult(@RequestPart("image") MultipartFile image) {
        List<OcrResultDto> blocks = passageOcrService.recognizeText(image);
        return DataResponse.from(PassageResponse.OcrRecognize.from(blocks));
    }

    @PostMapping("/similar-check")
    public DataResponse<PassageResponse.SimilarCandidates> checkSimilarPassages(
            @Valid @RequestBody PassageRequest.SimilarCheck request) {
        // 조회 결과는 유저 무관하게 동일하지만, 인증 필요 엔드포인트이므로 로그인 여부만 확인한다.
        currentUserProvider.getCurrentUserId();
        List<SimilarPassageProjection> candidates = similarPassageFinder.find(
                request.bookId(), request.pageNumber(), request.quotedText());
        return DataResponse.from(PassageResponse.SimilarCandidates.from(candidates));
    }
}
