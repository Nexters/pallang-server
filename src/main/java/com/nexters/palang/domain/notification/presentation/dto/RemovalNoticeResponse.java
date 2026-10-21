package com.nexters.palang.domain.notification.presentation.dto;

import com.nexters.palang.domain.notification.domain.Notification;
import com.nexters.palang.domain.notification.domain.RemovalReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

// 운영 안내(CONTENT_REMOVED) 알림을 눌렀을 때 보여줄 안내 화면용 정보. 삭제된 게시물 대신 이 스냅샷을 보여준다.
public record RemovalNoticeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RemovalReason reason,
        @Schema(example = "책 내용 입력란에 의견 작성", requiredMode = Schema.RequiredMode.REQUIRED) String reasonLabel,
        @Schema(description = "작성자에게 보여줄 삭제 사유 안내 문구", requiredMode = Schema.RequiredMode.REQUIRED)
        String reasonMessage,
        @Schema(example = "참을 수 없는 존재의 가벼움", nullable = true) String bookTitle,
        @Schema(example = "2026-10-19T14:30:00", nullable = true) LocalDateTime writtenAt,
        @Schema(description = "작성 내용 일부(최대 100자). 욕설·비방 사유는 미리보기를 생략해 null",
                nullable = true) String contentExcerpt,
        @Schema(example = "pallang4you@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED) String contactEmail,
        @Schema(description = "문의 안내 문구", requiredMode = Schema.RequiredMode.REQUIRED) String contactGuide
) {
    public static RemovalNoticeResponse from(Notification notification) {
        RemovalReason reason = notification.getRemovalReason();
        return new RemovalNoticeResponse(
                reason, reason.getLabel(), reason.getMessage(),
                notification.getRemovedBookTitle(), notification.getRemovedWrittenAt(),
                notification.getRemovedContentExcerpt(),
                RemovalReason.CONTACT_EMAIL, RemovalReason.CONTACT_GUIDE);
    }
}
