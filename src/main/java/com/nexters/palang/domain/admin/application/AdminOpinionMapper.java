package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.presentation.dto.AdminOpinionListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminOpinionSummaryResponse;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.global.common.response.PageInfo;
import java.util.Map;
import org.springframework.data.domain.Page;

public final class AdminOpinionMapper {

    private AdminOpinionMapper() {
    }

    // passage/book/group/user는 지연 로딩 연관관계라, 조회 쪽 EntityGraph(OpinionRepository)로 미리 불러온
    // 의견만 넘겨야 한다(open-in-view: false라 컨트롤러 단에서는 지연 로딩이 막혀 있다).
    public static AdminOpinionSummaryResponse toSummary(Opinion opinion, long reportCount) {
        Passage passage = opinion.getPassage();
        Group group = passage.getGroup();
        return new AdminOpinionSummaryResponse(
                opinion.getId(), passage.getId(), passage.getQuotedText(), passage.getBook().getTitle(),
                group != null ? group.getId() : null, group != null ? group.getName() : null,
                opinion.getUser().getId(), opinion.getUser().getNickname(),
                opinion.getContent(), opinion.getLikeCount(), reportCount, opinion.isDeleted(), opinion.getCreatedAt());
    }

    public static AdminOpinionListResponse toListResponse(Page<Opinion> opinions, Map<Long, Long> reportCounts) {
        return new AdminOpinionListResponse(
                opinions.map(opinion -> toSummary(opinion, reportCounts.getOrDefault(opinion.getId(), 0L))).getContent(),
                PageInfo.from(opinions));
    }
}
