package com.nexters.palang.domain.admin.presentation.dto;

import com.nexters.palang.domain.report.domain.ReportReason;
import com.nexters.palang.domain.report.domain.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

// 관리자 "신고" 메뉴의 한 행: 신고 대상(의견/댓글) 하나와 거기 쌓인 신고 묶음.
public record AdminReportedTargetResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ReportTargetType targetType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long targetId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long reportCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime lastReportedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ReasonCount> reasons,
        @Schema(description = "신고자가 적은 상세 사유(최근 순, 최대 5개)", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> details,
        @Schema(description = "대상이 아직 살아 있으면 true. 이미 삭제(처리)됐으면 false이고 아래 내용 필드는 null",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean targetAlive,
        @Schema(nullable = true) String authorNickname,
        @Schema(nullable = true) String content,
        @Schema(nullable = true, description = "의견이면 자기 자신, 댓글이면 그 댓글이 달린 의견") Long opinionId,
        @Schema(nullable = true) String passageQuotedText,
        @Schema(nullable = true) String bookTitle,
        @Schema(nullable = true) String groupName
) {
    public record ReasonCount(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ReportReason reason,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long count
    ) {
    }
}
