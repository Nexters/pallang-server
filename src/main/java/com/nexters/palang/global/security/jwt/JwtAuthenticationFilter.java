package com.nexters.palang.global.security.jwt;

import com.nexters.palang.global.security.AuthErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * OptionalAuthentication(backend_plan.md §4.2): 토큰이 없어도 요청을 막지 않고 통과시킨다.
 * 토큰이 있으면 SecurityContext + request attribute에 유저 ID를 채우고,
 * 유효하지 않으면 원인을 attribute에 남겨 CurrentUserProvider가 정확한 에러 코드로 변환하게 한다.
 * 실제 "인증 필요" 차단은 이 필터가 아니라 서비스 레이어(CurrentUserProvider)가 담당한다.
 *
 * 일부러 @Component로 등록하지 않는다: Filter를 구현하는 @Component는 @WebMvcTest 슬라이스가
 * 자동으로 스캔해 들여오는데, 그러면 JwtTokenProvider 빈이 없는 슬라이스 컨텍스트에서
 * 생성자 주입이 깨진다. SecurityConfig가 직접 new로 생성해 필터 체인에 끼워 넣는다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ID_ATTRIBUTE = "currentUserId";
    public static final String AUTH_ERROR_ATTRIBUTE = "authError";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Long userId = jwtTokenProvider.getUserId(token);
                request.setAttribute(CURRENT_USER_ID_ATTRIBUTE, userId);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of()));
            } catch (ExpiredJwtException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCode.TOKEN_EXPIRED);
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCode.INVALID_TOKEN);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
