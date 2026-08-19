package com.nexters.palang.domain.opinion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.event.OpinionLikedEvent;
import com.nexters.palang.domain.opinion.infrastructure.OpinionLikeRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OpinionLikeServiceTest {

    @Mock
    private OpinionRepository opinionRepository;

    @Mock
    private OpinionLikeRepository opinionLikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OpinionLikeService opinionLikeService;

    private void init() {
        opinionLikeService = new OpinionLikeService(
                opinionRepository, opinionLikeRepository, userRepository, eventPublisher);
    }

    private User user(Long id) {
        User user = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Opinion opinion(Long id, User owner) {
        Opinion opinion = Opinion.builder().user(owner).content("흔적 내용").build();
        ReflectionTestUtils.setField(opinion, "id", id);
        return opinion;
    }

    @Test
    @DisplayName("좋아요를 누르면 좋아요 카운트가 증가하고 알림 이벤트가 발행된다")
    void likeIncreasesCountAndPublishesEvent() {
        init();
        User owner = user(1L);
        Opinion opinion = opinion(10L, owner);
        given(opinionRepository.findById(10L)).willReturn(Optional.of(opinion));
        given(opinionLikeRepository.existsByUserIdAndOpinionId(2L, 10L)).willReturn(false);
        given(userRepository.getReferenceById(2L)).willReturn(user(2L));

        OpinionLikeResult result = opinionLikeService.toggleLike(2L, 10L);

        assertThat(result.liked()).isTrue();
        assertThat(opinion.getLikeCount()).isEqualTo(1);
        verify(eventPublisher).publishEvent(new OpinionLikedEvent(10L, 1L, 2L));
    }

    @Test
    @DisplayName("이미 좋아요를 누른 상태에서 다시 요청하면 좋아요가 취소되고 알림 이벤트는 발행되지 않는다")
    void unlikeDecreasesCountAndDoesNotPublishEvent() {
        init();
        User owner = user(1L);
        Opinion opinion = opinion(10L, owner);
        opinion.increaseLikeCount();
        given(opinionRepository.findById(10L)).willReturn(Optional.of(opinion));
        given(opinionLikeRepository.existsByUserIdAndOpinionId(2L, 10L)).willReturn(true);

        OpinionLikeResult result = opinionLikeService.toggleLike(2L, 10L);

        assertThat(result.liked()).isFalse();
        assertThat(opinion.getLikeCount()).isZero();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 흔적에 좋아요를 시도하면 예외가 발생한다")
    void toggleLikeThrowsExceptionWhenOpinionNotFound() {
        init();
        given(opinionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> opinionLikeService.toggleLike(2L, 999L))
                .isInstanceOf(OpinionException.class);
    }
}
