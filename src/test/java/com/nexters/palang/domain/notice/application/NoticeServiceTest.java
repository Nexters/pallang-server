package com.nexters.palang.domain.notice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.notice.common.error.NoticeException;
import com.nexters.palang.domain.notice.domain.Notice;
import com.nexters.palang.domain.notice.infrastructure.NoticeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    private NoticeService noticeService;

    @BeforeEach
    void setUp() {
        noticeService = new NoticeService(noticeRepository);
    }

    private Notice notice(Long id, String title) {
        Notice notice = Notice.builder().title(title).content("내용").build();
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }

    @Test
    @DisplayName("공지사항 목록을 조회하면 Repository 결과를 그대로 반환한다")
    void getNotices() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notice> expected = new PageImpl<>(List.of(notice(1L, "공지")), pageable, 1);
        given(noticeRepository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(expected);

        Page<Notice> results = noticeService.getNotices(pageable);

        assertThat(results).isEqualTo(expected);
    }

    @Test
    @DisplayName("존재하는 공지사항 ID로 조회하면 해당 공지사항을 반환한다")
    void getNotice() {
        Notice notice = notice(1L, "공지");
        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

        Notice result = noticeService.getNotice(1L);

        assertThat(result).isEqualTo(notice);
    }

    @Test
    @DisplayName("존재하지 않는 공지사항 ID로 조회하면 예외가 발생한다")
    void getNoticeFailsWhenNotFound() {
        given(noticeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.getNotice(1L)).isInstanceOf(NoticeException.class);
    }
}
