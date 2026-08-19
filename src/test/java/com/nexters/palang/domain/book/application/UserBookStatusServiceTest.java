package com.nexters.palang.domain.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.ReadingStatus;
import com.nexters.palang.domain.book.domain.UserBookStatus;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.book.infrastructure.UserBookStatusRepository;
import com.nexters.palang.domain.book.presentation.dto.UpdateUserBookStatusRequest;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserBookStatusServiceTest {

    @Mock
    private UserBookStatusRepository userBookStatusRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    private UserBookStatusService userBookStatusService;

    @BeforeEach
    void setUp() {
        userBookStatusService = new UserBookStatusService(userBookStatusRepository, bookRepository, userRepository);
    }

    private Book book(Long id, int pageCount) {
        Book book = Book.builder().title("제목").author("작가").publisher("출판사").pageCount(pageCount).build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private User user(Long id) {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("기존 읽기상태가 없으면 새로 생성한다")
    void updateBookStatusCreatesNewStatusWhenNoneExists() {
        given(bookRepository.findById(10L)).willReturn(Optional.of(book(10L, 300)));
        given(userBookStatusRepository.findByUserIdAndBookId(1L, 10L)).willReturn(Optional.empty());
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(userBookStatusRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        UserBookStatus result = userBookStatusService.updateBookStatus(
                1L, new UpdateUserBookStatusRequest(10L, ReadingStatus.READING, 50));

        assertThat(result.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(result.getCurrentPage()).isEqualTo(50);
    }

    @Test
    @DisplayName("기존 읽기상태가 있으면 값을 갱신한다")
    void updateBookStatusUpdatesExistingStatus() {
        Book book = book(10L, 300);
        UserBookStatus existing = UserBookStatus.builder()
                .user(user(1L)).book(book).status(ReadingStatus.PLANNED).currentPage(null).build();
        given(bookRepository.findById(10L)).willReturn(Optional.of(book));
        given(userBookStatusRepository.findByUserIdAndBookId(1L, 10L)).willReturn(Optional.of(existing));

        UserBookStatus result = userBookStatusService.updateBookStatus(
                1L, new UpdateUserBookStatusRequest(10L, ReadingStatus.READING, 100));

        assertThat(result).isSameAs(existing);
        assertThat(result.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(result.getCurrentPage()).isEqualTo(100);
    }

    @Test
    @DisplayName("동시 요청이 먼저 레코드를 생성해 유니크 제약 위반이 나면, 그 레코드를 다시 조회해 갱신으로 폴백한다")
    void updateBookStatusFallsBackToUpdateWhenConcurrentInsertViolatesUniqueConstraint() {
        Book book = book(10L, 300);
        UserBookStatus winnerOfRace = UserBookStatus.builder()
                .user(user(1L)).book(book).status(ReadingStatus.PLANNED).currentPage(null).build();
        given(bookRepository.findById(10L)).willReturn(Optional.of(book));
        given(userBookStatusRepository.findByUserIdAndBookId(1L, 10L))
                .willReturn(Optional.empty(), Optional.of(winnerOfRace));
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(userBookStatusRepository.save(any())).willThrow(new DataIntegrityViolationException("duplicate"));

        UserBookStatus result = userBookStatusService.updateBookStatus(
                1L, new UpdateUserBookStatusRequest(10L, ReadingStatus.READING, 70));

        assertThat(result).isSameAs(winnerOfRace);
        assertThat(result.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(result.getCurrentPage()).isEqualTo(70);
    }

    @Test
    @DisplayName("현재 페이지가 도서의 전체 페이지 수를 초과하면 예외가 발생한다")
    void updateBookStatusThrowsExceptionWhenCurrentPageExceedsPageCount() {
        given(bookRepository.findById(10L)).willReturn(Optional.of(book(10L, 300)));

        assertThatThrownBy(() -> userBookStatusService.updateBookStatus(
                1L, new UpdateUserBookStatusRequest(10L, ReadingStatus.READING, 301)))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("존재하지 않는 도서로 읽기상태를 설정하려 하면 예외가 발생한다")
    void updateBookStatusThrowsExceptionWhenBookDoesNotExist() {
        given(bookRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userBookStatusService.updateBookStatus(
                1L, new UpdateUserBookStatusRequest(10L, ReadingStatus.READING, 50)))
                .isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("설정된 읽기상태를 해제하면 삭제된다")
    void removeBookStatusDeletesExistingStatus() {
        given(userBookStatusRepository.existsByUserIdAndBookId(1L, 10L)).willReturn(true);

        userBookStatusService.removeBookStatus(1L, 10L);

        verify(userBookStatusRepository).deleteByUserIdAndBookId(1L, 10L);
    }

    @Test
    @DisplayName("설정된 읽기상태가 없으면 해제 시 예외가 발생한다")
    void removeBookStatusThrowsExceptionWhenStatusDoesNotExist() {
        given(userBookStatusRepository.existsByUserIdAndBookId(1L, 10L)).willReturn(false);

        assertThatThrownBy(() -> userBookStatusService.removeBookStatus(1L, 10L))
                .isInstanceOf(BookException.class);
    }
}
