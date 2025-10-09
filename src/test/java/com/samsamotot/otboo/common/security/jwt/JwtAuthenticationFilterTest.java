package com.samsamotot.otboo.common.security.jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.samsamotot.otboo.common.config.SecurityProperties;
import com.samsamotot.otboo.user.service.UserService;

public class JwtAuthenticationFilterTest {

  private JwtTokenProvider jwtTokenProvider;
  private TokenInvalidationService tokenInvalidationService;
  private UserService userService;
  private SecurityProperties securityProperties;
  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    jwtTokenProvider = mock(JwtTokenProvider.class);
    tokenInvalidationService = mock(TokenInvalidationService.class);
    userService = mock(UserService.class);
    securityProperties = new SecurityProperties();
    SecurityProperties.Cookie cookie = new SecurityProperties.Cookie();
    cookie.setSecure(false);
    cookie.setSameSite("Lax");
    securityProperties.setCookie(cookie);

    filter = new JwtAuthenticationFilter(jwtTokenProvider, tokenInvalidationService, userService, securityProperties);
  }

  private MockHttpServletRequest authRequest(String token) {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRequestURI("/api/sse");
    req.addHeader("Authorization", "Bearer " + token);
    return req;
  }

  @Test
  @DisplayName("블랙리스트 토큰이면 401과 X-Auth-Error, 삭제 쿠키를 반환한다")
  void blacklist_returns401_and_clearsCookie() throws Exception {
    String token = "t.jwt";
    String jti = "jti-123";
    UUID userId = UUID.randomUUID();

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    doReturn(userId).when(jwtTokenProvider).getUserIdFromToken(token);
    when(jwtTokenProvider.getJti(token)).thenReturn(jti);
    when(tokenInvalidationService.isBlacklisted(jti)).thenReturn(true);

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilterInternal(authRequest(token), res, new MockFilterChain());

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getHeader("X-Auth-Error")).isEqualTo("TOKEN_BLACKLISTED");
    String setCookie = res.getHeader("Set-Cookie");
    assertThat(setCookie).contains("REFRESH_TOKEN=");
    assertThat(setCookie).contains("Max-Age=0");
  }

  @Test
  @DisplayName("컷오프 이전에 발급된 토큰이면 401과 X-Auth-Error, 삭제 쿠키를 반환한다")
  void cutoff_returns401_and_clearsCookie() throws Exception {
    String token = "t.jwt";
    UUID userId = UUID.randomUUID();

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    doReturn(userId).when(jwtTokenProvider).getUserIdFromToken(token);
    when(jwtTokenProvider.getJti(token)).thenReturn("jti-x");
    when(tokenInvalidationService.isBlacklisted("jti-x")).thenReturn(false);
    // iat 100, invalidAfter 200 (millis)
    when(jwtTokenProvider.getIssuedAtEpochSeconds(token)).thenReturn(100L);
    when(tokenInvalidationService.getUserInvalidAfterMillis(userId.toString())).thenReturn(200_000L);

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilterInternal(authRequest(token), res, new MockFilterChain());

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getHeader("X-Auth-Error")).isEqualTo("TOKEN_CUTOFF");
    String setCookie = res.getHeader("Set-Cookie");
    assertThat(setCookie).contains("REFRESH_TOKEN=");
    assertThat(setCookie).contains("Max-Age=0");
  }

  @Test
  @DisplayName("정상 토큰이면 체인이 진행되고 401을 반환하지 않는다")
  void validToken_passesFilter() throws Exception {
    String token = "t.jwt";
    UUID userId = UUID.randomUUID();

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    doReturn(userId).when(jwtTokenProvider).getUserIdFromToken(token);
    when(jwtTokenProvider.getJti(token)).thenReturn("jti-ok");
    when(tokenInvalidationService.isBlacklisted("jti-ok")).thenReturn(false);
    when(jwtTokenProvider.getIssuedAtEpochSeconds(token)).thenReturn(300L);
    when(tokenInvalidationService.getUserInvalidAfterMillis(userId.toString())).thenReturn(200_000L); // 200s

    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilterInternal(authRequest(token), res, new MockFilterChain());

    assertThat(res.getStatus()).isNotEqualTo(401);
  }
}


