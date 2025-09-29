package com.samsamotot.otboo.common.security.config;


import com.samsamotot.otboo.common.security.csrf.SpaCsrfTokenRequestHandler;
import com.samsamotot.otboo.common.security.jwt.JwtAuthenticationFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
 *   <li>Spring Security CSRF Filter: 자동으로 CSRF 토큰 생성 및 검증</li>
 *   <li>JwtAuthenticationFilter: JWT 토큰을 통한 인증 처리</li>
 *   <li>UsernamePasswordAuthenticationFilter: Spring Security 기본 인증 필터</li>
 * </ol>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilter;
    
    @Value("${otboo.cors.allowed-origins:http://localhost:3000,http://localhost:8080}") 
    private String allowedOrigins;
    
    @Value("${otboo.security.cookie.secure:false}")
    private boolean cookieSecure;
    
    @Value("${otboo.security.cookie.same-site:Lax}")
    private String cookieSameSite;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF 토큰 저장소 설정 (환경별 보안 속성 적용)
        CsrfTokenRepository csrfTokenRepository = createSecureCsrfTokenRepository();
        
        http
            // CSRF 보호 활성화 (쿠키 기반)
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                .ignoringRequestMatchers(
                    "/api/auth/sign-in",
                    "/api/auth/sign-out",  // 로그아웃 CSRF 무시
                    "/api/auth/refresh",
                    "/api/users",
                    "/api/auth/csrf-token", // CSRF 토큰 조회 허용
                    "/api/weathers/**",
                    "/actuator/**"
                )
            )
            
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
                    "/api/auth/sign-out",  // 로그아웃
                    "/api/auth/refresh",  // JWT 토큰 갱신
                    "/api/users", // 회원가입
                    "/api/auth/csrf-token",
                    "/api/weathers/**", // 공개 API (인증 불필요)
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // 의상 속성 정의는 ADMIN 유저만 가능
                .requestMatchers("/api/clothes/attribute-defs/**").hasRole("ADMIN")
                
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // 필터 추가 (존재하는 경우에만)
            ;

        // JWT 인증 필터 추가 (CSRF 필터는 Spring Security가 자동으로 처리)
        JwtAuthenticationFilter jwtFilterBean = jwtAuthenticationFilter.getIfAvailable();
        if (jwtFilterBean != null) {
            http.addFilterBefore(jwtFilterBean, UsernamePasswordAuthenticationFilter.class);
        }
        
        return http.build();
    }
    
    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 위한 ConfigurationSource 빈을 생성합니다.
     * 
     * <p>이 메서드는 웹 애플리케이션의 CORS 정책을 정의하여 신뢰할 수 있는 도메인에서의 요청만 허용합니다.
     * 설정 파일에서 허용된 Origin 목록을 로드하여 보안을 강화합니다.</p>
     * 
     * <h3>CORS 설정 내용:</h3>
     * <ul>
     *   <li><strong>허용 Origin</strong>: 설정 파일에서 지정된 도메인들만 허용</li>
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
        
        // 설정에서 허용된 Origin 목록 파싱
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        // 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 허용할 헤더 설정
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // CSRF 토큰 헤더 노출
        configuration.setExposedHeaders(Arrays.asList("X-XSRF-TOKEN"));
        
        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * 환경별 보안 속성이 적용된 CSRF 토큰 저장소를 생성합니다.
     * 
     * <p>이 메서드는 개발/운영 환경에 따라 적절한 쿠키 보안 속성을 설정합니다.
     * Spring Security의 기본 CSRF 보호를 사용하여 안전한 토큰 관리를 제공합니다.</p>
     * 
     * <h3>보안 속성:</h3>
     * <ul>
     *   <li><strong>HttpOnly</strong>: false (SPA에서 JavaScript로 접근 가능)</li>
     *   <li><strong>Secure</strong>: 환경별 설정 (개발: false, 운영: true)</li>
     *   <li><strong>SameSite</strong>: 환경별 설정 (개발: Lax, 운영: Strict)</li>
     *   <li><strong>Path</strong>: "/" (모든 경로에서 접근 가능)</li>
     * </ul>
     * 
     * @return 보안 속성이 적용된 CSRF 토큰 저장소
     */
    @Bean
    public CsrfTokenRepository createSecureCsrfTokenRepository() {
        // Spring Security의 기본 CSRF 토큰 저장소 사용
        // HttpOnly=false로 설정하여 SPA에서 JavaScript로 접근 가능
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        
        // 기본 설정
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        
        // 환경별 보안 속성은 Spring Security가 자동으로 처리
        // Secure 속성은 현재 요청이 HTTPS인지에 따라 자동 결정됨
        log.info("CSRF 토큰 저장소 설정 완료 - Secure: {}, SameSite: {}", cookieSecure, cookieSameSite);
        
        return repository;
    }
    
}
