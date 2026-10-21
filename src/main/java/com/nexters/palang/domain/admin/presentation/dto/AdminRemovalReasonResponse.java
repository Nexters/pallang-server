package com.nexters.palang.domain.admin.presentation.dto;

import com.nexters.palang.domain.notification.domain.RemovalReason;
import io.swagger.v3.oas.annotations.media.Schema;

// 관리자 페이지 삭제 사유 선택 모달용. 사유별 안내 문구를 서버 한 곳에서만 관리하기 위해 내려준다.
public record AdminRemovalReasonResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RemovalReason reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String label,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message,
        @Schema(description = "false면 안내 화면에 작성 내용 미리보기를 생략", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean showsContentPreview
) {
    public static AdminRemovalReasonResponse from(RemovalReason reason) {
        return new AdminRemovalReasonResponse(reason, reason.getLabel(), reason.getMessage(), reason.isShowsContentPreview());
    }
}
