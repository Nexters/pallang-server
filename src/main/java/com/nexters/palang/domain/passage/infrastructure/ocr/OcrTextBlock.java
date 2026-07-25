package com.nexters.palang.domain.passage.infrastructure.ocr;

import java.util.List;

public record OcrTextBlock(String text, BoundingBox boundingBox, boolean lineBreak) {

    public record BoundingBox(List<Point> vertices) {
        public double topY() {
            return vertices.get(0).y();
        }

        public double leftX() {
            return vertices.get(0).x();
        }
    }

    public record Point(double x, double y) {
    }
}
