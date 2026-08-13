package com.nexters.palang.domain.passage.infrastructure.ocr.clova;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.passage.infrastructure.ocr.OcrTextBlock;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClovaOcrClientTest {

    private final ClovaOcrClient client = new ClovaOcrClient(null, null);

    @Test
    @DisplayName("좌표(topY)가 줄 사이에서 겹쳐도 Clova가 준 원본 순서를 그대로 유지한다")
    void keepsOriginalFieldOrderEvenWhenTopYOverlapsAcrossLines() {
        // 사진이 살짝 기울어져 두 번째 줄 앞부분 단어의 topY가 첫 번째 줄 뒷부분 단어보다 작은 상황을 재현
        ClovaOcrResponse.Field line1Word1 = field("보아,", 10, 100, false);
        ClovaOcrResponse.Field line1Word2 = field("개인적인", 12, 150, false);
        ClovaOcrResponse.Field line1Word3 = field("원한에", 9, 200, true);
        ClovaOcrResponse.Field line2Word1 = field("의한", 11, 100, false);
        ClovaOcrResponse.Field line2Word2 = field("범행이", 13, 150, false);
        ClovaOcrResponse.Field line2Word3 = field("아닐까", 8, 200, true);
        List<ClovaOcrResponse.Field> originalOrder = List.of(
                line1Word1, line1Word2, line1Word3, line2Word1, line2Word2, line2Word3
        );

        List<OcrTextBlock> blocks = client.toOrderedTextBlocks(originalOrder);

        assertThat(blocks).extracting(OcrTextBlock::text)
                .containsExactly("보아,", "개인적인", "원한에", "의한", "범행이", "아닐까");
    }

    @Test
    @DisplayName("각 필드의 lineBreak 값을 그대로 전달한다")
    void preservesLineBreakFlag() {
        ClovaOcrResponse.Field word = field("보아,", 10, 100, true);

        List<OcrTextBlock> blocks = client.toOrderedTextBlocks(List.of(word));

        assertThat(blocks.get(0).lineBreak()).isTrue();
    }

    private ClovaOcrResponse.Field field(String text, double topY, double leftX, boolean lineBreak) {
        ClovaOcrResponse.Vertex topLeft = new ClovaOcrResponse.Vertex(leftX, topY);
        ClovaOcrResponse.BoundingPoly boundingPoly = new ClovaOcrResponse.BoundingPoly(List.of(topLeft));
        return new ClovaOcrResponse.Field("NORMAL", boundingPoly, text, 0.99, lineBreak);
    }
}
