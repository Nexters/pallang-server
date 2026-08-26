package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.presentation.dto.AdminOpinionListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminOpinionSummaryResponse;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class AdminOpinionMapper {

    private AdminOpinionMapper() {
    }

    // passage/user는 지연 로딩 연관관계다. AdminPassageMapper와 같은 이유로 open-in-view에 기대고 있다.
    public static AdminOpinionSummaryResponse toSummary(Opinion opinion) {
        return new AdminOpinionSummaryResponse(
                opinion.getId(), opinion.getPassage().getId(), opinion.getPassage().getQuotedText(),
                opinion.getUser().getId(), opinion.getUser().getNickname(),
                opinion.getContent(), opinion.getLikeCount(), opinion.isDeleted(), opinion.getCreatedAt());
    }

    public static AdminOpinionListResponse toListResponse(Page<Opinion> opinions) {
        return new AdminOpinionListResponse(opinions.map(AdminOpinionMapper::toSummary).getContent(), PageInfo.from(opinions));
    }
}
