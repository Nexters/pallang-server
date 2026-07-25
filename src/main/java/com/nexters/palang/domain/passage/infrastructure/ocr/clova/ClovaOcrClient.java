package com.nexters.palang.domain.passage.infrastructure.ocr.clova;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.passage.common.error.PassageErrorCode;
import com.nexters.palang.domain.passage.infrastructure.ocr.OcrClient;
import com.nexters.palang.domain.passage.infrastructure.ocr.OcrTextBlock;
import com.nexters.palang.domain.passage.common.error.PassageException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ClovaOcrClient implements OcrClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ClovaOcrClient(ObjectMapper clovaObjectMapper, RestClient clovaOcrRestClient) {
        this.objectMapper = clovaObjectMapper;
        this.restClient = clovaOcrRestClient;
    }

    // 이미지 바이트 배열을 네이버 클로바 OCR API로 전송하여 텍스트 인식
    @Override
    public List<OcrTextBlock> recognize(byte[] imageBytes, String contentType) {
        String format = resolveFormat(contentType);
        String message = buildRequestMessage(format);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("message", message).contentType(MediaType.APPLICATION_JSON);
        bodyBuilder.part("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "image." + format;
            }
        });

        ClovaOcrResponse response = requestOcr(bodyBuilder);

        List<OcrTextBlock> blocks = response.images().get(0).fields().stream()
                .map(this::toTextBlock)
                // Clova는 필드를 읽기 순서로 보장하지 않으므로, 좌상단 좌표 기준으로 재정렬
                .sorted(Comparator.comparingDouble((OcrTextBlock block) -> block.boundingBox().topY())
                        .thenComparingDouble(block -> block.boundingBox().leftX()))
                .toList();

        if (blocks.isEmpty()) {
            throw new PassageException(PassageErrorCode.OCR_TEXT_NOT_FOUND);
        }

        return blocks;
    }

    // 클로바 OCR API에 HTTP POST 요청 보내기
    private ClovaOcrResponse requestOcr(MultipartBodyBuilder bodyBuilder) {
        try {
            ClovaOcrResponse response = restClient.post()
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(bodyBuilder.build())
                    .retrieve()
                    .body(ClovaOcrResponse.class);

            if (response == null || response.images().isEmpty()) {
                throw new PassageException(PassageErrorCode.OCR_REQUEST_FAILED);
            }
            return response;
        } catch (RestClientException e) {
            throw new PassageException(PassageErrorCode.OCR_REQUEST_FAILED);
        }
    }

    // 클로바 OCR 응답 필드 -> OcrTextBlock 객체 변환
    private OcrTextBlock toTextBlock(ClovaOcrResponse.Field field) {
        List<OcrTextBlock.Point> vertices = field.boundingPoly().vertices().stream()
                .map(vertex -> new OcrTextBlock.Point(vertex.x(), vertex.y()))
                .toList();
        return new OcrTextBlock(field.inferText(), new OcrTextBlock.BoundingBox(vertices), field.lineBreak());
    }

    // 클로바 OCR API 요청에 필요한 메타데이터(JSON) 생성
    private String buildRequestMessage(String format) {
        ClovaOcrRequest request = new ClovaOcrRequest(
                "V2",
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                List.of(new ClovaOcrRequest.Image(format, "image"))
        );
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new PassageException(PassageErrorCode.OCR_REQUEST_FAILED);
        }
    }

    // Content-Type에 따라 이미지 포맷 문자열 추출.
    private String resolveFormat(String contentType) {
        if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            return "png";
        }
        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            return "jpg";
        }
        // 지원 포맷(JPEG/PNG) 여부는 PassageOcrService에서 미리 검증
        throw new IllegalStateException("지원하지 않는 이미지 포맷입니다: " + contentType);
    }
}
