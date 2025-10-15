package com.samsamotot.otboo.common.oauth2.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String HANDLER = "[OAuth2LoginFailureHandler] ";

    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        log.warn(HANDLER + "인증 실패: {}", ex.getMessage());
        String errorMessage = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
        res.sendRedirect("/login?error=oauth2&message=" + errorMessage);
    }
}