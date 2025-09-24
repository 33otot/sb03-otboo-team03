package com.samsamotot.otboo.auth.controller;

import com.samsamotot.otboo.auth.dto.LoginRequest;
import com.samsamotot.otboo.auth.dto.LogoutRequest;
import com.samsamotot.otboo.auth.service.AuthService;
import com.samsamotot.otboo.common.security.jwt.JwtDto;
import com.samsamotot.otboo.common.config.SecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 인증 관련 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인, 로그아웃, CSRF 토큰 관련 API")
public class AuthController {
    
    private final AuthService authService;
    private final SecurityProperties securityProperties;
    
    /**
     * 로그인
     */
    @PostMapping(value = "/sign-in", consumes = "multipart/form-data")
    @Operation(summary = "로그인", description = "사용자명과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    public ResponseEntity<Map<String, Object>> signIn(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {
        
        log.info("[AuthController] 로그인 요청 - 사용자명: {}", username);
        
        // username을 email로 사용하여 LoginRequest 생성
        LoginRequest request = LoginRequest.builder()
            .email(username)
            .password(password)
            .build();
        
        JwtDto jwtDto = authService.login(request);
        
        log.info("[AuthController] 로그인 성공 - 사용자 ID: {}", jwtDto.getUserDto().getId());
        
        // 리프레시 토큰을 쿠키에 설정
        ResponseCookie refreshTokenCookie = ResponseCookie.from("REFRESH_TOKEN", jwtDto.getRefreshToken())
            .httpOnly(true)
            .secure(securityProperties.getCookie().isSecure())
            .path("/")
            .maxAge(7 * 24 * 60 * 60) // 7일
            .sameSite(securityProperties.getCookie().getSameSite())
            .build();
        
        // 리프레시 토큰을 제외한 응답 객체 생성
        Map<String, Object> response = Map.of(
            "userDto", jwtDto.getUserDto(),
            "accessToken", jwtDto.getAccessToken(),
            "tokenType", jwtDto.getTokenType(),
            "expiresIn", jwtDto.getExpiresIn()
        );
        
        return ResponseEntity.ok()
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(response);
    }
    
    /**
     * 로그아웃
     */
    @PostMapping("/sign-out")
    @Operation(summary = "로그아웃", description = "리프레시 토큰을 사용하여 로그아웃합니다.")
    public ResponseEntity<Map<String, String>> signOut(@Valid @RequestBody LogoutRequest request) {
        log.info("[AuthController] 로그아웃 요청");
        
        authService.logout(request);
        
        log.info("[AuthController] 로그아웃 성공");
        
        // 리프레시 토큰 쿠키 삭제
        ResponseCookie refreshTokenCookie = ResponseCookie.from("REFRESH_TOKEN", "")
            .httpOnly(true)
            .secure(securityProperties.getCookie().isSecure())
            .path("/")
            .maxAge(0) // 즉시 삭제
            .sameSite(securityProperties.getCookie().getSameSite())
            .build();
        
        return ResponseEntity.ok()
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(Map.of("message", "로그아웃되었습니다."));
    }
    
    /**
     * JWT 토큰 갱신
     */
    @PostMapping("/refresh")
    @Operation(summary = "JWT 토큰 갱신", description = "쿠키의 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletRequest request) {
        
        log.info("[AuthController] 토큰 갱신 요청");
        
        // 쿠키에서 리프레시 토큰이 없는 경우
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            log.warn("[AuthController] 쿠키에서 리프레시 토큰을 찾을 수 없음");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "리프레시 토큰이 필요합니다.");
        }
        
        JwtDto jwtDto = authService.refreshToken(refreshToken);
        
        log.info("[AuthController] 토큰 갱신 성공 - 사용자 ID: {}", jwtDto.getUserDto().getId());
        
        // Swagger 문서에 맞게 응답 형식 수정 (리프레시 토큰 제외)
        Map<String, Object> response = Map.of(
            "userDto", jwtDto.getUserDto(),
            "accessToken", jwtDto.getAccessToken()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * CSRF 토큰 조회
     */
    @GetMapping("/csrf-token")
    @Operation(summary = "CSRF 토큰 조회", description = "CSRF 보호를 위한 토큰을 조회합니다.")
    public ResponseEntity<Map<String, String>> getCsrfToken() {
        log.debug("[AuthController] CSRF 토큰 조회 요청");
        
        String csrfToken = authService.getCsrfToken();
        
        log.debug("[AuthController] CSRF 토큰 조회 성공");
        
        return ResponseEntity.ok(Map.of("csrfToken", csrfToken));
    }
}
