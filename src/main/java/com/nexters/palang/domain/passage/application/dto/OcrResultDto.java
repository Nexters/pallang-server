package com.nexters.palang.domain.passage.application.dto;

import java.util.List;

public record OcrResultDto(String text, BoundingBox boundingBox, boolean lineBreak) {

    public record BoundingBox(List<Point> vertices) {
    }

    public record Point(double x, double y) {
    }
}
