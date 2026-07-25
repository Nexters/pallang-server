package com.nexters.palang.domain.passage.infrastructure.ocr.clova;

import java.util.List;

record ClovaOcrRequest(String version, String requestId, long timestamp, List<Image> images) {

    record Image(String format, String name) {
    }
}
