package com.nexters.palang.domain.passage.infrastructure.ocr.clova;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.passage.common.error.PassageErrorCode;
import com.nexters.palang.domain.passage.infrastructure.ocr.OcrClient;
import com.nexters.palang.domain.passage.infrastructure.ocr.OcrTextBlock;
import com.nexters.palang.domain.passage.common.error.PassageException;
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

        List<OcrTextBlock> blocks = toOrderedTextBlocks(response.images().get(0).fields());

        if (blocks.isEmpty()) {
            throw new PassageException(PassageErrorCode.OCR_TEXT_NOT_FOUND);
        }

        return blocks;
    }

    // Clova는 같은 줄의 단어들을 순서대로 반환하고 각 줄의 마지막 단어에 lineBreak=true를 표시한다.
    // 단어별 바운딩 박스 좌표(특히 topY)는 사진 기울기·글자 높이 차이로 흔들려 좌표 재정렬 시 줄이 뒤섞이므로,
    // 좌표로 다시 정렬하지 않고 Clova가 준 원본 순서를 그대로 신뢰한다.
    List<OcrTextBlock> toOrderedTextBlocks(List<ClovaOcrResponse.Field> fields) {
        return fields.stream()
                .map(this::toTextBlock)
                .toList();
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
