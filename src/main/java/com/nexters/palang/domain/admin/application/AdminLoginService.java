package com.nexters.palang.domain.admin.application;

import com.nexters.palang.domain.admin.common.error.AdminErrorCode;
import com.nexters.palang.domain.admin.common.error.AdminException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// 관리자 페이지 전용 로그인. admin.login-username/password(둘 다 기본값 없음 — 설정 전까지는 어떤
// 입력으로도 로그인이 성공할 수 없다)와 정확히 일치해야 토큰이 발급된다. 문자열 비교에 String.equals
// 대신 MessageDigest.isEqual을 쓰는 이유: 이 엔드포인트는 인증 전 상태에서 인터넷에 노출되는
// "관문"이라 타이밍 공격으로 비밀번호를 한 글자씩 추측당하는 걸 막기 위함이다.
@Service
public class AdminLoginService {

    private final String configuredUsername;
    private final String configuredPassword;
    private final AdminJwtProvider adminJwtProvider;

    public AdminLoginService(
            @Value("${admin.login-username:}") String configuredUsername,
            @Value("${admin.login-password:}") String configuredPassword,
            AdminJwtProvider adminJwtProvider) {
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
        this.adminJwtProvider = adminJwtProvider;
    }

    public String login(String username, String password) {
        if (configuredUsername.isBlank() || configuredPassword.isBlank()
                || !constantTimeEquals(configuredUsername, username)
                || !constantTimeEquals(configuredPassword, password)) {
            throw new AdminException(AdminErrorCode.ADMIN_LOGIN_FAILED);
        }
        return adminJwtProvider.createToken();
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
