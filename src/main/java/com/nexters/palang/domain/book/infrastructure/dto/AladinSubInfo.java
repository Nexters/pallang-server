package com.nexters.palang.domain.book.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinSubInfo(Integer itemPage) {
}
