package com.nexters.palang.domain.group.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// 초대 링크로 들어온 사용자가 가입 전 미리 보는 정보. 비로그인 상태에서도 조회 가능해야 하므로
// 모임 상세(GroupDetailResponse)보다 훨씬 적은 정보만 담는다(멤버 목록, host 정보 등은 노출하지 않음).
public record GroupInvitationPreviewResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long groupId,
        @Schema(example = "고전 뽀개기", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(example = "채식주의자", requiredMode = Schema.RequiredMode.REQUIRED) String bookTitle,
        @Schema(example = "한강", requiredMode = Schema.RequiredMode.REQUIRED) String bookAuthor,
        @Schema(example = "https://image.aladin.co.kr/product/123/45/cover/8936434120_1.jpg", nullable = true) String bookCoverImageUrl,
        @Schema(example = "4", requiredMode = Schema.RequiredMode.REQUIRED) int capacity,
        @Schema(example = "3", requiredMode = Schema.RequiredMode.REQUIRED) long memberCount,
        @Schema(example = "false", description = "정원이 가득 차 더 이상 가입할 수 없는지 여부",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean full,
        @Schema(example = "false", description = "요청자가 이미 이 모임의 멤버인지 여부. 비로그인 요청이면 항상 false.",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean alreadyJoined
) {
}
