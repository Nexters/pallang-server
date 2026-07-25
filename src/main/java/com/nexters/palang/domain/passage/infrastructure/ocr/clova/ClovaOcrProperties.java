package com.nexters.palang.domain.passage.infrastructure.ocr.clova;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clova.ocr")
public record ClovaOcrProperties(String invokeUrl, String secretKey) {
}
