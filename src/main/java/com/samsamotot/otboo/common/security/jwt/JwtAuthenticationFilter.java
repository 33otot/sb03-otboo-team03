package com.samsamotot.otboo.common.security.jwt;

import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT 토큰을 통한 HTTP 요청 인증을 처리하는 필터 클래스
 * 
 * <p>이 필터는 HTTP 요청의 Authorization 헤더에서 JWT 토큰을 추출하고,
 * 토큰의 유효성을 검증하여 Spring Security의 인증 컨텍스트를 설정합니다.
 * JWT 기반의 stateless 인증을 구현하는 핵심 컴포넌트입니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><strong>토큰 추출</strong>: Authorization 헤더에서 JWT 토큰 추출</li>
 *   <li><strong>토큰 검증</strong>: JwtTokenProvider를 통한 토큰 유효성 검사</li>
 *   <li><strong>인증 처리</strong>: 유효한 토큰이면 SecurityContext에 인증 정보 설정</li>
 *   <li><strong>사용자 조회</strong>: 토큰의 사용자 ID로 UserDetails 조회 및 인증 객체 생성</li>
 *   <li><strong>경로 제외</strong>: 특정 공개 API 경로는 인증을 건너뛰도록 설정</li>
 * </ul>
 * 
 * <h3>인증 처리 과정:</h3>
 * <ol>
 *   <li>HTTP 요청에서 Authorization 헤더 확인</li>
 *   <li>"Bearer " 접두사가 있는지 검사</li>
 *   <li>JWT 토큰 추출 및 유효성 검증</li>
 *   <li>토큰에서 사용자 ID 추출</li>
 *   <li>UserService를 통해 UserDetails 조회</li>
 *   <li>UsernamePasswordAuthenticationToken 생성</li>
 *   <li>SecurityContext에 인증 정보 설정</li>
 * </ol>
 * 
 * <h3>필터 순서:</h3>
 * <p>CsrfTokenFilter 다음, Spring Security 기본 인증 필터 이전에 실행됩니다.</p>
 * 
 * <h3>제외 경로:</h3>
 * <ul>
 *   <li>/api/auth/sign-in - 로그인</li>
 *   <li>/api/users (POST) - 회원가입</li>
 *   <li>/api/auth/csrf-token - CSRF 토큰 요청</li>
 *   <li>/api/weathers/** - 공개 API</li>
 *   <li>/actuator/** - 모니터링</li>
 *   <li>/swagger-ui/** - API 문서</li>
 *   <li>/v3/api-docs/** - OpenAPI 스펙</li>
 * </ul>
 * 
 * <h3>예외 처리:</h3>
 * <ul>
 *   <li><strong>토큰 추출 실패</strong>: 다음 필터로 진행 (인증 없음)</li>
 *   <li><strong>토큰 검증 실패</strong>: 로그 기록 후 다음 필터로 진행</li>
 *   <li><strong>사용자 조회 실패</strong>: 로그 기록 후 다음 필터로 진행</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "otboo.security", name = "enable-filters", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final String JWTAUTHFILTER = "[JwtAuthenticationFilter] ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                UUID userId;
                try {
                    userId = jwtTokenProvider.getUserIdFromToken(token);
                } catch (Exception e) {
                    log.warn(JWTAUTHFILTER + "JWT 토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // SecurityContext에 인증 정보가 없는 경우에만 설정
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userService.loadUserByUsername(userId.toString());
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug(JWTAUTHFILTER +"JWT 인증 성공 - 사용자 ID: {}", userId);
                }
            }
            
        } catch (OtbooException e) {
            log.warn(JWTAUTHFILTER +"JWT 인증 실패: {}", e.getMessage());
            // 인증 실패 시 SecurityContext를 비우지 않고 다음 필터로 진행
            // GlobalExceptionHandler에서 처리
        } catch (Exception e) {
            log.error(JWTAUTHFILTER +"JWT 인증 처리 중 오류 발생", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * HTTP 요청에서 JWT 토큰을 추출합니다.
     *
     * @param request HTTP 요청
     * @return JWT 토큰 (없으면 null)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    /**
     * 특정 경로는 JWT 인증을 건너뛰도록 설정
     * (예: 로그인, 회원가입, 공개 API 등)
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // JWT 인증을 건너뛸 경로들
        return path.startsWith("/api/auth/sign-in") ||
                path.startsWith("/api/users") && request.getMethod().equals("POST") || // 회원가입
                path.startsWith("/api/auth/csrf-token") ||
                path.startsWith("/api/weathers") || // 공개 API
                path.startsWith("/actuator") || // 모니터링
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }
}
