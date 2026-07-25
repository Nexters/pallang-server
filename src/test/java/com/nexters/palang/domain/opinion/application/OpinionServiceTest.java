package com.nexters.palang.domain.opinion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest;
import com.nexters.palang.domain.opinion.presentation.dto.CreateOpinionRequest.DecorationRequest;
import com.nexters.palang.domain.passage.common.error.PassageException;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OpinionServiceTest {

    @Mock
    private OpinionRepository opinionRepository;

    @Mock
    private PassageRepository passageRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    private OpinionService opinionService;

    @BeforeEach
    void setUp() {
        opinionService = new OpinionService(opinionRepository, passageRepository, bookRepository, userRepository);
    }

    private User user(Long id) {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Book book(Long id) {
        Book book = Book.builder().title("제목").author("작가").publisher("출판사").pageCount(300).build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private Passage passage(Long id, Book book) {
        Passage passage = Passage.builder().book(book).build();
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }

    private CreateOpinionRequest request(Long bookId, Long passageId) {
        return new CreateOpinionRequest(
                bookId, 5, "발췌 문장", false, passageId, "흔적 내용",
                List.of(new DecorationRequest(0, 5, EffectType.UNDERLINE, null))
        );
    }

    @Test
    @DisplayName("passageId 없이 흔적을 생성하면 새 Passage가 만들어지고 흔적이 연결된다")
    void createOpinionCreatesNewPassageWhenPassageIdIsNull() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(bookRepository.findById(10L)).willReturn(Optional.of(book(10L)));
        given(passageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(opinionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        Opinion opinion = opinionService.createOpinion(1L, request(10L, null));

        assertThat(opinion.getPassage().getBook().getId()).isEqualTo(10L);
        assertThat(opinion.getDecorations()).hasSize(1);
    }

    @Test
    @DisplayName("passageId를 지정해 흔적을 생성하면 기존 Passage에 병합된다")
    void createOpinionMergesIntoExistingPassageWhenPassageIdIsGiven() {
        Book book = book(10L);
        Passage existing = passage(100L, book);
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(passageRepository.findById(100L)).willReturn(Optional.of(existing));
        given(opinionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        Opinion opinion = opinionService.createOpinion(1L, request(10L, 100L));

        assertThat(opinion.getPassage()).isEqualTo(existing);
    }

    @Test
    @DisplayName("passageId가 요청의 bookId와 다른 도서에 속하면 예외가 발생한다")
    void createOpinionThrowsExceptionWhenPassageBelongsToDifferentBook() {
        Book otherBook = book(99L);
        Passage existing = passage(100L, otherBook);
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(passageRepository.findById(100L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> opinionService.createOpinion(1L, request(10L, 100L)))
                .isInstanceOf(PassageException.class);
    }

    @Test
    @DisplayName("존재하지 않는 도서로 새 Passage를 만들려 하면 예외가 발생한다")
    void createOpinionThrowsExceptionWhenBookDoesNotExist() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(bookRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> opinionService.createOpinion(1L, request(10L, null)))
                .isInstanceOf(BookException.class);
    }
}
