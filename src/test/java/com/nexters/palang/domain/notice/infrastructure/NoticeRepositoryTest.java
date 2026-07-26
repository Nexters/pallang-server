package com.nexters.palang.domain.notice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexters.palang.domain.notice.domain.Notice;
import com.nexters.palang.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class NoticeRepositoryTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Test
    @DisplayName("공지사항 목록을 조회하면 최신 등록순으로 반환한다")
    void findAllByOrderByCreatedAtDesc() {
        Notice older = noticeRepository.save(Notice.builder().title("첫 공지").content("첫 공지 내용").build());
        Notice newer = noticeRepository.save(Notice.builder().title("둘째 공지").content("둘째 공지 내용").build());

        Page<Notice> results = noticeRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(Notice::getId).containsExactly(newer.getId(), older.getId());
        assertThat(results.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("등록된 공지사항이 없으면 빈 목록을 반환한다")
    void findAllByOrderByCreatedAtDescReturnsEmptyWhenNoNotice() {
        Page<Notice> results = noticeRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20));

        assertThat(results.getContent()).isEmpty();
    }
}
