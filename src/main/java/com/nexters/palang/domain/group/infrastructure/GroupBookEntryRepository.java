package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.GroupBookEntry;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupBookEntryRepository extends JpaRepository<GroupBookEntry, Long> {

    // 모임들의 entry를 book과 함께 읽는다. id 오름차순이라 모임별 첫 항목이 가장 먼저 등록된 entry다 —
    // 기존 단일 책 API(상세/목록/초대 미리보기)는 이 첫 entry를 모임의 "대표 책"으로 응답한다(GroupBookEntries).
    @Query("select e from GroupBookEntry e join fetch e.book where e.group.id in :groupIds order by e.id asc")
    List<GroupBookEntry> findAllWithBookByGroupIdIn(@Param("groupIds") Collection<Long> groupIds);

    boolean existsByGroupIdAndBookId(Long groupId, Long bookId);

    // 종료 임박 알림(GroupEndingSoonNotifier): 이 날짜에 끝나는 ACTIVE 모임의 entry. group은 이름을 쓰므로 join fetch.
    @Query("select e from GroupBookEntry e join fetch e.group g "
            + "where e.endDate = :endDate and g.status = com.nexters.palang.domain.group.domain.GroupStatus.ACTIVE")
    List<GroupBookEntry> findAllInActiveGroupsByEndDate(@Param("endDate") LocalDate endDate);

    // 관리자 도서 삭제(AdminBookService): 이 책을 함께 읽는 모임 id 전부.
    @Query("select distinct e.group.id from GroupBookEntry e where e.book.id = :bookId")
    List<Long> findGroupIdsByBookId(@Param("bookId") Long bookId);

    void deleteAllByGroupIdIn(Collection<Long> groupIds);
}
