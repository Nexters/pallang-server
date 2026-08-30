package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.common.error.BookException;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.book.infrastructure.UserBookStatusRepository;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminBookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserBookStatusRepository userBookStatusRepository;
    @Mock
    private AdminCascadeDeleter cascadeDeleter;

    private AdminBookService adminBookService;

    @BeforeEach
    void setUp() {
        adminBookService = new AdminBookService(
                bookRepository, passageRepository, groupRepository, userBookStatusRepository, cascadeDeleter);
    }

    @Test
    @DisplayName("존재하지 않는 책을 삭제하려 하면 예외가 발생한다")
    void deleteFailsWhenBookNotFound() {
        given(bookRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminBookService.deleteBook(1L)).isInstanceOf(BookException.class);
    }

    @Test
    @DisplayName("책을 삭제하면 이 책의 모임/대목을 먼저 지운 뒤 읽기 상태와 책 자체를 지운다")
    void deleteBookCascadesGroupsAndPassagesThenBook() {
        given(bookRepository.findById(1L)).willReturn(Optional.of(mock(Book.class)));

        Group hostedGroup = mock(Group.class);
        given(hostedGroup.getId()).willReturn(100L);
        given(groupRepository.findAllByBookId(1L)).willReturn(List.of(hostedGroup));

        Passage globalPassage = mock(Passage.class);
        given(globalPassage.getId()).willReturn(200L);
        given(passageRepository.findAllByBookId(1L)).willReturn(List.of(globalPassage));

        adminBookService.deleteBook(1L);

        verify(cascadeDeleter).deleteGroups(List.of(100L));
        verify(cascadeDeleter).deletePassages(List.of(200L));
        verify(userBookStatusRepository).deleteAllByBookId(1L);
        verify(bookRepository).deleteById(1L);
    }
}
