package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.presentation.dto.AdminPassageListResponse;
import com.nexters.palang.domain.admin.presentation.dto.AdminPassageSummaryResponse;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class AdminPassageMapper {

    private AdminPassageMapper() {
    }

    // book/creator/group은 지연 로딩 연관관계다. 이 프로젝트는 spring.jpa.open-in-view가 켜져 있어
    // 컨트롤러 응답 직렬화 시점까지 세션이 열려 있으므로 지금은 동작하지만, open-in-view를 끄게 되면
    // 이 매퍼가 호출되기 전에 서비스 트랜잭션 안에서 fetch join으로 미리 로딩하도록 바꿔야 한다.
    public static AdminPassageSummaryResponse toSummary(Passage passage) {
        return new AdminPassageSummaryResponse(
                passage.getId(),
                passage.getBook().getId(), passage.getBook().getTitle(),
                passage.getGroup() != null ? passage.getGroup().getId() : null,
                passage.getGroup() != null ? passage.getGroup().getName() : null,
                passage.getCreator().getId(), passage.getCreator().getNickname(),
                passage.getPageNumber(), passage.getQuotedText(), passage.isSpoiler(),
                passage.isDeleted(), passage.getCreatedAt());
    }

    public static AdminPassageListResponse toListResponse(Page<Passage> passages) {
        return new AdminPassageListResponse(passages.map(AdminPassageMapper::toSummary).getContent(), PageInfo.from(passages));
    }
}
