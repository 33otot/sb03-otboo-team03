package com.samsamotot.otboo.common.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CSRF 토큰 관련 API 컨트롤러
 * 
 * <p>CSRF 토큰 조회 기능을 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class CsrfController {

    private final CsrfTokenRepository csrfTokenRepository;
    private final CsrfTokenService csrfTokenService;

    /**
     * CSRF 토큰 조회
     * 
     * <p>현재 세션의 CSRF 토큰을 조회합니다. 토큰은 쿠키(XSRF-TOKEN)에 저장됩니다.</p>
     * 
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return CSRF 토큰 정보 (204 No Content + 쿠키 설정)
     */
    @GetMapping("/csrf-token")
    public ResponseEntity<Void> getCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
        return ResponseEntity.noContent().build();
    }

    /**
     * CSRF 토큰 재발급
     * 
     * <p>기존 CSRF 토큰을 무효화하고 새로운 토큰을 발급합니다.</p>
     * 
     * @param request HTTP 요청 (X-XSRF-TOKEN 헤더에 현재 토큰 포함)
     * @param response HTTP 응답
     * @return 새로운 CSRF 토큰 정보
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        String currentToken = request.getHeader(csrfTokenService.getCsrfTokenHeader());
        
        // 현재 토큰 검증
        if (currentToken == null || !csrfTokenService.validateCsrfToken(currentToken)) {
            return ResponseEntity.badRequest().build();
        }
        
        // 새 토큰 생성 및 저장
        String newToken = csrfTokenService.generateCsrfToken();
        CsrfToken csrfToken = new org.springframework.security.web.csrf.DefaultCsrfToken(
            csrfTokenService.getCsrfTokenHeader(),
            csrfTokenService.getCsrfTokenCookie(),
            newToken
        );
        csrfTokenRepository.saveToken(csrfToken, request, response);
        
        return ResponseEntity.ok(Map.of("csrfToken", newToken));
    }
}
