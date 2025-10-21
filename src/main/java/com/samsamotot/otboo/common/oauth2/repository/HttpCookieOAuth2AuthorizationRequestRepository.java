package com.samsamotot.otboo.common.oauth2.repository;

import com.samsamotot.otboo.common.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository implements
    AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTH_REQUEST_COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3분
    public static final String REPOSITORY = "[HttpCookieOAuth2AuthorizationRequestRepository] ";

    private final SecurityProperties securityProperties;


    /**
     * 쿠키에서 OAuth2AuthorizationRequest를 로드합니다.
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, OAUTH2_AUTH_REQUEST_COOKIE_NAME)
            .map(cookie -> {
                try {
                    return deserializeReq(cookie.getValue());
                } catch (Exception e) {
                    log.error(REPOSITORY + "쿠키 역직렬화 실패", e);
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
            deleteCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
            deleteCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        try {
            String encoded = serializeReq(authRequest);
            addCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME, encoded, COOKIE_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error(REPOSITORY + "쿠키 저장 실패", e);
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
        deleteCookie(response, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        deleteCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    // --- cookie helpers ---

    private static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return java.util.Optional.empty();
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return java.util.Optional.of(c);
        }
        return Optional.empty();
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        String sameSite = securityProperties.getCookie().getSameSite();
        boolean secure = securityProperties.getCookie().isSecure();
        String domain = securityProperties.getCookie().getDomain();

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .sameSite(sameSite);

        // domain이 설정되어 있으면 추가
        if (domain != null && !domain.isEmpty()) {
            cookieBuilder.domain(domain);
        }

        String setCookie = cookieBuilder.build().toString();
        response.addHeader("Set-Cookie", setCookie);
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        String sameSite = securityProperties.getCookie().getSameSite();
        boolean secure = securityProperties.getCookie().isSecure();
        String domain = securityProperties.getCookie().getDomain();

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(Duration.ZERO)     // 즉시 만료
            .sameSite(sameSite);

        // domain이 설정되어 있으면 추가
        if (domain != null && !domain.isEmpty()) {
            cookieBuilder.domain(domain);
        }

        String setCookie = cookieBuilder.build().toString();
        response.addHeader("Set-Cookie", setCookie);
    }

    // --- serialization helpers ---

    private static String serializeReq(OAuth2AuthorizationRequest req) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(SerializationUtils.serialize(req));
    }

    private static OAuth2AuthorizationRequest deserializeReq(String s) {
        byte[] data = Base64.getUrlDecoder().decode(s);
        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(data);
    }
}
