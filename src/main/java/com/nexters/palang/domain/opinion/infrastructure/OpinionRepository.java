package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.opinion.domain.Opinion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpinionRepository extends JpaRepository<Opinion, Long> {

    long countByUserIdAndDeletedAtIsNull(Long userId);

    // 관리자 의견 검색(AdminOpinionService): 소프트 삭제 여부와 무관하게 내용에 키워드가 포함된 의견 전부.
    // open-in-view: false 환경에서 AdminOpinionMapper가 컨트롤러 단(트랜잭션 밖)에서 passage/user를
    // 참조하므로, EntityGraph로 미리 로딩해두지 않으면 LazyInitializationException이 난다.
    @EntityGraph(attributePaths = {"passage", "user"})
    Page<Opinion> findByContentContaining(String keyword, Pageable pageable);

    // 관리자 의견 수정/삭제(AdminOpinionService): 위와 같은 이유로, 단건 조회에도 passage/user를
    // 미리 로딩해둔다(수정 응답도 컨트롤러 단에서 같은 필드를 참조한다).
    @EntityGraph(attributePaths = {"passage", "user"})
    Optional<Opinion> findWithAssociationsById(Long id);

    // 흔적 삭제 시 그 대목에 남은 다른 살아있는 흔적이 있는지 확인하기 위함 (없으면 대목도 함께 삭제).
    boolean existsByPassageIdAndDeletedAtIsNullAndIdNot(Long passageId, Long id);

    // 대목 소유자 판정(findMyPassages와 동일 기준): 이 대목에 흔적을 남긴 사용자인지 확인.
    boolean existsByPassageIdAndUserIdAndDeletedAtIsNull(Long passageId, Long userId);

    // open-in-view: false 환경에서 컨트롤러 단 응답 매핑이 opinion.getUser()/getDecorations()를
    // 참조하므로 트랜잭션 안에서 미리 로딩해야 LazyInitializationException을 피할 수 있다.
    // passage/passage.group도 함께 fetch join한다 — OpinionService.getOpinion()이 모임 스코프 흔적인지
    // 판단하려면 group을 초기화해야 하는데, 서비스 메서드 트랜잭션이 끝난 뒤(컨트롤러 매핑 시점)에는
    // 지연 로딩이 막혀 있어 이 시점에 함께 가져와야 한다.
    @Query("select distinct o from Opinion o join fetch o.user left join fetch o.decorations "
            + "left join fetch o.passage p left join fetch p.group where o.id = :id")
    Optional<Opinion> findDetailById(@Param("id") Long id);

    // "책에 새 의견 N개" 알림(BookNewOpinionsNotifier)의 현재 카운트 기준값.
    long countByPassage_Book_IdAndDeletedAtIsNull(Long bookId);

    // 위 알림의 수신 대상 후보: 이 책에 살아있는 의견을 남긴 적 있는 사용자 목록(작성자 본인 제외는 호출부 책임).
    @Query("select distinct o.user.id from Opinion o where o.passage.book.id = :bookId and o.deletedAt is null")
    List<Long> findDistinctUserIdsByBookId(@Param("bookId") Long bookId);

    // 관리자 유저 삭제(AdminUserService): 삭제 대상 대목들에 달린 의견 id 전부. 소프트 삭제 여부와 무관하게
    // 물리적으로 존재하는 행을 전부 대상으로 해야 대목을 지울 때 FK 위반이 나지 않는다.
    @Query("select o.id from Opinion o where o.passage.id in :passageIds")
    List<Long> findIdsByPassageIdIn(@Param("passageIds") List<Long> passageIds);

    // 관리자 유저 삭제(AdminUserService): 이 유저 본인이 남긴 의견 id 전부(대목 소속 모임과 무관).
    @Query("select o.id from Opinion o where o.user.id = :userId")
    List<Long> findIdsByUserId(@Param("userId") Long userId);

    // 관리자 유저 삭제 차단 조건: 이 유저가 작성한 대목 중, 다른 사용자가 의견을 남긴 대목이 있는지 확인.
    // 있으면 그 대목(과 대목에 달린 모든 의견/댓글/좋아요)을 지울 때 타인의 1차 콘텐츠까지 함께 사라지므로
    // 삭제를 차단해야 한다.
    @Query("select distinct o.passage.id from Opinion o where o.passage.id in :passageIds and o.user.id <> :userId")
    List<Long> findPassageIdsWithOpinionsFromOtherUsers(
            @Param("passageIds") List<Long> passageIds, @Param("userId") Long userId);
}
