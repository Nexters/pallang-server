package com.nexters.palang.domain.book.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.palang.domain.book.application.UserBookStatusService;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.domain.UserBookStatus;
import com.nexters.palang.domain.book.presentation.dto.UpdateUserBookStatusRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.security.CurrentUserProvider;
import com.nexters.palang.global.security.LoginRequiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserBookStatusController.class)
class UserBookStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserBookStatusService userBookStatusService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private UserBookStatus userBookStatus(Long bookId, ReadingStatus status, Integer currentPage) {
        Book book = Book.builder().title("제목").author("작가").publisher("출판사").pageCount(300).build();
        ReflectionTestUtils.setField(book, "id", bookId);
        return UserBookStatus.builder()
                .user(User.builder().build())
                .book(book)
                .status(status)
                .currentPage(currentPage)
                .build();
    }

    @Test
    @DisplayName("읽기상태와 현재 페이지를 설정하면 설정된 값을 반환한다")
    void updateBookStatus() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(userBookStatusService.updateBookStatus(anyLong(), any()))
                .willReturn(userBookStatus(10L, ReadingStatus.READING, 50));

        UpdateUserBookStatusRequest request = new UpdateUserBookStatusRequest(10L, ReadingStatus.READING, 50);

        mockMvc.perform(put("/api/users/me/book-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookId").value(10))
                .andExpect(jsonPath("$.data.status").value("READING"))
                .andExpect(jsonPath("$.data.currentPage").value(50));
    }

    @Test
    @DisplayName("인증 없이 읽기상태를 설정하면 401 에러가 발생한다")
    void updateBookStatusFailsWhenUnauthenticated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willThrow(new LoginRequiredException());

        UpdateUserBookStatusRequest request = new UpdateUserBookStatusRequest(10L, ReadingStatus.READING, 50);

        mockMvc.perform(put("/api/users/me/book-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("AUTH_401_1"));
    }

    @Test
    @DisplayName("읽기 상태 없이 요청하면 400 에러가 발생한다")
    void updateBookStatusFailsWhenStatusIsMissing() throws Exception {
        String requestBody = "{\"bookId\":10,\"currentPage\":50}";

        mockMvc.perform(put("/api/users/me/book-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }
}
