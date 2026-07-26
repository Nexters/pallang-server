package com.nexters.palang.domain.notice.presentation;

import com.nexters.palang.domain.notice.application.NoticeMapper;
import com.nexters.palang.domain.notice.application.NoticeService;
import com.nexters.palang.domain.notice.presentation.dto.NoticeListResponse;
import com.nexters.palang.domain.notice.presentation.dto.NoticeResponse;
import com.nexters.palang.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NoticeController implements NoticeApi {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final NoticeService noticeService;

    @Override
    @GetMapping("/api/notices")
    public ResponseEntity<DataResponse<NoticeListResponse>> getNotices(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(
                DataResponse.from(NoticeMapper.toListResponse(noticeService.getNotices(pageable(page, size)))));
    }

    @Override
    @GetMapping("/api/notices/{noticeId}")
    public ResponseEntity<DataResponse<NoticeResponse>> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(DataResponse.from(NoticeMapper.toResponse(noticeService.getNotice(noticeId))));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
