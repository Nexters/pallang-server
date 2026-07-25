package com.nexters.palang.domain.passage.infrastructure.ocr;

import java.util.List;

public interface OcrClient {

    List<OcrTextBlock> recognize(byte[] imageBytes, String contentType);
}
