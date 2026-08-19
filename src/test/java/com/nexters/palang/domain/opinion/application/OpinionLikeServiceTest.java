package com.nexters.palang.domain.opinion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.group.application.GroupAccessValidator;
import com.nexters.palang.domain.group.common.error.GroupErrorCode;
import com.nexters.palang.domain.group.common.error.GroupException;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.opinion.common.error.OpinionException;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.opinion.domain.OpinionLike;
import com.nexters.palang.domain.opinion.infrastructure.OpinionLikeRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private GroupAccessValidator groupAccessValidator;

    private OpinionLikeService opinionLikeService;

    private User writer;

    @BeforeEach
    void setUp() {
        opinionLikeService = new OpinionLikeService(opinionRepository, opinionLikeRepository, userRepository, groupAccessValidator);
        writer = user(1L);
    }

    private User user(Long id) {
        User built = User.builder().nickname("닉네임" + id).build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Opinion opinion(Long id, Group group) {
        Passage passage = Passage.builder().group(group).build();
        Opinion built = Opinion.builder().user(writer).passage(passage).content("흔적 내용").build();
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    private Group group(Long id) {
        Book book = Book.builder().title("제목").author("작가").publisher("출판사").pageCount(300).build();
        Group built = Group.create("모임", book, writer, 4, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 20));
        ReflectionTestUtils.setField(built, "id", id);
        return built;
    }

    @Test
    @DisplayName("존재하지 않는 흔적에 좋아요를 시도하면 예외가 발생한다")
    void toggleLikeFailsWhenOpinionNotFound() {
        given(opinionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> opinionLikeService.toggleLike(1L, 999L)).isInstanceOf(OpinionException.class);
    }

    @Test
    @DisplayName("좋아요를 누르지 않은 흔적에 토글하면 좋아요가 생성되고 카운트가 증가한다")
    void toggleLikeLikesWhenNotAlreadyLiked() {
        Opinion opinion = opinion(1L, null);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(opinionLikeRepository.existsByUserIdAndOpinionId(1L, 1L)).willReturn(false);

        OpinionLikeResult result = opinionLikeService.toggleLike(1L, 1L);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1);
        verify(opinionLikeRepository).save(any(OpinionLike.class));
    }

    @Test
    @DisplayName("이미 좋아요를 누른 흔적에 토글하면 좋아요가 취소되고 카운트가 감소한다")
    void toggleLikeUnlikesWhenAlreadyLiked() {
        Opinion opinion = opinion(1L, null);
        opinion.increaseLikeCount();
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(opinionLikeRepository.existsByUserIdAndOpinionId(1L, 1L)).willReturn(true);

        OpinionLikeResult result = opinionLikeService.toggleLike(1L, 1L);

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isZero();
        verify(opinionLikeRepository).deleteByUserIdAndOpinionId(1L, 1L);
    }

    @Test
    @DisplayName("모임 전용 흔적에 모임원이 아닌 사용자가 좋아요를 시도하면 예외가 발생한다")
    void toggleLikeFailsWhenNotGroupMember() {
        Group group = group(9L);
        Opinion opinion = opinion(1L, group);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        doThrow(new GroupException(GroupErrorCode.NOT_MEMBER)).when(groupAccessValidator).validateMember(9L, 20L);

        assertThatThrownBy(() -> opinionLikeService.toggleLike(20L, 1L)).isInstanceOf(GroupException.class);
        verify(opinionLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("모임 전용 흔적에 모임원이 좋아요를 시도하면 정상적으로 처리된다")
    void toggleLikeSucceedsWhenGroupMember() {
        Group group = group(9L);
        Opinion opinion = opinion(1L, group);
        given(opinionRepository.findById(1L)).willReturn(Optional.of(opinion));
        given(opinionLikeRepository.existsByUserIdAndOpinionId(10L, 1L)).willReturn(false);

        OpinionLikeResult result = opinionLikeService.toggleLike(10L, 1L);

        assertThat(result.liked()).isTrue();
        verify(groupAccessValidator).validateMember(9L, 10L);
    }
}
