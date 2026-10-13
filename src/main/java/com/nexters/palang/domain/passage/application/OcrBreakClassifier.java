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
    // 줄 시작이 주변 줄의 왼쪽 끝보다 글자 높이의 0.4배 이상 들어가 있으면 들여쓰기로 본다.
    // 책의 들여쓰기는 보통 한 글자 폭이지만 Clova 박스 높이에 여백이 포함돼 실제 들여쓰기는 박스 높이의 0.6배 안팎으로 잡힌다.
    // 기울기 보정 후 들여쓰지 않은 줄끼리는 왼쪽 끝이 거의 일치하므로 이 정도로 낮춰도 오판하지 않는다.
    private static final double INDENT_RATIO = 0.4;
    // 줄 사이 간격이 평소보다 글자 0.8개 이상 벌어지면 빈 줄(문단·연 구분)로 본다.
    private static final double PARAGRAPH_GAP_RATIO = 0.8;
    // 단(컬럼)의 줄 끝 중 이 비율 이상이 산문의 특징(오른쪽 끝까지 참, 또는 짧게 끝나고 다음 줄이 들여쓰기)을 보여야
    // 산문으로 본다. 아니면 운문(시)으로 본다. 대화문이 많은 산문은 짧은 줄이 많아 "꽉 찬 줄"만으로는 운문으로 오판한다.
    private static final double PROSE_LINE_RATIO = 0.5;
    // 줄 수가 이보다 적으면 산문인지 판단할 근거가 없으므로 운문처럼 줄마다 끊는다(잘못 끊는 쪽이 복구하기 쉽다).
    private static final int MIN_PROSE_LINES = 3;
    // 사진 기울기·책 휘어짐 때문에 단 전체의 여백이 일정하지 않으므로, 앞뒤 몇 줄만 보고 여백 기준을 잡는다.
    // 1줄만 보면 한 줄짜리 대화문(들여쓰기)이 3줄 이상 이어질 때 가운데 줄의 들여쓰기를 놓쳐 운문으로 오판할 수 있어 3줄을 본다.
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
    // 겹치는 단이 여럿이면 마지막 줄이 세로로 가장 가까운 단을 고른다. 사진 귀퉁이에 걸린 다른 페이지 조각이
    // 먼저 단을 차지해 본문 줄을 빼앗아 가는 것을 막는다. 단 안의 줄 순서는 Clova가 준 순서를 그대로 유지한다.
    private static List<List<Line>> groupIntoColumns(List<Line> lines) {
        List<List<Line>> columns = new ArrayList<>();
        List<double[]> spans = new ArrayList<>();
        for (Line line : lines) {
            int columnIndex = findNearestOverlappingColumn(columns, spans, line);
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

    private static int findNearestOverlappingColumn(List<List<Line>> columns, List<double[]> spans, Line line) {
        int nearest = -1;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < spans.size(); i++) {
            double[] span = spans.get(i);
            double overlap = Math.min(span[1], line.right()) - Math.max(span[0], line.left());
            double shorterWidth = Math.min(span[1] - span[0], line.right() - line.left());
            if (overlap <= 0 || overlap < shorterWidth * COLUMN_OVERLAP_RATIO) {
                continue;
            }
            List<Line> column = columns.get(i);
            double distance = Math.abs(line.top() - column.get(column.size() - 1).top());
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static void classifyColumn(List<Line> column, OcrBreakType[] breakTypes) {
        double charHeight = median(column.stream().flatMap(line -> line.wordHeights().stream()).toList());
        double usualPitch = median(linePitches(column));

        double marginDrift = median(leftMarginSteps(column));

        boolean[] fullLines = new boolean[column.size()];
        boolean[] nextIndented = new boolean[column.size()];
        for (int i = 0; i < column.size(); i++) {
            fullLines[i] = neighborRight(column, i) - column.get(i).right() < SHORT_LINE_RATIO * charHeight;
            if (i < column.size() - 1) {
                nextIndented[i] = column.get(i + 1).left() - neighborLeft(column, i + 1, marginDrift) > INDENT_RATIO * charHeight;
            }
        }
        boolean verse = isVerse(fullLines, nextIndented);

        for (int i = 0; i < column.size(); i++) {
            Line line = column.get(i);
            if (i == column.size() - 1) {
                // 다음 줄 정보가 없으므로 줄이 꽉 차 있을 때만(다음 페이지로 이어질 때만) 잇는다.
                breakTypes[line.lastIndex()] = lastLineBreakType(verse, fullLines[i]);
                continue;
            }
            Line next = column.get(i + 1);
            boolean blankLineBetween = (next.top() - line.top()) - usualPitch > PARAGRAPH_GAP_RATIO * charHeight;
            breakTypes[line.lastIndex()] = decide(verse, fullLines[i], nextIndented[i], blankLineBetween);
        }
    }

    // 산문의 줄 끝은 대부분 "오른쪽 끝까지 참(지면 폭에서 꺾임)"이거나 "짧게 끝나고 다음 줄이 들여쓰기(문단 끝)"다.
    // 시의 행은 짧게 끝나도 다음 행이 들여쓰기 없이 시작하므로 둘 다 해당하지 않는다.
    // 단의 마지막 줄은 다음 줄 정보가 없어 판정 비율에서 뺀다.
    private static boolean isVerse(boolean[] fullLines, boolean[] nextIndented) {
        int lineEnds = fullLines.length - 1;
        if (fullLines.length < MIN_PROSE_LINES) {
            return true;
        }
        int proseLikeCount = 0;
        for (int i = 0; i < lineEnds; i++) {
            if (fullLines[i] || nextIndented[i]) {
                proseLikeCount++;
            }
        }
        return proseLikeCount < lineEnds * PROSE_LINE_RATIO;
    }

    private static OcrBreakType decide(boolean verse, boolean fullLine, boolean nextIndented, boolean blankLineBetween) {
        if (blankLineBetween) {
            return OcrBreakType.BLANK_LINE;
        }
        if (verse) {
            return OcrBreakType.LINE;
        }
        // 짧게 끝났거나 다음 줄이 들여쓰기면 문단 끝으로 끊는다. 산문에서 꺾인 줄(WRAP)이 아닌데 끊기는 곳은
        // 사실상 전부 문단 끝이다(들여쓰기 없는 책의 짧은 줄 포함). 애매할 때 끊는 쪽을 택한다(잘못 합친 행은 복구가 어렵다).
        if (!fullLine || nextIndented) {
            return OcrBreakType.PARAGRAPH;
        }
        return OcrBreakType.WRAP;
    }

    private static OcrBreakType lastLineBreakType(boolean verse, boolean fullLine) {
        if (verse) {
            return OcrBreakType.LINE;
        }
        return fullLine ? OcrBreakType.WRAP : OcrBreakType.PARAGRAPH;
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

    // 이웃 줄의 왼쪽 끝을 이 줄 위치로 옮겨 온 값(줄 간 거리 × 줄당 여백 밀림만큼 보정) 중 가장 왼쪽을 여백 기준으로 삼는다.
    // 원근 왜곡이나 책 휘어짐으로 여백이 줄마다 조금씩 밀리면, 보정 없이 3줄 떨어진 줄과 비교할 때 밀림이 쌓여 들여쓰기로 오판한다.
    private static double neighborLeft(List<Line> column, int index, double marginDrift) {
        int from = Math.max(0, index - NEIGHBOR_WINDOW);
        int to = Math.min(column.size(), index + NEIGHBOR_WINDOW + 1);
        double margin = Double.MAX_VALUE;
        for (int j = from; j < to; j++) {
            margin = Math.min(margin, column.get(j).left() + marginDrift * (index - j));
        }
        return margin;
    }

    // 이웃한 두 줄의 왼쪽 끝 차이. 대부분의 줄은 들여쓰지 않은 줄끼리 이웃하므로 중앙값이 "줄당 여백 밀림"이 된다.
    // (들여쓰기 시작·끝, 다른 영역 조각에서 생기는 큰 차이는 중앙값이 무시한다)
    private static List<Double> leftMarginSteps(List<Line> column) {
        List<Double> steps = new ArrayList<>();
        for (int i = 0; i < column.size() - 1; i++) {
            steps.add(column.get(i + 1).left() - column.get(i).left());
        }
        return steps;
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
