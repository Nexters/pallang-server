package com.nexters.palang.domain.book.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoBookSearchResponse(Meta meta, List<KakaoBookDocument> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            @JsonProperty("total_count") Long totalCount,
            @JsonProperty("pageable_count") Long pageableCount,
            @JsonProperty("is_end") Boolean isEnd
    ) {
    }
}
