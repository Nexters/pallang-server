package com.nexters.palang.domain.passage.presentation.response;

import com.nexters.palang.domain.passage.application.SimilarPassageProjection;
import com.nexters.palang.domain.passage.application.dto.OcrResultDto;
import java.util.List;

public class PassageResponse {

    public record SimilarCandidates(List<SimilarCandidate> passages) {
        public static SimilarCandidates from(List<SimilarPassageProjection> projections) {
            List<SimilarCandidate> candidates = projections.stream()
                    .map(SimilarCandidate::from)
                    .toList();
            return new SimilarCandidates(candidates);
        }
    }

    public record SimilarCandidate(Long passageId, String quotedText, int pageNumber, long opinionCount) {
        public static SimilarCandidate from(SimilarPassageProjection projection) {
            return new SimilarCandidate(
                    projection.passageId(),
                    projection.quotedText(),
                    projection.pageNumber(),
                    projection.opinionCount()
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
