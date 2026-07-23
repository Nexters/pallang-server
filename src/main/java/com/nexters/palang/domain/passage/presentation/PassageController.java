package com.nexters.palang.domain.passage.presentation;

import com.nexters.palang.domain.passage.application.PassageOcrService;
import com.nexters.palang.domain.passage.application.PassageService;
import com.nexters.palang.domain.passage.application.dto.OcrResultDto;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.presentation.request.PassageRequest;
import com.nexters.palang.domain.passage.presentation.response.PassageResponse;
import com.nexters.palang.global.common.response.DataResponse;
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
public class PassageController {

    private final PassageOcrService passageOcrService;
    private final PassageService passageService;

    @PostMapping(path = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataResponse<PassageResponse.OcrRecognize> createOcrResult(@RequestPart("image") MultipartFile image) {
        List<OcrResultDto> blocks = passageOcrService.recognizeText(image);
        return DataResponse.from(PassageResponse.OcrRecognize.from(blocks));
    }

    @PostMapping
    public DataResponse<PassageResponse.Detail> createPassage(@Valid @RequestBody PassageRequest.Create request) {
        Passage passage = passageService.addPassage(
                request.bookId(),
                request.creatorId(),
                request.pageNumber(),
                request.quotedText(),
                request.isSpoiler()
        );
        return DataResponse.from(PassageResponse.Detail.from(passage));
    }
}
