package com.nexters.palang.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.application.BookService;
import com.nexters.palang.domain.book.application.ExternalBookResult;
import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.policy.domain.Policy;
import com.nexters.palang.domain.policy.domain.PolicyType;
import com.nexters.palang.domain.policy.infrastructure.PolicyRepository;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import com.nexters.palang.global.common.error.AppException;
import com.nexters.palang.global.common.error.GlobalErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LocalDataSeederTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private BookService bookService;

    private LocalDataSeeder localDataSeeder;

    @BeforeEach
    void setUp() {
        localDataSeeder =
                new LocalDataSeeder(bookRepository, passageRepository, userRepository, policyRepository, bookService);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임").snsProvider(SnsProvider.KAKAO).snsId("seed-user").build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("알라딘 검색이 실패하면 플레이스홀더 값으로 책·대목 시드 데이터를 생성한다")
    void run_seedsWithFallbackWhenAladinFails() {
        given(policyRepository.findByType(any(PolicyType.class))).willReturn(Optional.empty());
        given(bookRepository.count()).willReturn(0L);
        given(userRepository.save(any(User.class))).willReturn(user(1L));
        given(bookService.searchExternalBooks(anyString(), any(PageRequest.class)))
                .willThrow(new AppException(GlobalErrorCode.INTERNAL_SERVER_ERROR));
        given(bookRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        localDataSeeder.run(null);

        verify(policyRepository, times(2)).save(any(Policy.class));
        verify(bookRepository).saveAll(anyList());
        verify(passageRepository, times(6)).save(any(Passage.class));
    }

    @Test
    @DisplayName("알라딘 검색이 성공하면 저자/출판사/ISBN/표지를 검색 결과 값으로 채운다")
    void run_usesAladinResultWhenAvailable() {
        given(policyRepository.findByType(any(PolicyType.class))).willReturn(Optional.empty());
        given(bookRepository.count()).willReturn(0L);
        given(userRepository.save(any(User.class))).willReturn(user(1L));
        ExternalBookResult aladinResult =
                new ExternalBookResult("달러구트 꿈 백화점", "이미예(실제)", "팩토리나인(실제)", "9791165341909",
                        "https://image.aladin.co.kr/real-cover.jpg");
        given(bookService.searchExternalBooks(anyString(), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(aladinResult)))
                .willReturn(Page.empty());
        given(bookRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        localDataSeeder.run(null);

        ArgumentCaptor<List<Book>> booksCaptor = ArgumentCaptor.forClass(List.class);
        verify(bookRepository).saveAll(booksCaptor.capture());
        Book firstBook = booksCaptor.getValue().get(0);
        assertThat(firstBook.getAuthor()).isEqualTo("이미예(실제)");
        assertThat(firstBook.getPublisher()).isEqualTo("팩토리나인(실제)");
        assertThat(firstBook.getIsbn()).isEqualTo("9791165341909");
        assertThat(firstBook.getCoverImageUrl()).isEqualTo("https://image.aladin.co.kr/real-cover.jpg");
        assertThat(firstBook.getSource()).isEqualTo(BookSource.API);
    }

    @Test
    @DisplayName("이미 약관과 책이 존재하면 다시 생성하지 않는다")
    void run_skipsWhenAlreadySeeded() {
        given(policyRepository.findByType(any(PolicyType.class)))
                .willReturn(Optional.of(Policy.builder().type(PolicyType.TERMS).content("기존 내용").build()));
        given(bookRepository.count()).willReturn(6L);

        localDataSeeder.run(null);

        verify(policyRepository, never()).save(any(Policy.class));
        verify(bookRepository, never()).saveAll(anyList());
        verify(passageRepository, never()).save(any(Passage.class));
    }
}
