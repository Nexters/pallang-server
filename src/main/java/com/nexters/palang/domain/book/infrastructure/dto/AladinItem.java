package com.nexters.palang.domain.book.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinItem(
        String title,
        String author,
        String publisher,
        String isbn13,
        String cover,
        AladinSubInfo subInfo
) {
}
