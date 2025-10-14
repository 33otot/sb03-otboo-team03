package com.samsamotot.otboo.oauth2.handler;

import static org.springframework.http.HttpHeaders.SET_COOKIE;

import com.samsamotot.otboo.common.config.SecurityProperties;
import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.oauth2.principal.OAuth2UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final String HANDLER = "[OAuth2LoginSuccessHandler] ";

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
        throws ServletException, IOException {

        log.debug(HANDLER + "인증 성공: {}", auth.getPrincipal());

        // CustomOAuth2UserService에서 반환한 UserPrincipal을 그대로 사용
        var principal = (OAuth2UserPrincipal) auth.getPrincipal();
        UUID userId = principal.getId();
        log.debug(HANDLER + "인증된 사용자 ID: {}", userId);

        // 토큰 발급
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        log.debug(HANDLER + "JWT 토큰 생성 완료 - userId: {}", userId);

        // 쿠키에 토큰 저장
        ResponseCookie refreshTokenCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
            .httpOnly(true)
            .secure(securityProperties.getCookie().isSecure())
            .sameSite(securityProperties.getCookie().getSameSite())
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .build();
        res.addHeader(SET_COOKIE, refreshTokenCookie.toString());

        // 프론트 콜백으로 리다이렉트
        res.sendRedirect("/");
    }
}