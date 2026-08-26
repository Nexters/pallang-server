package com.nexters.palang.domain.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.nexters.palang.domain.admin.common.error.AdminErrorCode;
import com.nexters.palang.domain.admin.common.error.AdminException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminLoginServiceTest {

    @Mock
    private AdminJwtProvider adminJwtProvider;

    @Test
    @DisplayName("아이디/비밀번호가 일치하면 토큰을 발급한다")
    void loginSucceedsWithMatchingCredentials() {
        AdminLoginService service = new AdminLoginService("pallang", "pallang123!", adminJwtProvider);
        given(adminJwtProvider.createToken()).willReturn("issued-token");

        String token = service.login("pallang", "pallang123!");

        assertThat(token).isEqualTo("issued-token");
    }

    @Test
    @DisplayName("비밀번호가 다르면 로그인에 실패한다")
    void loginFailsWithWrongPassword() {
        AdminLoginService service = new AdminLoginService("pallang", "pallang123!", adminJwtProvider);

        assertThatThrownBy(() -> service.login("pallang", "wrong"))
                .isInstanceOf(AdminException.class)
                .hasFieldOrPropertyWithValue("errorCode", AdminErrorCode.ADMIN_LOGIN_FAILED);
    }

    @Test
    @DisplayName("아이디가 다르면 로그인에 실패한다")
    void loginFailsWithWrongUsername() {
        AdminLoginService service = new AdminLoginService("pallang", "pallang123!", adminJwtProvider);

        assertThatThrownBy(() -> service.login("someone-else", "pallang123!"))
                .isInstanceOf(AdminException.class)
                .hasFieldOrPropertyWithValue("errorCode", AdminErrorCode.ADMIN_LOGIN_FAILED);
    }

    @Test
    @DisplayName("설정된 아이디/비밀번호가 비어있으면 어떤 입력으로도 로그인할 수 없다")
    void loginAlwaysFailsWhenNotConfigured() {
        AdminLoginService service = new AdminLoginService("", "", adminJwtProvider);

        assertThatThrownBy(() -> service.login("pallang", "pallang123!"))
                .isInstanceOf(AdminException.class)
                .hasFieldOrPropertyWithValue("errorCode", AdminErrorCode.ADMIN_LOGIN_FAILED);
    }
}
