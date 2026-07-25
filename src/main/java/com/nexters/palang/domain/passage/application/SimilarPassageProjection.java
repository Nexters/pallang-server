package com.nexters.palang.domain.passage.application;

public record SimilarPassageProjection(Long passageId, String quotedText, int pageNumber, long opinionCount) {
}
