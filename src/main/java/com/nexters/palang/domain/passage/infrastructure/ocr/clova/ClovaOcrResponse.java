package com.nexters.palang.domain.passage.infrastructure.ocr.clova;

import java.util.List;

record ClovaOcrResponse(String version, String requestId, long timestamp, List<ImageResult> images) {

    record ImageResult(String uid, String name, String inferResult, String message, List<Field> fields) {
    }

    record Field(String valueType, BoundingPoly boundingPoly, String inferText, double inferConfidence, boolean lineBreak) {
    }

    record BoundingPoly(List<Vertex> vertices) {
    }

    record Vertex(double x, double y) {
    }
}
