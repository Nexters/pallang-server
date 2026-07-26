package com.nexters.palang.domain.notice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.notice.application.NoticeService;
import com.nexters.palang.domain.notice.common.error.NoticeErrorCode;
import com.nexters.palang.domain.notice.common.error.NoticeException;
import com.nexters.palang.domain.notice.domain.Notice;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NoticeController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeService noticeService;

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    private Notice notice(Long id, String title) {
        Notice notice = Notice.builder().title(title).content("공지 내용").build();
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }

    @Test
    @DisplayName("공지사항 목록을 요청하면 목록을 반환한다")
    void getNotices() throws Exception {
        given(noticeService.getNotices(any(Pageable.class))).willReturn(
                new PageImpl<>(List.of(notice(1L, "공지 제목")), DEFAULT_PAGEABLE, 1));

        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notices[0].noticeId").value(1))
                .andExpect(jsonPath("$.data.notices[0].title").value("공지 제목"))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1));
    }

    @Test
    @DisplayName("size에 숫자가 아닌 값을 주면 400 에러가 발생한다")
    void getNoticesFailsWhenSizeIsNotNumber() throws Exception {
        mockMvc.perform(get("/api/notices").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("존재하는 공지사항 ID로 상세 조회를 요청하면 공지사항을 반환한다")
    void getNotice() throws Exception {
        given(noticeService.getNotice(eq(1L))).willReturn(notice(1L, "공지 제목"));

        mockMvc.perform(get("/api/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeId").value(1))
                .andExpect(jsonPath("$.data.title").value("공지 제목"))
                .andExpect(jsonPath("$.data.content").value("공지 내용"));
    }

    @Test
    @DisplayName("존재하지 않는 공지사항 ID로 상세 조회를 요청하면 404 에러가 발생한다")
    void getNoticeFailsWhenNotFound() throws Exception {
        given(noticeService.getNotice(eq(1L))).willThrow(new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        mockMvc.perform(get("/api/notices/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("NOTICE_404_1"));
    }
}
