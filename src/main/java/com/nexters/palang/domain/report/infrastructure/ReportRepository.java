package com.nexters.palang.domain.report.infrastructure;

import com.nexters.palang.domain.report.domain.Report;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    // 관리자 유저 삭제(AdminUserService): 이 유저가 신고자인 신고 내역 전부.
    void deleteAllByReporterId(Long reporterId);

    // 관리자 사유 선택 삭제: 모임 게시물은 신고가 1건 이상 접수된 경우에만 삭제할 수 있다.
    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    // 관리자 페이지 목록에 대상별 신고 수 뱃지를 붙이기 위한 집계.
    @Query("select r.targetId as targetId, count(r) as reportCount from Report r "
            + "where r.targetType = :targetType and r.targetId in :targetIds group by r.targetId")
    List<ReportCountProjection> countByTargets(
            @Param("targetType") ReportTargetType targetType, @Param("targetIds") Collection<Long> targetIds);

    // 관리자 "신고" 메뉴: 신고 대상(의견/댓글) 단위로 묶어 가장 최근에 신고된 대상부터 보여준다.
    @Query(value = "select r.targetType as targetType, r.targetId as targetId, count(r) as reportCount, "
            + "max(r.createdAt) as lastReportedAt from Report r "
            + "group by r.targetType, r.targetId order by max(r.createdAt) desc",
            countQuery = "select count(r) from Report r where r.id in "
                    + "(select max(r2.id) from Report r2 group by r2.targetType, r2.targetId)")
    Page<ReportTargetSummaryProjection> findTargetSummaries(Pageable pageable);

    // 위와 같고 정렬만 반대(오래 방치된 신고부터 처리할 때). 집계 결과 기준 정렬이라 Pageable의 Sort로는 표현할 수 없다.
    @Query(value = "select r.targetType as targetType, r.targetId as targetId, count(r) as reportCount, "
            + "max(r.createdAt) as lastReportedAt from Report r "
            + "group by r.targetType, r.targetId order by max(r.createdAt) asc",
            countQuery = "select count(r) from Report r where r.id in "
                    + "(select max(r2.id) from Report r2 group by r2.targetType, r2.targetId)")
    Page<ReportTargetSummaryProjection> findTargetSummariesOldestFirst(Pageable pageable);

    List<Report> findByTargetTypeAndTargetIdIn(ReportTargetType targetType, Collection<Long> targetIds);
}
