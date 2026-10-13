package com.nexters.palang.domain.passage.application;

import com.nexters.palang.domain.passage.domain.OcrBreakType;
import com.nexters.palang.domain.passage.infrastructure.ocr.OcrTextBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// OCR 단어 블록마다 뒤에 오는 끊김의 종류(OcrBreakType)를 바운딩 박스 좌표로 추정한다.
// Clova는 인쇄된 줄의 끝에만 lineBreak=true를 주므로, 그 줄 끝이 "지면 폭 때문에 꺾인 줄(WRAP)"인지
// "원문에서 끊는 줄(LINE/PARAGRAPH)"인지는 줄 길이·들여쓰기·줄 간격으로 판단한다.
// 모든 기준값은 사진 해상도·촬영 거리에 흔들리지 않도록 픽셀이 아니라 글자 높이(h) 대비 비율로 둔다.
// 사진이 기울면 줄마다 여백이 밀리고 글자 높이가 부풀려 보이므로, 판정 전에 좌표를 기울기만큼 되돌린다.
public final class OcrBreakClassifier {

    // 줄 끝이 주변 줄의 오른쪽 끝보다 글자 1개 이상 모자라면 짧게 끝난 줄로 본다.
    private static final double SHORT_LINE_RATIO = 1.0;
    // 줄 시작이 주변 줄의 왼쪽 끝보다 글자 0.7개 이상 들어가 있으면 들여쓰기로 본다.
    private static final double INDENT_RATIO = 0.7;
    // 줄 사이 간격이 평소보다 글자 0.8개 이상 벌어지면 빈 줄(문단·연 구분)로 본다.
    private static final double PARAGRAPH_GAP_RATIO = 0.8;
    // 단(컬럼)의 줄 중 이 비율 이상이 오른쪽 끝까지 차 있어야 양쪽 정렬된 산문으로 본다. 아니면 운문(시)으로 본다.
    private static final double PROSE_FULL_LINE_RATIO = 0.5;
    // 줄 수가 이보다 적으면 산문인지 판단할 근거가 없으므로 운문처럼 줄마다 끊는다(잘못 끊는 쪽이 복구하기 쉽다).
    private static final int MIN_PROSE_LINES = 3;
    // 사진 기울기·책 휘어짐 때문에 단 전체의 여백이 일정하지 않으므로, 앞뒤 몇 줄만 보고 여백 기준을 잡는다.
    private static final int NEIGHBOR_WINDOW = 3;
    // 두 줄의 가로 범위가 이 비율 이상 겹치면 같은 단(같은 페이지)으로 묶는다.
    private static final double COLUMN_OVERLAP_RATIO = 0.5;

    private OcrBreakClassifier() {
    }

    public static List<OcrBreakType> classify(List<OcrTextBlock> blocks) {
        OcrBreakType[] breakTypes = new OcrBreakType[blocks.size()];
        Arrays.fill(breakTypes, OcrBreakType.NONE);

        for (List<Line> column : groupIntoColumns(groupIntoLines(deskew(blocks)))) {
            classifyColumn(column, breakTypes);
        }
        return List.of(breakTypes);
    }

    // Clova 바운딩 박스는 글자 방향을 따라 회전된 사각형(좌상→우상→우하→좌하)이라 윗변 각도가 곧 그 단어의 기울기다.
    // 단어별 각도의 중앙값을 사진 기울기로 보고 모든 좌표를 반대로 돌린다. (다른 페이지 조각처럼 각도가 튀는 단어는 중앙값이 무시한다)
    private static List<OcrTextBlock> deskew(List<OcrTextBlock> blocks) {
        List<Double> angles = blocks.stream()
                .map(block -> block.boundingBox().vertices())
                .filter(vertices -> vertices.size() >= 2)
                .map(vertices -> Math.atan2(vertices.get(1).y() - vertices.get(0).y(), vertices.get(1).x() - vertices.get(0).x()))
                .toList();
        double angle = median(angles);
        if (angle == 0) {
            return blocks;
        }
        double cos = Math.cos(-angle);
        double sin = Math.sin(-angle);
        return blocks.stream()
                .map(block -> {
                    List<OcrTextBlock.Point> rotated = block.boundingBox().vertices().stream()
                            .map(p -> new OcrTextBlock.Point(p.x() * cos - p.y() * sin, p.x() * sin + p.y() * cos))
                            .toList();
                    return new OcrTextBlock(block.text(), new OcrTextBlock.BoundingBox(rotated), block.lineBreak());
                })
                .toList();
    }

    // Clova가 준 순서대로 lineBreak=true인 단어까지를 한 줄로 묶는다. 마지막 단어는 lineBreak가 없어도 줄 끝으로 본다.
    private static List<Line> groupIntoLines(List<OcrTextBlock> blocks) {
        List<Line> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).lineBreak() || i == blocks.size() - 1) {
                lines.add(Line.of(blocks, start, i));
                start = i + 1;
            }
        }
        return lines;
    }

    // 양쪽 페이지가 함께 찍힌 사진에서 여백 기준이 섞이지 않도록, 가로 범위가 겹치는 줄끼리 같은 단으로 묶는다.
    // 단 안의 줄 순서는 Clova가 준 순서를 그대로 유지한다.
    private static List<List<Line>> groupIntoColumns(List<Line> lines) {
        List<List<Line>> columns = new ArrayList<>();
        List<double[]> spans = new ArrayList<>();
        for (Line line : lines) {
            int columnIndex = findOverlappingColumn(spans, line);
            if (columnIndex < 0) {
                columns.add(new ArrayList<>(List.of(line)));
                spans.add(new double[]{line.left(), line.right()});
                continue;
            }
            columns.get(columnIndex).add(line);
            double[] span = spans.get(columnIndex);
            span[0] = Math.min(span[0], line.left());
            span[1] = Math.max(span[1], line.right());
        }
        return columns;
    }

    private static int findOverlappingColumn(List<double[]> spans, Line line) {
        for (int i = 0; i < spans.size(); i++) {
            double[] span = spans.get(i);
            double overlap = Math.min(span[1], line.right()) - Math.max(span[0], line.left());
            double shorterWidth = Math.min(span[1] - span[0], line.right() - line.left());
            if (overlap > 0 && overlap >= shorterWidth * COLUMN_OVERLAP_RATIO) {
                return i;
            }
        }
        return -1;
    }

    private static void classifyColumn(List<Line> column, OcrBreakType[] breakTypes) {
        double charHeight = median(column.stream().flatMap(line -> line.wordHeights().stream()).toList());
        double usualPitch = median(linePitches(column));

        boolean[] fullLines = new boolean[column.size()];
        int fullLineCount = 0;
        for (int i = 0; i < column.size(); i++) {
            fullLines[i] = neighborRight(column, i) - column.get(i).right() < SHORT_LINE_RATIO * charHeight;
            // 단의 마지막 줄은 문단 끝일 가능성이 커서 산문 판정 비율에서 뺀다.
            if (fullLines[i] && i < column.size() - 1) {
                fullLineCount++;
            }
        }
        boolean verse = column.size() < MIN_PROSE_LINES
                || fullLineCount < (column.size() - 1) * PROSE_FULL_LINE_RATIO;

        for (int i = 0; i < column.size(); i++) {
            Line line = column.get(i);
            if (i == column.size() - 1) {
                // 다음 줄 정보가 없으므로 줄이 꽉 차 있을 때만(다음 페이지로 이어질 때만) 잇는다.
                breakTypes[line.lastIndex()] = !verse && fullLines[i] ? OcrBreakType.WRAP : OcrBreakType.LINE;
                continue;
            }
            Line next = column.get(i + 1);
            boolean blankLineBetween = (next.top() - line.top()) - usualPitch > PARAGRAPH_GAP_RATIO * charHeight;
            boolean nextIndented = next.left() - neighborLeft(column, i + 1) > INDENT_RATIO * charHeight;
            breakTypes[line.lastIndex()] = decide(verse, fullLines[i], nextIndented, blankLineBetween);
        }
    }

    private static OcrBreakType decide(boolean verse, boolean fullLine, boolean nextIndented, boolean blankLineBetween) {
        if (blankLineBetween) {
            return OcrBreakType.PARAGRAPH;
        }
        if (verse) {
            return OcrBreakType.LINE;
        }
        if (!fullLine && nextIndented) {
            return OcrBreakType.PARAGRAPH;
        }
        // 짧게 끝났거나 다음 줄이 들여쓰기면 끊는다. 애매할 때 끊는 쪽을 택한다(잘못 합친 행은 복구가 어렵다).
        if (!fullLine || nextIndented) {
            return OcrBreakType.LINE;
        }
        return OcrBreakType.WRAP;
    }

    // 줄 간격은 각 줄 첫 단어의 위쪽 y 차이로 잰다. 줄 전체의 위아래 범위를 쓰면 사진이 기울었을 때 값이 크게 흔들린다.
    private static List<Double> linePitches(List<Line> column) {
        List<Double> pitches = new ArrayList<>();
        for (int i = 0; i < column.size() - 1; i++) {
            pitches.add(column.get(i + 1).top() - column.get(i).top());
        }
        return pitches;
    }

    private static double neighborRight(List<Line> column, int index) {
        return neighbors(column, index).stream().mapToDouble(Line::right).max().orElseThrow();
    }

    private static double neighborLeft(List<Line> column, int index) {
        return neighbors(column, index).stream().mapToDouble(Line::left).min().orElseThrow();
    }

    private static List<Line> neighbors(List<Line> column, int index) {
        int from = Math.max(0, index - NEIGHBOR_WINDOW);
        int to = Math.min(column.size(), index + NEIGHBOR_WINDOW + 1);
        return column.subList(from, to);
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    // 한 줄의 요약. left/top은 첫 단어, right는 마지막 단어 기준이다(기울어진 사진에서 줄 전체 범위보다 안정적).
    private record Line(int lastIndex, double left, double right, double top, List<Double> wordHeights) {

        static Line of(List<OcrTextBlock> blocks, int firstIndex, int lastIndex) {
            List<OcrTextBlock.Point> first = blocks.get(firstIndex).boundingBox().vertices();
            List<OcrTextBlock.Point> last = blocks.get(lastIndex).boundingBox().vertices();
            List<Double> wordHeights = blocks.subList(firstIndex, lastIndex + 1).stream()
                    .map(block -> maxY(block.boundingBox().vertices()) - minY(block.boundingBox().vertices()))
                    .toList();
            return new Line(lastIndex, minX(first), maxX(last), minY(first), wordHeights);
        }

        private static double minX(List<OcrTextBlock.Point> points) {
            return points.stream().mapToDouble(OcrTextBlock.Point::x).min().orElse(0);
        }

        private static double maxX(List<OcrTextBlock.Point> points) {
            return points.stream().mapToDouble(OcrTextBlock.Point::x).max().orElse(0);
        }

        private static double minY(List<OcrTextBlock.Point> points) {
            return points.stream().mapToDouble(OcrTextBlock.Point::y).min().orElse(0);
        }

        private static double maxY(List<OcrTextBlock.Point> points) {
            return points.stream().mapToDouble(OcrTextBlock.Point::y).max().orElse(0);
        }
    }
}
