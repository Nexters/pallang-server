package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.passage.application.dto.OcrResultDto;
import com.nexters.palang.domain.passage.common.error.PassageErrorCode;
import com.nexters.palang.domain.passage.common.error.PassageException;
import com.nexters.palang.domain.passage.infrastructure.ocr.OcrClient;
import com.nexters.palang.domain.passage.infrastructure.ocr.OcrTextBlock;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PassageOcrService {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE
    );

    private final OcrClient ocrClient;

    // 업로드된 이미지에서 텍스트를 추출(OCR)하여 반환합니다.
    public List<OcrResultDto> recognizeText(MultipartFile image) {
        if (image == null || image.isEmpty() || !SUPPORTED_CONTENT_TYPES.contains(image.getContentType())) {
            throw new PassageException(PassageErrorCode.INVALID_IMAGE_FILE);
        }

        try {
            List<OcrTextBlock> blocks = ocrClient.recognize(image.getBytes(), image.getContentType());
            return blocks.stream()
                    .map(this::toResultDto)
                    .toList();
        } catch (IOException e) {
            throw new PassageException(PassageErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private OcrResultDto toResultDto(OcrTextBlock block) {
        List<OcrResultDto.Point> points = block.boundingBox().vertices().stream()
                .map(p -> new OcrResultDto.Point(p.x(), p.y()))
                .toList();
        return new OcrResultDto(
                block.text(),
                new OcrResultDto.BoundingBox(points),
                block.lineBreak()
        );
    }
}
