package com.samsamotot.otboo.auth.service;

import com.samsamotot.otboo.auth.dto.LoginRequest;
import com.samsamotot.otboo.auth.dto.LogoutRequest;
import com.samsamotot.otboo.common.security.jwt.JwtDto;

/**
 * 인증 서비스 인터페이스
 */
public interface AuthService {
    
    /**
     * 로그인 처리
     * 
     * @param request 로그인 요청
     * @return JWT 토큰 정보
     */
    JwtDto login(LoginRequest request);
    
    /**
     * 로그아웃 처리
     * 
     * @param request 로그아웃 요청
     */
    void logout(LogoutRequest request);
    
    /**
     * CSRF 토큰 조회
     * 
     * @return CSRF 토큰
     */
    String getCsrfToken();
    
    /**
     * JWT 토큰 갱신
     * 
     * @param refreshToken 리프레시 토큰
     * @return 새로운 JWT 토큰 정보
     */
    JwtDto refreshToken(String refreshToken);
}
