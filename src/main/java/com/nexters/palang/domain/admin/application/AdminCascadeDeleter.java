package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.comment.infrastructure.CommentRepository;
import com.nexters.palang.domain.decoration.infrastructure.DecorationRepository;
import com.nexters.palang.domain.group.infrastructure.GroupMemberRepository;
import com.nexters.palang.domain.group.infrastructure.GroupRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionLikeRepository;
import com.nexters.palang.domain.opinion.infrastructure.OpinionRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// AdminPassageService/AdminOpinionService/AdminGroupService/AdminBookService가 공통으로 쓰는 하드 삭제
// cascade. AdminUserService.deleteUser의 삭제 순서(댓글/데코/좋아요 → 의견 → 대목 → 모임원 → 모임)를
// "특정 유저"가 아니라 "특정 의견/대목/모임 id 집합"을 기준으로 재사용할 수 있게 일반화했다. 이 프로젝트엔
// DB 레벨 ON DELETE CASCADE가 없어(ddl-auto: update, 마이그레이션 도구 없음) 항상 자식부터 순서대로 지워야
// FK 위반이 나지 않는다.
@Component
@RequiredArgsConstructor
public class AdminCascadeDeleter {

    private final PassageRepository passageRepository;
    private final OpinionRepository opinionRepository;
    private final CommentRepository commentRepository;
    private final DecorationRepository decorationRepository;
    private final OpinionLikeRepository opinionLikeRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    // 의견(들)과 거기 달린 댓글/데코/좋아요를 전부 지운다. 한 의견에 달린 댓글은 부모/답글(자기참조 FK)
    // 관계여도 opinion_id로 한 번에 지우면 같은 DELETE 문 안에서 함께 제거되어 순서 문제가 없다
    // (AdminUserService에서 이미 검증된 방식).
    public void deleteOpinions(Collection<Long> opinionIds) {
        if (opinionIds.isEmpty()) {
            return;
        }
        List<Long> ids = List.copyOf(opinionIds);
        commentRepository.deleteAllByOpinionIdIn(ids);
        decorationRepository.deleteAllByOpinionIdIn(ids);
        opinionLikeRepository.deleteAllByOpinionIdIn(ids);
        opinionRepository.deleteAllById(ids);
    }

    // 대목(들)과 그 위의 의견/댓글/데코/좋아요를 전부 지운다.
    public void deletePassages(Collection<Long> passageIds) {
        if (passageIds.isEmpty()) {
            return;
        }
        List<Long> ids = List.copyOf(passageIds);
        deleteOpinions(opinionRepository.findIdsByPassageIdIn(ids));
        passageRepository.deleteAllById(ids);
    }

    // 모임(들)과 그 안의 대목/의견/댓글/데코/좋아요, 모임원까지 전부 지운다.
    public void deleteGroups(Collection<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return;
        }
        List<Long> ids = List.copyOf(groupIds);
        List<Long> passageIds = passageRepository.findAllByGroupIdIn(ids).stream().map(Passage::getId).toList();
        deletePassages(passageIds);
        groupMemberRepository.deleteAllByGroupIdIn(ids);
        groupRepository.deleteAllById(ids);
    }
}
