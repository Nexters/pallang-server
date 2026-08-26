package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.admin.common.error.AdminErrorCode;
import com.nexters.palang.domain.admin.common.error.AdminException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAccessGuardTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private AdminJwtProvider adminJwtProvider;

    @Test
    @DisplayName("유효한 관리자 토큰이면 통과한다")
    void passesWithValidToken() {
        given(request.getHeader("Authorization")).willReturn("Bearer valid-token");
        given(adminJwtProvider.isValid("valid-token")).willReturn(true);
        AdminAccessGuard guard = new AdminAccessGuard(request, adminJwtProvider);

        assertThatCode(guard::requireAdmin).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 거부한다")
    void deniesWithoutHeader() {
        given(request.getHeader("Authorization")).willReturn(null);
        AdminAccessGuard guard = new AdminAccessGuard(request, adminJwtProvider);

        assertThatThrownBy(guard::requireAdmin)
                .isInstanceOf(AdminException.class)
                .hasFieldOrPropertyWithValue("errorCode", AdminErrorCode.ADMIN_ACCESS_DENIED);
    }

    @Test
    @DisplayName("토큰이 유효하지 않으면 거부한다")
    void deniesWithInvalidToken() {
        given(request.getHeader("Authorization")).willReturn("Bearer bad-token");
        given(adminJwtProvider.isValid("bad-token")).willReturn(false);
        AdminAccessGuard guard = new AdminAccessGuard(request, adminJwtProvider);

        assertThatThrownBy(guard::requireAdmin)
                .isInstanceOf(AdminException.class)
                .hasFieldOrPropertyWithValue("errorCode", AdminErrorCode.ADMIN_ACCESS_DENIED);
    }

    @Test
    @DisplayName("일반 유저 JWT를 흉내낸 비-Bearer 헤더는 거부한다")
    void deniesNonBearerHeader() {
        given(request.getHeader("Authorization")).willReturn("Basic dXNlcjpwYXNz");
        AdminAccessGuard guard = new AdminAccessGuard(request, adminJwtProvider);

        assertThatThrownBy(guard::requireAdmin).isInstanceOf(AdminException.class);
    }
}
