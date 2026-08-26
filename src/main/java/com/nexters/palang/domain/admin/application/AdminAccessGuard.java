package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.common.error.AdminErrorCode;
import com.nexters.palang.domain.admin.common.error.AdminException;
import com.nexters.palang.global.security.CurrentUserProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 인가는 서비스/컨트롤러 레이어의 CurrentUserProvider가 담당한다는 이 프로젝트의 컨벤션(SecurityConfig
// 참고)을 그대로 따른다: 별도 관리자 로그인/역할 체계를 새로 만들지 않고, 기존 로그인 JWT의 userId가
// admin.user-ids 화이트리스트에 있는지만 확인한다.
@Component
@RequiredArgsConstructor
public class AdminAccessGuard {

    private final CurrentUserProvider currentUserProvider;

    @Value("${admin.user-ids:}")
    private List<Long> adminUserIds;

    public Long requireAdmin() {
        Long userId = currentUserProvider.getCurrentUserId();
        if (!adminUserIds.contains(userId)) {
            throw new AdminException(AdminErrorCode.ADMIN_ACCESS_DENIED);
        }
        return userId;
    }
}
