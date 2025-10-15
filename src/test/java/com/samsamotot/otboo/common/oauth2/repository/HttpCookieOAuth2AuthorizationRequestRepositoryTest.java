package com.samsamotot.otboo.common.oauth2.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.samsamotot.otboo.common.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpCookieOAuth2AuthorizationRequest 레포지토리 슬라이스 테스트")
class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private SecurityProperties.Cookie cookieProps;

    HttpCookieOAuth2AuthorizationRequestRepository repo;

    @BeforeEach
    void setUp() {
        repo = new HttpCookieOAuth2AuthorizationRequestRepository(securityProperties);
    }

    private OAuth2AuthorizationRequest authReq() {
        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.example.com/o/oauth2/v2/auth")
            .clientId("client-123")
            .redirectUri("https://app.example.com/login/oauth2/code/google")
            .state("abc123")
            .scopes(Set.of("openid","email","profile"))
            .additionalParameters(Map.of("prompt","consent"))
            .build();
    }

    @Test
    void 인증_요청이_주어지면_저장하고_로드했을_때_동일한_요청을_반환한다() {

        // given
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();
        var original = authReq();

        when(securityProperties.getCookie()).thenReturn(cookieProps);
        when(cookieProps.getSameSite()).thenReturn("Lax");
        when(cookieProps.isSecure()).thenReturn(true);

        // when
        repo.saveAuthorizationRequest(original, req, res);

        var next = new MockHttpServletRequest();
        Cookie c = extractCookie(res, HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        assertThat(c).isNotNull();
        next.setCookies(c);
        OAuth2AuthorizationRequest loaded = repo.loadAuthorizationRequest(next);

        // then
        String setCookie = res.getHeader("Set-Cookie");
        assertThat(setCookie).contains("OAUTH2_AUTH_REQUEST=").contains("HttpOnly").contains("SameSite=Lax").contains("Secure");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getAuthorizationUri()).isEqualTo(original.getAuthorizationUri());
        assertThat(loaded.getRedirectUri()).isEqualTo(original.getRedirectUri());
        assertThat(loaded.getClientId()).isEqualTo(original.getClientId());
        assertThat(loaded.getScopes()).isEqualTo(original.getScopes());
        assertThat(loaded.getState()).isEqualTo(original.getState());
        assertThat(loaded.getAdditionalParameters()).isEqualTo(original.getAdditionalParameters());
    }

    @Test
    void 저장된_요청이_있을_때_remove를_호출하면_쿠키를_삭제한다() {

        // given
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();

        when(securityProperties.getCookie()).thenReturn(cookieProps);
        when(cookieProps.getSameSite()).thenReturn("Lax");
        when(cookieProps.isSecure()).thenReturn(true);

        repo.saveAuthorizationRequest(authReq(), req, res);

        var req2 = new MockHttpServletRequest();
        Cookie c = extractCookie(res, HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        assertThat(c).isNotNull();
        req2.setCookies(c);
        var res2 = new MockHttpServletResponse();

        // when
        repo.removeAuthorizationRequest(req2, res2);

        // then
        String deleteHeader = res2.getHeader("Set-Cookie");
        assertThat(deleteHeader).contains("OAUTH2_AUTH_REQUEST=").contains("Max-Age=0");
    }

    @Test
    void 쿠키_삭제를_호출하면_쿠키가_모두_삭제되어야_한다() {

        // given
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();

        when(securityProperties.getCookie()).thenReturn(cookieProps);
        when(cookieProps.getSameSite()).thenReturn("Lax");
        when(cookieProps.isSecure()).thenReturn(true);

        repo.saveAuthorizationRequest(authReq(), req, res);

        // when
        repo.saveAuthorizationRequest(null, req, res);

        // then
        assertThat(res.getHeaders("Set-Cookie"))
            .anyMatch(h -> h.contains("OAUTH2_AUTH_REQUEST=") && h.contains("Max-Age=0"));
    }

    @Test
    void 쿠키가_없을_때_로드하면_null을_반환한다() {

        // given
        var req = new MockHttpServletRequest();

        // when
        OAuth2AuthorizationRequest loaded = repo.loadAuthorizationRequest(req);

        // then
        assertThat(loaded).isNull();
    }

    @Test
    void 쿠키가_잘못된_형식이면_로드할_때_null을_반환한다() {

        // given
        var req = new MockHttpServletRequest();
        req.setCookies(new Cookie(
            HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME, "###not-base64###"));

        // when
        OAuth2AuthorizationRequest loaded = repo.loadAuthorizationRequest(req);

        // then
        assertThat(loaded).isNull();
    }

    /* 쿠키에서 특정 이름의 쿠키를 추출 */
    private Cookie extractCookie(MockHttpServletResponse res, String name) {
        String header = res.getHeader("Set-Cookie");
        if (header == null) return null;
        String[] parts = header.split(";", 2);
        String[] nv = parts[0].split("=", 2);
        if (nv.length < 2) return null;
        if (!name.equals(nv[0])) return null;
        return new Cookie(nv[0], nv[1]);
    }
}
