package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.GroupBookEntry;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupBookEntryRepository extends JpaRepository<GroupBookEntry, Long> {

    // 모임들의 entry를 book과 함께 읽는다. id 오름차순이라 모임별 첫 항목이 가장 먼저 등록된 entry다 —
    // 기존 단일 책 API(상세/목록/초대 미리보기)는 이 첫 entry를 모임의 "대표 책"으로 응답한다(GroupBookEntries).
    @Query("select e from GroupBookEntry e join fetch e.book where e.group.id in :groupIds order by e.id asc")
    List<GroupBookEntry> findAllWithBookByGroupIdIn(@Param("groupIds") Collection<Long> groupIds);

    // 책 설정/조회: 이 모임에 속한 entry만 찾는다(다른 모임의 entryId로 접근하는 것을 막음). 응답에 book을 쓰므로 join fetch.
    @Query("select e from GroupBookEntry e join fetch e.book where e.id = :entryId and e.group.id = :groupId")
    Optional<GroupBookEntry> findWithBookByIdAndGroupId(
            @Param("entryId") Long entryId, @Param("groupId") Long groupId);

    // 책 추가 전용: 모임 행을 잠근 뒤 같은 판본 중복을 최신 커밋 기준으로 확인한다(잠금 대기 중 다른 요청이 추가한 책 포함).
    // MySQL REPEATABLE READ에서 일반 조회는 잠금 전 스냅샷을 읽을 수 있어 잠금 읽기로 읽는다. book까지 잠그지 않도록
    // join fetch는 하지 않고, 판본 비교에 필요한 book은 지연 로딩한다(모임당 책 수가 적다).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from GroupBookEntry e where e.group.id = :groupId")
    List<GroupBookEntry> findAllByGroupIdForUpdate(@Param("groupId") Long groupId);

    boolean existsByGroupIdAndBookId(Long groupId, Long bookId);

    // 종료 임박 알림(GroupEndingSoonNotifier): 이 날짜에 끝나는 ACTIVE 모임의 entry. 알림 문구에 모임 이름과 책 제목을
    // 쓰므로 group/book을 join fetch.
    @Query("select e from GroupBookEntry e join fetch e.group g join fetch e.book "
            + "where e.endDate = :endDate and g.status = com.nexters.palang.domain.group.domain.GroupStatus.ACTIVE")
    List<GroupBookEntry> findAllInActiveGroupsByEndDate(@Param("endDate") LocalDate endDate);

    // 관리자 도서 삭제(AdminBookService): 이 책을 함께 읽는 모임 id 전부.
    @Query("select distinct e.group.id from GroupBookEntry e where e.book.id = :bookId")
    List<Long> findGroupIdsByBookId(@Param("bookId") Long bookId);

    void deleteAllByGroupIdIn(Collection<Long> groupIds);
}
