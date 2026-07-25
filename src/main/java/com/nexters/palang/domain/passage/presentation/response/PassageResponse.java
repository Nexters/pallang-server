package com.nexters.palang.domain.passage.presentation.response;

import com.nexters.palang.domain.passage.application.dto.OcrResultDto;
import com.nexters.palang.domain.passage.domain.Passage;
import java.time.LocalDateTime;
import java.util.List;

public class PassageResponse {

    public record Detail(
            Long id,
            Long bookId,
            int pageNumber,
            String quotedText,
            boolean isSpoiler,
            LocalDateTime createdAt
    ) {
        public static Detail from(Passage passage) {
            return new Detail(
                    passage.getId(),
                    passage.getBook().getId(),
                    passage.getPageNumber(),
                    passage.getQuotedText(),
                    passage.isSpoiler(),
                    passage.getCreatedAt()
            );
        }
    }

    public record OcrRecognize(List<TextBlock> blocks) {
        public static OcrRecognize from(List<OcrResultDto> blocks) {
            List<TextBlock> textBlockResponses = blocks.stream()
                    .map(TextBlock::from)
                    .toList();
            return new OcrRecognize(textBlockResponses);
        }
    }

    public record TextBlock(String text, BoundingBox boundingBox, boolean lineBreak) {
        public static TextBlock from(OcrResultDto block) {
            return new TextBlock(block.text(), BoundingBox.from(block.boundingBox()), block.lineBreak());
        }
    }

    public record BoundingBox(List<Point> vertices) {
        public static BoundingBox from(OcrResultDto.BoundingBox boundingBox) {
            List<Point> vertices = boundingBox.vertices().stream()
                    .map(point -> new Point(point.x(), point.y()))
                    .toList();
            return new BoundingBox(vertices);
        }
    }

    public record Point(double x, double y) {
    }
}
