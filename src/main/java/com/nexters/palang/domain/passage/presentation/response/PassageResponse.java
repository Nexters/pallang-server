package com.nexters.palang.domain.passage.presentation.response;

import com.nexters.palang.domain.decoration.application.DecorationMergeCandidate;
import com.nexters.palang.domain.opinion.presentation.dto.OpinionResponse.DecorationResponse;
import com.nexters.palang.domain.passage.application.PageNumbersResult;
import com.nexters.palang.domain.passage.application.SimilarPassageProjection;
import com.nexters.palang.domain.passage.application.dto.OcrResultDto;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.global.common.response.PageInfo;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public class PassageResponse {

    public record SpoilerUpdate(
            @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
            @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean isSpoiler
    ) {
        public static SpoilerUpdate from(Passage passage) {
            return new SpoilerUpdate(passage.getId(), passage.isSpoiler());
        }
    }

    public record SimilarCandidates(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<SimilarCandidate> passages
    ) {
        public static SimilarCandidates from(List<SimilarPassageProjection> projections) {
            List<SimilarCandidate> candidates = projections.stream()
                    .map(SimilarCandidate::from)
                    .toList();
            return new SimilarCandidates(candidates);
        }
    }

    // 대목/흔적 보기 페이지 네비게이션(FR-VIEW-02): 발췌된 페이지 번호 목록 + 헤더용 책 정보.
    public record PageNumbers(
            @Schema(example = "이방인", requiredMode = Schema.RequiredMode.REQUIRED) String bookTitle,
            @Schema(example = "https://example.com/cover.jpg") String coverImageUrl,
            @ArraySchema(schema = @Schema(example = "3"), arraySchema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED))
            List<Integer> pageNumbers,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageInfo pageInfo
    ) {
        public static PageNumbers from(PageNumbersResult result) {
            return new PageNumbers(
                    result.book().getTitle(),
                    result.book().getCoverImageUrl(),
                    result.pageNumbers().getContent(),
                    PageInfo.from(result.pageNumbers()));
        }
    }

    // 대목 전환(FR-VIEW-03 2-b) + 꾸밈 병합 결과(FR-VIEW-03 꾸밈 병합 표시).
    public record PassagesByPage(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Detail> passages
    ) {
        public static PassagesByPage from(List<Passage> passages, Map<Long, List<DecorationMergeCandidate>> mergedByPassageId) {
            List<Detail> details = passages.stream()
                    .map(passage -> Detail.from(passage, mergedByPassageId.getOrDefault(passage.getId(), List.of())))
                    .toList();
            return new PassagesByPage(details);
        }

        // 스포일러 대목(isSpoiler=true)도 quotedText/decorations를 그대로 내려준다.
        // 블러 처리 + [버튼] 눌러야 확인 가능한 화면(FR-VIEW-03 스포일러 처리)은 즉시 반응해야 하는 순수 UI 동작이라
        // 프론트가 isSpoiler 플래그만 보고 클라이언트에서 처리한다 (서버 왕복 없이 버튼 클릭 즉시 확인 가능).
        public record Detail(
                @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
                @Schema(example = "87", requiredMode = Schema.RequiredMode.REQUIRED) int pageNumber,
                @Schema(example = "우리는 모두 이야기를 찾아 헤맨다.", requiredMode = Schema.RequiredMode.REQUIRED) String quotedText,
                @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean isSpoiler,
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<DecorationResponse> decorations
        ) {
            public static Detail from(Passage passage, List<DecorationMergeCandidate> merged) {
                List<DecorationResponse> decorations = merged.stream()
                        .map(candidate -> new DecorationResponse(
                                candidate.decorationId(), candidate.startOffset(), candidate.endOffset(),
                                candidate.effectType(), candidate.color()))
                        .toList();
                return new Detail(passage.getId(), passage.getPageNumber(), passage.getQuotedText(),
                        passage.isSpoiler(), decorations);
            }
        }
    }

    public record SimilarCandidate(
            @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long passageId,
            @Schema(example = "우리는 모두 이야기를 찾아 헤맨다.", requiredMode = Schema.RequiredMode.REQUIRED) String quotedText,
            @Schema(example = "87", requiredMode = Schema.RequiredMode.REQUIRED) int pageNumber,
            @Schema(example = "4", requiredMode = Schema.RequiredMode.REQUIRED) long opinionCount
    ) {
        public static SimilarCandidate from(SimilarPassageProjection projection) {
            return new SimilarCandidate(
                    projection.passageId(),
                    projection.quotedText(),
                    projection.pageNumber(),
                    projection.opinionCount()
            );
        }
    }

    public record OcrRecognize(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<TextBlock> blocks
    ) {
        public static OcrRecognize from(List<OcrResultDto> blocks) {
            List<TextBlock> textBlockResponses = blocks.stream()
                    .map(TextBlock::from)
                    .toList();
            return new OcrRecognize(textBlockResponses);
        }
    }

    public record TextBlock(
            @Schema(example = "우리는 모두 이야기를 찾아 헤맨다.", requiredMode = Schema.RequiredMode.REQUIRED) String text,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BoundingBox boundingBox,
            @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED) boolean lineBreak
    ) {
        public static TextBlock from(OcrResultDto block) {
            return new TextBlock(block.text(), BoundingBox.from(block.boundingBox()), block.lineBreak());
        }
    }

    public record BoundingBox(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Point> vertices
    ) {
        public static BoundingBox from(OcrResultDto.BoundingBox boundingBox) {
            List<Point> vertices = boundingBox.vertices().stream()
                    .map(point -> new Point(point.x(), point.y()))
                    .toList();
            return new BoundingBox(vertices);
        }
    }

    public record Point(
            @Schema(example = "120.5", requiredMode = Schema.RequiredMode.REQUIRED) double x,
            @Schema(example = "48.0", requiredMode = Schema.RequiredMode.REQUIRED) double y
    ) {
    }
}
