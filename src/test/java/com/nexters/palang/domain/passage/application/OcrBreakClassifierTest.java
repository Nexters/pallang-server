package com.nexters.palang.domain.passage.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.passage.application.OcrBreakClassifier.BlockLayout;
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
    private static final String INDENT = "\u3000";

    @Test
    @DisplayName("양쪽 정렬된 산문은 꽉 찬 줄을 잇고, 짧게 끝나거나 다음 줄이 들여쓰기인 곳을 문단 끝으로 본다")
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
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, layouts)).containsExactly(
                OcrBreakType.WRAP, OcrBreakType.WRAP, OcrBreakType.PARAGRAPH,
                OcrBreakType.WRAP, OcrBreakType.PARAGRAPH, OcrBreakType.WRAP, OcrBreakType.WRAP);
        assertThat(midLineBreakTypes(blocks, layouts)).containsOnly(OcrBreakType.NONE);
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
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, layouts)).containsExactly(
                OcrBreakType.LINE, OcrBreakType.LINE, OcrBreakType.BLANK_LINE,
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
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, layouts)).containsOnly(OcrBreakType.WRAP);
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
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, layouts)).containsExactly(
                OcrBreakType.WRAP, OcrBreakType.WRAP, OcrBreakType.WRAP, OcrBreakType.WRAP,
                OcrBreakType.LINE, OcrBreakType.LINE, OcrBreakType.LINE, OcrBreakType.LINE);
    }

    @Test
    @DisplayName("한 줄짜리 대화문이 연속돼도 가운데 줄의 들여쓰기를 놓치지 않고 산문으로 판정한다")
    void consecutiveDialogue_detectsEveryIndent() {
        // given: 들여쓰기 = 왼쪽 끝 120 (여백 100보다 한 글자 들어감)
        List<OcrTextBlock> blocks = page(
                line(120, 600, 0),   // 서술 문단 시작, 꽉 참
                line(100, 200, 1),   // 서술 문단 끝
                line(120, 200, 2),   // "왜?"
                line(120, 220, 3),   // "그냥."  ← 위아래가 모두 들여쓴 줄
                line(120, 210, 4),   // "뭐야."  ← 위아래가 모두 들여쓴 줄
                line(120, 600, 5),   // 서술 문단 시작, 꽉 참
                line(100, 250, 6)    // 서술 문단 끝
        );

        // when
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then: 산문으로 판정돼 꽉 찬 서술 줄은 잇고, 대화문 줄 끝은 모두 문단 끝으로 끊는다.
        assertThat(lineEndBreakTypes(blocks, layouts)).containsExactly(
                OcrBreakType.WRAP, OcrBreakType.PARAGRAPH, OcrBreakType.PARAGRAPH,
                OcrBreakType.PARAGRAPH, OcrBreakType.PARAGRAPH, OcrBreakType.WRAP, OcrBreakType.PARAGRAPH);
    }

    @Test
    @DisplayName("들여쓰기로 시작하는 줄은 사진의 첫 줄이어도 첫 단어에 indented를 표시한다")
    void indentedLine_markedEvenOnFirstLine() {
        // given: 첫 줄(앞 줄이 없음)과 4번째 줄이 들여쓰기
        List<OcrTextBlock> blocks = page(
                line(120, 600, 0),
                line(100, 600, 1),
                line(100, 300, 2),
                line(120, 600, 3),
                line(100, 600, 4)
        );

        // when
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then: 줄의 첫 단어에만 표시된다
        assertThat(IntStream.range(0, blocks.size()).filter(i -> !blocks.get(i).lineBreak())
                .mapToObj(i -> layouts.get(i).indented()).toList())
                .containsExactly(true, false, false, true, false);
        assertThat(IntStream.range(0, blocks.size()).filter(i -> blocks.get(i).lineBreak())
                .mapToObj(i -> layouts.get(i).indented()).toList())
                .containsOnly(false);
    }

    @Test
    @DisplayName("줄 중간 단어에 좌표가 없어도 나머지 판정은 그대로다")
    void missingVerticesInMiddleWord_ignored() {
        // given: 두 번째 줄 가운데 단어의 바운딩 박스가 비어 있다
        List<OcrTextBlock> blocks = page(
                line(120, 600, 0),
                new OcrTextBlock[]{
                        word("앞", 100, 300, LINE_PITCH, false),
                        wordWithoutBox("중", false),
                        word("뒤", 310, 600, LINE_PITCH, true)},
                line(100, 400, 2),
                line(120, 600, 3),
                line(100, 600, 4)
        );

        // when
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, layouts)).containsExactly(
                OcrBreakType.WRAP, OcrBreakType.WRAP, OcrBreakType.PARAGRAPH, OcrBreakType.WRAP, OcrBreakType.WRAP);
        assertThat(layouts.get(3)).isEqualTo(new BlockLayout(OcrBreakType.NONE, false));
    }

    @Test
    @DisplayName("줄의 첫·마지막 단어에 좌표가 없으면 그 줄의 들여쓰기·꽉 참 판정만 건너뛰고 줄 끝은 끊는다")
    void missingVerticesAtLineEdge_skipsOnlyThatJudgement() {
        // given: 두 번째 줄은 마지막 단어, 세 번째 줄은 첫 단어의 바운딩 박스가 비어 있다
        List<OcrTextBlock> blocks = page(
                line(120, 600, 0),
                new OcrTextBlock[]{
                        word("앞", 100, 345, LINE_PITCH, false),
                        wordWithoutBox("뒤", true)},
                new OcrTextBlock[]{
                        wordWithoutBox("앞", false),
                        word("뒤", 355, 600, LINE_PITCH * 2, true)},
                line(100, 600, 3),
                line(100, 300, 4)
        );

        // when
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then: 오른쪽 끝을 모르는 줄은 이어 붙이지 않고 끊는다. 왼쪽 끝을 모르는 줄은 들여쓰기로 보지 않는다.
        assertThat(lineEndBreakTypes(blocks, layouts)).containsExactly(
                OcrBreakType.WRAP, OcrBreakType.PARAGRAPH, OcrBreakType.WRAP, OcrBreakType.WRAP, OcrBreakType.PARAGRAPH);
        assertThat(layouts.get(4).indented()).isFalse();
    }

    @Test
    @DisplayName("줄 전체에 좌표가 없으면 그 줄은 판정에서 빼고 줄 끝을 끊는다")
    void missingVerticesInWholeLine_breaksLine() {
        // given: 세 번째 줄의 모든 단어에 바운딩 박스가 없다. 본문은 사진 왼쪽 끝(x=0) 근처에서 시작한다.
        // (좌표 없는 줄은 판정에서 명시적으로 뺀다. 0 좌표로 계산하면 폭 0인 줄이 되어 지금은 우연히 단 묶기에서 빠지지만,
        // 그 우연에 기대지 않도록 동작을 고정한다)
        List<OcrTextBlock> blocks = page(
                line(20, 500, 0),
                line(0, 500, 1),
                new OcrTextBlock[]{wordWithoutBox("앞", false), wordWithoutBox("뒤", true)},
                line(0, 500, 3),
                line(20, 500, 4),
                line(0, 200, 5)
        );

        // when
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then: 좌표 없는 줄 끝은 끊고, 나머지 줄의 들여쓰기·이어 붙이기 판정은 흐트러지지 않는다.
        assertThat(layouts.get(4)).isEqualTo(new BlockLayout(OcrBreakType.NONE, false));
        assertThat(layouts.get(5)).isEqualTo(new BlockLayout(OcrBreakType.LINE, false));
        assertThat(IntStream.range(0, blocks.size()).filter(i -> !blocks.get(i).lineBreak())
                .mapToObj(i -> layouts.get(i).indented()).toList())
                .containsExactly(true, false, false, false, true, false);
        assertThat(layouts.get(7).breakType()).isEqualTo(OcrBreakType.PARAGRAPH);
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
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(lineEndBreakTypes(blocks, layouts)).containsExactly(OcrBreakType.LINE, OcrBreakType.LINE);
    }

    // ---- 실제 사진의 Clova OCR 응답으로 만든 골든 테스트 (src/test/resources/ocr) ----

    @Test
    @DisplayName("[실제 사진] 기울어진 산문은 꺾인 줄을 잇고 문단 끝에서만 줄을 바꾼다")
    void golden_proseTilted() throws IOException {
        // given
        List<OcrTextBlock> blocks = fixture("ocr/prose-tilted.tsv");

        // when
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then: 문단 끝은 줄바꿈 + 들여쓰기, 제목 아래 빈 공간만 빈 줄이다.
        // 제목과 첫 문단도 책에서 다른 문단 시작 줄과 같은 위치(한 글자 안쪽)에 인쇄돼 있어 들여쓰기로 나온다.
        // "구 겨진", "구역 질"처럼 단어 안에서 꺾인 곳의 공백, OCR 오인식("및")은 분류기 범위 밖이다.
        assertThat(render(blocks, layouts)).isEqualTo(String.join("\n",
                "- 대답했다.",
                INDENT + "결핍",
                "",
                INDENT + "사랑은 외로움의 또 다른 이름일 뿐이다.",
                INDENT + "\"결핍이야. 사랑이라고 발음하면 꼭 존귀한 영역에 속하는 것처럼 들리지만 기껏해야 갈증일 뿐이야.\"",
                INDENT + "천장을 보던 수가 텁텁한 발음으로 말했다.",
                INDENT + "\"같이 잠을 자는 것도, 서로의 살을 간지럽히는 것도, 언제 보자며 약속을 잡는 것도 죄다 결핍에서 비롯된 거야. "
                        + "결핍의 본질은 결국 외로움이고.\"",
                INDENT + "나는 수의 발가벗은 가슴을 껴안으며 몸을 기댔다. 그는 구 겨진 얼굴로 우리의 모든 감정적 공유와 "
                        + "육체적 행위는 구역 질 나는 자기만족일 뿐이라며 중얼거렸다.",
                INDENT + "\"수야, 옆으로 누워 봐. 알아, 오래전부터 알았어. 너를 및 기 전부터 알았어. 아니, 태어나기 전부터 알았어.\"",
                INDENT + "나는 수의 몸을 옆으로 눕혀 낱낱이 끌어안았다. 등에"));
    }

    @Test
    @DisplayName("[실제 사진] 원근 왜곡으로 여백이 줄마다 밀려도 꽉 찬 줄을 들여쓰기로 오판하지 않는다")
    void golden_prosePerspective() throws IOException {
        // given: 아래로 갈수록 글자가 커지고 왼쪽 여백이 줄마다 약 4~10px씩 밀린다. 맨 아래는 다른 영역 조각.
        List<OcrTextBlock> blocks = fixture("ocr/prose-perspective.tsv");

        // when
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then: "소비 / 의"처럼 꽉 찬 줄도 이어야 하고, 앞 줄이 없는 첫 줄의 들여쓰기("디드로의")도 살린다. ("것 까지", "어울리 는", "연 쇄"의 공백은 분류기 범위 밖)
        assertThat(render(blocks, layouts)).isEqualTo(String.join("\n",
                INDENT + "디드로의 행동이 특이한 것은 아니다. 이렇게 하나를 사면 다른 것 까지 사게 되는 경향은 흔하다. "
                        + "이런 행동을 가리켜 디드로 효과라고 한다. 디드로 효과는 새로운 것을 사게 되면 추가 구매가 일어나 "
                        + "소비 의 소용돌이가 생겨나는 것을 말한다.14",
                INDENT + "이런 광경은 어디서나 발견할 수 있다. 옷을 사면 그 옷에 어울리 는 신발과 귀걸이를 새로 사야 한다. "
                        + "소파를 사면 갑자기 거실 전체의 조화를 위해 다른 가구들을 사야 할 것 같다. 자녀에게 장난감을 사 "
                        + "주면 자신을 위해서도 뭔가 사야 할 것 같은 기분이 든다. 소비의 연 쇄 반응이다.",
                INDENT + "( 따르다 우리는 종종 막 끝마친"));
    }

    @Test
    @DisplayName("[실제 사진] 수평으로 찍은 시는 행마다 끊고 연 사이에 빈 줄을 둔다")
    void golden_verseFlat() throws IOException {
        // given
        List<OcrTextBlock> blocks = fixture("ocr/verse-flat.tsv");

        // when
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(render(blocks, layouts)).isEqualTo(String.join("\n",
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
        List<BlockLayout> layouts = OcrBreakClassifier.classify(blocks);

        // then
        assertThat(render(blocks, layouts)).isEqualTo(String.join("\n",
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

    // 프론트 조립 규칙(기획 확정안: 문단 줄바꿈 한 번 + 들여쓰기)을 그대로 흉내 낸다.
    // indented → 단어 앞에 전각 공백, NONE/WRAP → 공백, LINE/PARAGRAPH → 줄바꿈, BLANK_LINE → 빈 줄.
    private String render(List<OcrTextBlock> blocks, List<BlockLayout> layouts) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            if (layouts.get(i).indented()) {
                text.append(INDENT);
            }
            text.append(blocks.get(i).text());
            text.append(switch (layouts.get(i).breakType()) {
                case NONE, WRAP -> " ";
                case LINE, PARAGRAPH -> "\n";
                case BLANK_LINE -> "\n\n";
            });
        }
        // 첫 줄의 들여쓰기(전각 공백)는 남겨야 하므로 뒤쪽만 지운다.
        return text.toString().stripTrailing();
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

    private OcrTextBlock wordWithoutBox(String text, boolean lineBreak) {
        return new OcrTextBlock(text, new OcrTextBlock.BoundingBox(List.of()), lineBreak);
    }

    private List<OcrTextBlock> page(OcrTextBlock[]... lines) {
        List<OcrTextBlock> blocks = new ArrayList<>();
        for (OcrTextBlock[] line : lines) {
            blocks.addAll(List.of(line));
        }
        return blocks;
    }

    private List<OcrBreakType> lineEndBreakTypes(List<OcrTextBlock> blocks, List<BlockLayout> layouts) {
        return IntStream.range(0, blocks.size())
                .filter(i -> blocks.get(i).lineBreak())
                .mapToObj(i -> layouts.get(i).breakType())
                .toList();
    }

    private List<OcrBreakType> midLineBreakTypes(List<OcrTextBlock> blocks, List<BlockLayout> layouts) {
        return IntStream.range(0, blocks.size())
                .filter(i -> !blocks.get(i).lineBreak())
                .mapToObj(i -> layouts.get(i).breakType())
                .toList();
    }
}
