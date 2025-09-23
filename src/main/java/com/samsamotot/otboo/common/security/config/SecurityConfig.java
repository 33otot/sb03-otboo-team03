package com.samsamotot.otboo.common.security.config;

import com.samsamotot.otboo.common.security.jwt.JwtAuthenticationFilter;
import com.samsamotot.otboo.common.security.csrf.CsrfTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정 클래스
 * 
 * <p>이 클래스는 애플리케이션의 전체 보안 정책을 정의하고 관리합니다.
 * JWT 기반 인증, CSRF 보호, CORS 설정, 그리고 요청별 권한 관리를 담당합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><strong>보안 필터 체인 설정</strong>: JWT 인증 필터와 CSRF 필터를 적절한 순서로 배치</li>
 *   <li><strong>CORS 설정</strong>: 크로스 오리진 요청 허용 (모든 Origin, 모든 HTTP 메서드, 모든 헤더)</li>
 *   <li><strong>인증 경로 설정</strong>: 공개 API와 보호된 API를 구분하여 접근 제어</li>
 *   <li><strong>세션 관리</strong>: STATELESS 모드로 설정하여 JWT 기반 인증 사용</li>
 * </ul>
 * 
 * <h3>보안 정책:</h3>
 * <ul>
 *   <li><strong>공개 API</strong>: 로그인, 회원가입, 날씨 API, 모니터링, Swagger UI</li>
 *   <li><strong>보호된 API</strong>: 나머지 모든 요청은 JWT 인증 필요</li>
 * </ul>
 * 
 * <h3>필터 순서:</h3>
 * <ol>
 *   <li>CsrfTokenFilter: CSRF 토큰을 쿠키에 설정</li>
 *   <li>JwtAuthenticationFilter: JWT 토큰을 통한 인증 처리</li>
 *   <li>UsernamePasswordAuthenticationFilter: Spring Security 기본 인증 필터</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CsrfTokenFilter csrfTokenFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용 시 불필요)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 관리 (STATELESS로 설정하여 세션 사용 안함)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 요청별 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",                    // 루트 경로
                    "/index.html",         // 메인 페이지
                    "/static/**",          // 정적 리소스
                    "/assets/**",          // 정적 에셋
                    "/favicon.ico", // 파비콘
                    "/logo_symbol.svg", // 로고 아이콘
                    "/api/auth/sign-in",
                    "/api/auth/refresh",  // JWT 토큰 갱신
                    "/api/users", // 회원가입
                    "/api/auth/csrf-token",
                    "/api/weathers/**", // 공개 API (인증 불필요)
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // 필터 추가
            .addFilterBefore(csrfTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 위한 ConfigurationSource 빈을 생성합니다.
     * 
     * <p>이 메서드는 웹 애플리케이션의 CORS 정책을 정의하여 다른 도메인에서의 요청을 허용합니다.
     * 개발 환경에서는 모든 Origin, HTTP 메서드, 헤더를 허용하도록 설정되어 있습니다.</p>
     * 
     * <h3>CORS 설정 내용:</h3>
     * <ul>
     *   <li><strong>허용 Origin</strong>: 모든 도메인 (*)</li>
     *   <li><strong>허용 HTTP 메서드</strong>: GET, POST, PUT, PATCH, DELETE, OPTIONS</li>
     *   <li><strong>허용 헤더</strong>: 모든 헤더 (*)</li>
     *   <li><strong>인증 정보 포함</strong>: 쿠키 및 인증 헤더 허용</li>
     *   <li><strong>Preflight 캐시</strong>: 1시간 (3600초)</li>
     * </ul>
     * 
     * @return CORS 설정이 적용된 CorsConfigurationSource 객체
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 허용할 헤더 설정
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
}
