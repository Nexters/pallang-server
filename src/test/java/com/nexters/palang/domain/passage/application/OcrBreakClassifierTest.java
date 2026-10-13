package com.nexters.palang.domain.passage.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.passage.domain.OcrBreakType;
import com.nexters.palang.domain.passage.infrastructure.ocr.OcrTextBlock;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

    // ---- 실제 사진의 Clova OCR 응답으로 만든 골든 테스트 (src/test/resources/ocr) ----

    @Test
    @DisplayName("[실제 사진] 기울어진 산문은 꺾인 줄을 잇고 문단 끝에서만 줄을 바꾼다")
    void golden_proseTilted() throws IOException {
        // given
        List<OcrTextBlock> blocks = fixture("ocr/prose-tilted.tsv");

        // when
        List<OcrBreakType> breakTypes = OcrBreakClassifier.classify(blocks);

        // then: 산문은 LINE/PARAGRAPH 구분 없이 "끊었는지"만 본다.
        // "구 겨진", "구역 질"처럼 단어 안에서 꺾인 곳의 공백, OCR 오인식("및")은 분류기 범위 밖이다.
        assertThat(render(blocks, breakTypes, "\n")).isEqualTo(String.join("\n",
                "- 대답했다.",
                "결핍",
                "사랑은 외로움의 또 다른 이름일 뿐이다.",
                "\"결핍이야. 사랑이라고 발음하면 꼭 존귀한 영역에 속하는 것처럼 들리지만 기껏해야 갈증일 뿐이야.\"",
                "천장을 보던 수가 텁텁한 발음으로 말했다.",
                "\"같이 잠을 자는 것도, 서로의 살을 간지럽히는 것도, 언제 보자며 약속을 잡는 것도 죄다 결핍에서 비롯된 거야. "
                        + "결핍의 본질은 결국 외로움이고.\"",
                "나는 수의 발가벗은 가슴을 껴안으며 몸을 기댔다. 그는 구 겨진 얼굴로 우리의 모든 감정적 공유와 "
                        + "육체적 행위는 구역 질 나는 자기만족일 뿐이라며 중얼거렸다.",
                "\"수야, 옆으로 누워 봐. 알아, 오래전부터 알았어. 너를 및 기 전부터 알았어. 아니, 태어나기 전부터 알았어.\"",
                "나는 수의 몸을 옆으로 눕혀 낱낱이 끌어안았다. 등에"));
    }

    @Test
    @DisplayName("[실제 사진] 수평으로 찍은 시는 행마다 끊고 연 사이에 빈 줄을 둔다")
    void golden_verseFlat() throws IOException {
        // given
        List<OcrTextBlock> blocks = fixture("ocr/verse-flat.tsv");

        // when
        List<OcrBreakType> breakTypes = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(render(blocks, breakTypes, "\n\n")).isEqualTo(String.join("\n",
                "축, 생일",
                "",
                "이목구비는 대부분의 시간을 제멋대로 존재하다가",
                "오늘은 나를 위해제자리로 돌아온다",
                "",
                "그렇지만 나는 정돈하는 법을 배운 적이 없다.",
                "나는 내가 되어가고",
                "나는 나를",
                "좋아하고 싶어지지만",
                "이런 어색한 시간은 도대체 어디서 오는 것일까.",
                "",
                "나는 점점 갓 지은 밥 냄새에 미쳐간다.",
                "",
                "내 삶은 나보다 오래 지속될 것만 같다"));
    }

    @Test
    @DisplayName("[실제 사진] 기울어진 시도 행마다 끊고 연 사이에 빈 줄을 둔다")
    void golden_verseTilted() throws IOException {
        // given
        List<OcrTextBlock> blocks = fixture("ocr/verse-tilted.tsv");

        // when
        List<OcrBreakType> breakTypes = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(render(blocks, breakTypes, "\n\n")).isEqualTo(String.join("\n",
                "참담해지세요",
                "실패가 아니라 비굴함이라고 적어두세요",
                "",
                "여러분께 묻지 않겠습니다",
                "여러 겹의 따옴표 속에서 빠져나오는 방법을",
                "모르는 척하느라 애쓰지 마세요",
                "",
                "내 옆에서",
                "내 뒤에서"));
    }

    // 프론트가 할 조립을 그대로 흉내 낸다: NONE/WRAP → 공백, LINE → 줄바꿈, PARAGRAPH → paragraphSeparator.
    private String render(List<OcrTextBlock> blocks, List<OcrBreakType> breakTypes, String paragraphSeparator) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            text.append(blocks.get(i).text());
            text.append(switch (breakTypes.get(i)) {
                case NONE, WRAP -> " ";
                case LINE -> "\n";
                case PARAGRAPH -> paragraphSeparator;
            });
        }
        return text.toString().strip();
    }

    // 픽스처 한 줄: text<TAB>lineBreak(0|1)<TAB>x0 y0 x1 y1 x2 y2 x3 y3. '#'으로 시작하면 주석.
    private List<OcrTextBlock> fixture(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .map(this::parseFixtureLine)
                    .toList();
        }
    }

    private OcrTextBlock parseFixtureLine(String line) {
        String[] columns = line.split("\t");
        double[] coordinates = Arrays.stream(columns[2].trim().split(" ")).mapToDouble(Double::parseDouble).toArray();
        List<OcrTextBlock.Point> vertices = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) {
            vertices.add(new OcrTextBlock.Point(coordinates[i], coordinates[i + 1]));
        }
        return new OcrTextBlock(columns[0], new OcrTextBlock.BoundingBox(vertices), "1".equals(columns[1]));
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
