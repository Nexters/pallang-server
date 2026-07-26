package com.nexters.palang.domain.notice.application;

import com.nexters.palang.domain.notice.domain.Notice;
import com.nexters.palang.domain.notice.presentation.dto.NoticeListResponse;
import com.nexters.palang.domain.notice.presentation.dto.NoticeResponse;
import com.nexters.palang.global.common.response.PageInfo;
import org.springframework.data.domain.Page;

public final class NoticeMapper {

    private NoticeMapper() {
    }

    public static NoticeResponse toResponse(Notice notice) {
        return new NoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt());
    }

    public static NoticeListResponse toListResponse(Page<Notice> notices) {
        return new NoticeListResponse(notices.map(NoticeMapper::toResponse).getContent(), PageInfo.from(notices));
    }
}
