package com.samsamotot.otboo.common.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        
        request.setAttribute(CsrfToken.class.getName(), token);
        request.setAttribute(token.getParameterName(), token);
        response.setHeader(token.getHeaderName(), token.getToken());
        
        return ResponseEntity.noContent().build();
    }
}
