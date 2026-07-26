package com.nexters.palang.domain.notice.application;

import com.nexters.palang.domain.notice.common.error.NoticeErrorCode;
import com.nexters.palang.domain.notice.common.error.NoticeException;
import com.nexters.palang.domain.notice.domain.Notice;
import com.nexters.palang.domain.notice.infrastructure.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public Page<Notice> getNotices(Pageable pageable) {
        return noticeRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Notice getNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }
}
