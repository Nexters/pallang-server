package com.nexters.palang.domain.book.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverBookSearchResponse(List<NaverBookItem> items, Long total) {
}
