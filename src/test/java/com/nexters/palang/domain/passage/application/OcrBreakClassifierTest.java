package com.nexters.palang.domain.passage.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.passage.domain.OcrBreakType;
import com.nexters.palang.domain.passage.infrastructure.ocr.OcrTextBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OcrBreakClassifierTest {

    private static final double CHAR_HEIGHT = 20;
    private static final double LINE_PITCH = 30;

    @Test
    @DisplayName("양쪽 정렬된 산문은 꽉 찬 줄을 잇고, 짧게 끝난 뒤 들여쓰기로 시작하는 줄 앞에서 문단을 나눈다")
    void prose_wrapsFullLinesAndBreaksParagraphs() {
        // given
        List<OcrTextBlock> blocks = page(
                line(100, 600, 0),
                line(100, 600, 1),
                line(100, 400, 2),   // 문단 끝: 짧게 끝남
                line(125, 600, 3),   // 들여쓰기로 시작
                line(100, 600, 4),   // 꽉 찼지만 다음 줄이 들여쓰기
                line(125, 600, 5),
                line(100, 600, 6)    // 마지막 줄, 꽉 참 → 다음 페이지로 이어짐
        );

        // when
        List<OcrBreakType> breakTypes = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, breakTypes)).containsExactly(
                OcrBreakType.WRAP, OcrBreakType.WRAP, OcrBreakType.PARAGRAPH,
                OcrBreakType.WRAP, OcrBreakType.LINE, OcrBreakType.WRAP, OcrBreakType.WRAP);
        assertThat(midLineBreakTypes(blocks, breakTypes)).containsOnly(OcrBreakType.NONE);
    }

    @Test
    @DisplayName("줄 길이가 들쭉날쭉한 시는 행마다 끊고, 연 사이 빈 줄에서 연을 나눈다")
    void verse_breaksEveryLineAndStanza() {
        // given
        List<OcrTextBlock> blocks = page(
                line(100, 300, 0),
                line(100, 450, 1),
                line(100, 250, 2),
                line(100, 380, 4),   // 한 줄 비우고 다음 연
                line(100, 320, 5)
        );

        // when
        List<OcrBreakType> breakTypes = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, breakTypes)).containsExactly(
                OcrBreakType.LINE, OcrBreakType.LINE, OcrBreakType.PARAGRAPH,
                OcrBreakType.LINE, OcrBreakType.LINE);
    }

    @Test
    @DisplayName("사진이 기울어 줄마다 여백이 조금씩 밀려도 꽉 찬 산문 줄은 잇는다")
    void tiltedProse_stillWraps() {
        // given: 줄마다 오른쪽으로 2px씩 밀림
        List<OcrTextBlock> blocks = page(IntStream.range(0, 10)
                .mapToObj(i -> line(100 + i * 2, 600 + i * 2, i))
                .toArray(OcrTextBlock[][]::new));

        // when
        List<OcrBreakType> breakTypes = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, breakTypes)).containsOnly(OcrBreakType.WRAP);
    }

    @Test
    @DisplayName("양쪽 페이지가 함께 찍히면 페이지별로 여백을 따로 계산한다")
    void twoPages_classifiedPerColumn() {
        // given: 왼쪽 페이지는 산문, 오른쪽 페이지는 시
        List<OcrTextBlock> blocks = page(
                line(100, 600, 0),
                line(100, 600, 1),
                line(100, 600, 2),
                line(100, 600, 3),
                line(700, 900, 0),
                line(700, 1000, 1),
                line(700, 850, 2),
                line(700, 950, 3)
        );

        // when
        List<OcrBreakType> breakTypes = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, breakTypes)).containsExactly(
                OcrBreakType.WRAP, OcrBreakType.WRAP, OcrBreakType.WRAP, OcrBreakType.WRAP,
                OcrBreakType.LINE, OcrBreakType.LINE, OcrBreakType.LINE, OcrBreakType.LINE);
    }

    @Test
    @DisplayName("줄이 너무 적어 산문인지 판단할 수 없으면 줄마다 끊는다")
    void fewLines_breakEveryLine() {
        // given
        List<OcrTextBlock> blocks = page(
                line(100, 600, 0),
                line(100, 600, 1)
        );

        // when
        List<OcrBreakType> breakTypes = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, breakTypes)).containsExactly(OcrBreakType.LINE, OcrBreakType.LINE);
    }

    // 한 줄을 단어 두 개로 만든다. 두 번째 단어가 줄 끝(lineBreak=true)이다.
    private OcrTextBlock[] line(double left, double right, int row) {
        double top = row * LINE_PITCH;
        double middle = (left + right) / 2;
        return new OcrTextBlock[]{
                word("앞", left, middle - 5, top, false),
                word("뒤", middle + 5, right, top, true)
        };
    }

    private OcrTextBlock word(String text, double left, double right, double top, boolean lineBreak) {
        double bottom = top + CHAR_HEIGHT;
        OcrTextBlock.BoundingBox box = new OcrTextBlock.BoundingBox(List.of(
                new OcrTextBlock.Point(left, top), new OcrTextBlock.Point(right, top),
                new OcrTextBlock.Point(right, bottom), new OcrTextBlock.Point(left, bottom)));
        return new OcrTextBlock(text, box, lineBreak);
    }

    private List<OcrTextBlock> page(OcrTextBlock[]... lines) {
        List<OcrTextBlock> blocks = new ArrayList<>();
        for (OcrTextBlock[] line : lines) {
            blocks.addAll(List.of(line));
        }
        return blocks;
    }

    private List<OcrBreakType> lineEndBreakTypes(List<OcrTextBlock> blocks, List<OcrBreakType> breakTypes) {
        return IntStream.range(0, blocks.size())
                .filter(i -> blocks.get(i).lineBreak())
                .mapToObj(breakTypes::get)
                .toList();
    }

    private List<OcrBreakType> midLineBreakTypes(List<OcrTextBlock> blocks, List<OcrBreakType> breakTypes) {
        return IntStream.range(0, blocks.size())
                .filter(i -> !blocks.get(i).lineBreak())
                .mapToObj(breakTypes::get)
                .toList();
    }
}
