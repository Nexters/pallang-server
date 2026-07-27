package com.nexters.palang.global.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.policy.domain.Policy;
import com.nexters.palang.domain.policy.domain.PolicyType;
import com.nexters.palang.domain.policy.infrastructure.PolicyRepository;
import com.nexters.palang.domain.user.domain.SnsProvider;
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
class LocalDataSeederTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private PassageRepository passageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PolicyRepository policyRepository;

    private LocalDataSeeder localDataSeeder;

    @BeforeEach
    void setUp() {
        localDataSeeder = new LocalDataSeeder(bookRepository, passageRepository, userRepository, policyRepository);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임").snsProvider(SnsProvider.KAKAO).snsId("seed-user").build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("약관과 책이 모두 비어있으면 약관 2건과 책·대목 시드 데이터를 생성한다")
    void run_seedsWhenEmpty() {
        given(policyRepository.findByType(any(PolicyType.class))).willReturn(Optional.empty());
        given(bookRepository.count()).willReturn(0L);
        given(userRepository.save(any(User.class))).willReturn(user(1L));
        given(bookRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        localDataSeeder.run(null);

        verify(policyRepository, times(2)).save(any(Policy.class));
        verify(bookRepository).saveAll(anyList());
        verify(passageRepository, times(6)).save(any(Passage.class));
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
