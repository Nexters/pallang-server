package com.nexters.palang.domain.admin.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.decoration.infrastructure.DecorationRepository;
import com.nexters.palang.domain.group.domain.Group;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionLikeRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminCascadeDeleterTest {

    @Mock
    private PassageRepository passageRepository;
    @Mock
    private OpinionRepository opinionRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private DecorationRepository decorationRepository;
    @Mock
    private OpinionLikeRepository opinionLikeRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;

    private AdminCascadeDeleter cascadeDeleter;

    @BeforeEach
    void setUp() {
        cascadeDeleter = new AdminCascadeDeleter(passageRepository, opinionRepository, commentRepository,
                decorationRepository, opinionLikeRepository, groupRepository, groupMemberRepository);
    }

    @Test
    @DisplayName("의견 삭제는 댓글/데코/좋아요를 먼저 지운 뒤 의견 자체를 지운다")
    void deleteOpinionsRemovesSubContentFirst() {
        cascadeDeleter.deleteOpinions(List.of(1L, 2L));

        verify(commentRepository).deleteAllByOpinionIdIn(List.of(1L, 2L));
        verify(decorationRepository).deleteAllByOpinionIdIn(List.of(1L, 2L));
        verify(opinionLikeRepository).deleteAllByOpinionIdIn(List.of(1L, 2L));
        verify(opinionRepository).deleteAllById(List.of(1L, 2L));
    }

    @Test
    @DisplayName("빈 id 목록이면 아무 것도 지우지 않는다")
    void deleteOpinionsNoOpWhenEmpty() {
        cascadeDeleter.deleteOpinions(List.of());

        verify(opinionRepository, never()).deleteAllById(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("대목 삭제는 그 위 의견들을 먼저 지운 뒤 대목 자체를 지운다")
    void deletePassagesRemovesOpinionsFirst() {
        given(opinionRepository.findIdsByPassageIdIn(List.of(10L))).willReturn(List.of(1L));

        cascadeDeleter.deletePassages(List.of(10L));

        verify(opinionRepository).deleteAllById(List.of(1L));
        verify(passageRepository).deleteAllById(List.of(10L));
    }

    @Test
    @DisplayName("모임 삭제는 그 안의 대목들을 먼저 지운 뒤 모임원과 모임을 지운다")
    void deleteGroupsRemovesPassagesThenMembersThenGroup() {
        Passage passage = mock(Passage.class);
        given(passage.getId()).willReturn(10L);
        given(passageRepository.findAllByGroupIdIn(List.of(100L))).willReturn(List.of(passage));
        given(opinionRepository.findIdsByPassageIdIn(List.of(10L))).willReturn(List.of());

        cascadeDeleter.deleteGroups(List.of(100L));

        verify(passageRepository).deleteAllById(List.of(10L));
        verify(groupMemberRepository).deleteAllByGroupIdIn(List.of(100L));
        verify(groupRepository).deleteAllById(List.of(100L));
    }
}
