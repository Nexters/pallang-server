package com.nexters.palang.domain.group.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.group.application.GroupBookPage;
import com.nexters.palang.domain.group.application.GroupBookService;
import com.nexters.palang.domain.group.application.GroupBookStatusFilter;
import com.nexters.palang.domain.group.application.GroupBookView;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.domain.GroupBookEntry;
import com.nexters.palang.domain.group.domain.ReadingPeriod;
import com.nexters.palang.domain.group.domain.ReadingStatus;
import com.nexters.palang.domain.group.presentation.dto.AddGroupBookRequest;
import com.nexters.palang.domain.group.presentation.dto.UpdateGroupBookRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.global.security.CurrentUserProvider;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GroupBookController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupBookService groupBookService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private GroupBookView view(ReadingStatus status) {
        User host = User.builder().nickname("호스트").build();
        ReflectionTestUtils.setField(host, "id", 1L);
        Group group = Group.create("고전 뽀개기", host, 4);
        Book book = Book.builder().title("모순").author("양귀자").publisher("쓰다").pageCount(300).source(BookSource.API).build();
        ReflectionTestUtils.setField(book, "id", 100L);
        GroupBookEntry entry = GroupBookEntry.create(
                group, book, ReadingPeriod.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 22)), LocalDate.MIN);
        ReflectionTestUtils.setField(entry, "id", 10L);
        return new GroupBookView(entry, status, 6L, 17L);
    }

    @Test
    @DisplayName("모임 책 목록은 적용한 탭, 탭별 개수, 책 카드(상태와 기록 수)를 반환한다")
    void getGroupBooks() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        PageRequest pageable = PageRequest.of(0, 20);
        given(groupBookService.getGroupBooks(eq(9L), eq(1L), isNull(), any()))
                .willReturn(new GroupBookPage(GroupBookStatusFilter.READING, 2, 0,
                        new PageImpl<>(List.of(view(ReadingStatus.READING)), pageable, 1)));

        mockMvc.perform(get("/api/groups/9/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READING"))
                .andExpect(jsonPath("$.data.readingCount").value(2))
                .andExpect(jsonPath("$.data.finishedCount").value(0))
                .andExpect(jsonPath("$.data.books[0].entryId").value(10))
                .andExpect(jsonPath("$.data.books[0].title").value("모순"))
                .andExpect(jsonPath("$.data.books[0].author").value("양귀자"))
                .andExpect(jsonPath("$.data.books[0].endDate").value("2026-10-22"))
                .andExpect(jsonPath("$.data.books[0].status").value("READING"))
                .andExpect(jsonPath("$.data.books[0].statusMode").value("AUTO"))
                .andExpect(jsonPath("$.data.books[0].passageCount").value(6))
                .andExpect(jsonPath("$.data.books[0].opinionCount").value(17));
    }

    @Test
    @DisplayName("status 파라미터로 탭을 지정할 수 있다")
    void getGroupBooksWithStatus() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupBookService.getGroupBooks(eq(9L), eq(1L), eq(GroupBookStatusFilter.FINISHED), any()))
                .willReturn(new GroupBookPage(GroupBookStatusFilter.FINISHED, 0, 1,
                        new PageImpl<>(List.of(view(ReadingStatus.FINISHED)), PageRequest.of(0, 20), 1)));

        mockMvc.perform(get("/api/groups/9/books").param("status", "FINISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FINISHED"))
                .andExpect(jsonPath("$.data.books[0].status").value("FINISHED"));
    }

    @Test
    @DisplayName("현재 모임원이 아니면 책 목록 조회 시 403 에러가 발생한다")
    void getGroupBooksFailsWhenNotMember() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(2L);
        given(groupBookService.getGroupBooks(eq(9L), eq(2L), any(), any()))
                .willThrow(new GroupException(GroupErrorCode.NOT_MEMBER));

        mockMvc.perform(get("/api/groups/9/books"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("GROUP_403_2"));
    }

    @Test
    @DisplayName("책을 추가하면 Idempotency-Key가 서비스로 전달되고 추가된 책을 반환한다")
    void addGroupBook() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupBookService.addGroupBook(eq(9L), eq(1L), any(AddGroupBookRequest.class), eq("add-key")))
                .willReturn(view(ReadingStatus.READING));

        mockMvc.perform(post("/api/groups/9/books")
                        .header("Idempotency-Key", "add-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entryId").value(10));
    }

    @Test
    @DisplayName("책을 선택하지 않고 추가하면 400 에러가 발생한다")
    void addGroupBookFailsWithoutBook() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/groups/9/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("COMMON_400_1"));
    }

    @Test
    @DisplayName("이미 등록된 판본을 추가하면 409 에러가 발생한다")
    void addGroupBookFailsWhenDuplicated() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupBookService.addGroupBook(eq(9L), eq(1L), any(AddGroupBookRequest.class), isNull()))
                .willThrow(new GroupException(GroupErrorCode.GROUP_BOOK_ALREADY_ADDED));

        mockMvc.perform(post("/api/groups/9/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":100}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("GROUP_409_4"));
    }

    @Test
    @DisplayName("책 설정을 바꾸면 바뀐 책을 반환한다")
    void updateGroupBook() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupBookService.updateGroupBook(eq(9L), eq(10L), eq(1L), any(UpdateGroupBookRequest.class)))
                .willReturn(view(ReadingStatus.FINISHED));

        mockMvc.perform(patch("/api/groups/9/books/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-10-01\",\"endDate\":\"2026-10-22\",\"status\":\"FINISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FINISHED"));
    }

    @Test
    @DisplayName("이 모임의 책이 아니면 책 설정 변경 시 404 에러가 발생한다")
    void updateGroupBookFailsWhenEntryNotFound() throws Exception {
        given(currentUserProvider.getCurrentUserId()).willReturn(1L);
        given(groupBookService.updateGroupBook(eq(9L), eq(99L), eq(1L), any(UpdateGroupBookRequest.class)))
                .willThrow(new GroupException(GroupErrorCode.GROUP_BOOK_ENTRY_NOT_FOUND));

        mockMvc.perform(patch("/api/groups/9/books/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("GROUP_404_3"));
    }
}
