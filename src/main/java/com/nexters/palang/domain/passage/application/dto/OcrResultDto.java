package com.nexters.palang.domain.passage.application.dto;

import com.nexters.palang.domain.passage.domain.OcrBreakType;
import java.util.List;

public record OcrResultDto(String text, BoundingBox boundingBox, boolean lineBreak, OcrBreakType breakType,
                           boolean indented) {

    public record BoundingBox(List<Point> vertices) {
    }

    public record Point(double x, double y) {
    }
}
