package com.nexters.palang.domain.passage.infrastructure.ocr.clova;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ClovaOcrProperties.class)
public class ClovaOcrConfig {

    @Bean
    public RestClient clovaOcrRestClient(ClovaOcrProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.invokeUrl())
                .defaultHeader("X-OCR-SECRET", properties.secretKey())
                .build();
    }

    @Bean
    public ObjectMapper clovaObjectMapper() {
        return new ObjectMapper();
    }
}
