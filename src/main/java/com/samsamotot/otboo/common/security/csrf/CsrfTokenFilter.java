package com.samsamotot.otboo.common.security.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CSRF 토큰을 HTTP 쿠키에 자동으로 설정하는 필터 클래스
 * 
 * <p>이 필터는 모든 HTTP 요청에 대해 CSRF 토큰이 쿠키에 존재하는지 확인하고,
 * 없을 경우 자동으로 생성하여 쿠키에 설정합니다. 이를 통해 클라이언트 측에서
 * CSRF 토큰을 쉽게 접근할 수 있도록 합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><strong>쿠키 확인</strong>: 요청에 CSRF 토큰 쿠키가 있는지 검사</li>
 *   <li><strong>토큰 생성 및 설정</strong>: 쿠키가 없으면 새 토큰을 생성하여 쿠키에 설정</li>
 *   <li><strong>쿠키 속성 설정</strong>: HttpOnly, Secure, Path, MaxAge 등 쿠키 속성 관리</li>
 *   <li><strong>경로 제외</strong>: 특정 공개 API 경로는 필터를 건너뛰도록 설정</li>
 * </ul>
 * 
 * <h3>쿠키 설정:</h3>
 * <ul>
 *   <li><strong>쿠키명</strong>: "XSRF-TOKEN"</li>
 *   <li><strong>HttpOnly</strong>: false (JavaScript에서 접근 가능)</li>
 *   <li><strong>Secure</strong>: false (개발환경), true (운영환경)</li>
 *   <li><strong>Path</strong>: "/" (모든 경로에서 접근 가능)</li>
 *   <li><strong>MaxAge</strong>: 3600초 (1시간)</li>
 * </ul>
 * 
 * <h3>필터 순서:</h3>
 * <p>JWT 인증 필터보다 먼저 실행되어 CSRF 토큰을 미리 준비합니다.</p>
 * 
 * <h3>제외 경로:</h3>
 * <ul>
 *   <li>/api/weathers/** - 공개 API</li>
 *   <li>/actuator/** - 모니터링</li>
 *   <li>/swagger-ui/** - API 문서</li>
 *   <li>/v3/api-docs/** - OpenAPI 스펙</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "otboo.security", name = "enable-filters", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CsrfTokenFilter extends OncePerRequestFilter {
    
    private final String SERVICE = "[CsrfTokenFilter] ";
    private final CsrfTokenService csrfTokenService;
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // CSRF 토큰이 쿠키에 없는 경우 생성하여 설정
            if (!hasCsrfTokenCookie(request)) {
                String csrfToken = csrfTokenService.generateCsrfToken();
                setCsrfTokenCookie(response, csrfToken);
                log.debug(SERVICE + "CSRF 토큰 쿠키 설정 완료");
            }
        } catch (Exception e) {
            log.error(SERVICE + "CSRF 토큰 처리 중 오류 발생", e);
            // CSRF 토큰 처리 실패해도 요청은 계속 진행
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * CSRF 토큰 쿠키가 존재하는지 확인합니다.
     *
     * @param request HTTP 요청
     * @return CSRF 토큰 쿠키 존재 여부
     */
    private boolean hasCsrfTokenCookie(HttpServletRequest request) {
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return false;
            }
            
            String cookieName = csrfTokenService.getCsrfTokenCookie();
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.warn(SERVICE + "CSRF 토큰 쿠키 확인 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * CSRF 토큰을 쿠키에 설정합니다.
     *
     * @param response HTTP 응답
     * @param token CSRF 토큰
     */
    private void setCsrfTokenCookie(HttpServletResponse response, String token) {
        try {
            Cookie cookie = new Cookie(csrfTokenService.getCsrfTokenCookie(), token);
            cookie.setHttpOnly(false); // JavaScript에서 접근 가능하도록 설정
            cookie.setSecure(false); // HTTPS에서만 전송 (개발환경에서는 false)
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1시간
            
            response.addCookie(cookie);
        } catch (Exception e) {
            log.error(SERVICE + "CSRF 토큰 쿠키 설정 중 오류 발생", e);
        }
    }
    
    /**
     * 특정 경로는 CSRF 토큰 필터를 건너뛰도록 설정
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // CSRF 토큰 필터를 건너뛸 경로들
        return path.startsWith("/api/weathers") || // 공개 API
                path.startsWith("/actuator") || // 모니터링
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }
}
