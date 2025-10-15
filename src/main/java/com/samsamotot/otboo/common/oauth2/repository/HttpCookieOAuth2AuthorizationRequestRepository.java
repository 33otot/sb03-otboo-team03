package com.samsamotot.otboo.common.oauth2.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements
    AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTH_REQUEST_COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3분
    public static final String REPOSITORY = "[HttpCookieOAuth2AuthorizationRequestRepository] ";

    private final ObjectMapper objectMapper;

    @Autowired
    public HttpCookieOAuth2AuthorizationRequestRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 쿠키에서 OAuth2AuthorizationRequest를 로드합니다.
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, OAUTH2_AUTH_REQUEST_COOKIE_NAME)
            .map(cookie -> {
                try {
                    byte[] decodedBytes = base64Decode(cookie.getValue());
                    // JSON 역직렬화로 변경
                    return objectMapper.readValue(decodedBytes, OAuth2AuthorizationRequest.class);
                } catch (Exception e) {
                    log.error(REPOSITORY + "쿠키에서 OAuth2AuthorizationRequest 로드 실패", e);
                    return null;
                }
            })
            .orElse(null);
    }

    /**
     * OAuth2AuthorizationRequest를 쿠키에 저장합니다.
     * authRequest가 null이면 관련 쿠키를 삭제합니다.
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authRequest,
        HttpServletRequest request,
        HttpServletResponse response) {
        if (authRequest == null) {
            deleteCookie(request, response, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(authRequest);
            String encoded = base64Encode(bytes);
            addCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME, encoded, COOKIE_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error(REPOSITORY + "OAuth2AuthorizationRequest 쿠키 저장 실패", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * 쿠키에서 OAuth2AuthorizationRequest를 제거하고 반환합니다.
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
        HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return authRequest;
    }

    /**
     * 인증 요청과 관련된 쿠키를 제거합니다.
     */
    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    // --- cookie helpers ---

    private static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return java.util.Optional.empty();
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return java.util.Optional.of(c);
        }
        return java.util.Optional.empty();
    }

    private static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        getCookie(request, name).ifPresent(c -> {
            c.setValue("");
            c.setPath("/");
            c.setMaxAge(0);
            response.addCookie(c);
        });
    }

    private static String base64Encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64Decode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}
