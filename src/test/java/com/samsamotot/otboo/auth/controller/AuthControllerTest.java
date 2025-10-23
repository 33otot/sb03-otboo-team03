package com.samsamotot.otboo.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.auth.service.AuthService;
import com.samsamotot.otboo.common.security.jwt.JwtDto;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.common.config.SecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.samsamotot.otboo.common.oauth2.handler.OAuth2LoginFailureHandler;
import com.samsamotot.otboo.common.oauth2.handler.OAuth2LoginSuccessHandler;
import com.samsamotot.otboo.common.oauth2.service.OAuth2UserService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.samsamotot.otboo.common.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { JwtAuthenticationFilter.class }
    )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private SecurityProperties securityProperties;

    @MockBean
    private OAuth2UserService oAuth2UserService;

    @MockBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockBean
    private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("/api/auth/sign-in 성공: 리프레시 토큰은 쿠키로만 전달, JSON 응답에는 포함되지 않음")
    void signIn_success() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtDto jwtDto = JwtDto.builder()
        .userDto(UserDto.builder().id(userId).email("test@example.com").name("tester").locked(false).build())
        .accessToken("access.jwt")
        .refreshToken("refresh.jwt")
        .expiresIn(3600L)
        .build();

        // SecurityProperties 모킹
        SecurityProperties.Cookie cookie = new SecurityProperties.Cookie();
        cookie.setSecure(false);
        cookie.setSameSite("Lax");
        given(securityProperties.getCookie()).willReturn(cookie);

        given(authService.login(any())).willReturn(jwtDto);

        mockMvc.perform(multipart("/api/auth/sign-in")
            .param("username", "test@example.com")
            .param("password", "password"))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("REFRESH_TOKEN")))
        .andExpect(jsonPath("$.accessToken").value("access.jwt"))
        .andExpect(jsonPath("$.refreshToken").doesNotExist()) // 리프레시 토큰이 JSON 응답에 없어야 함
        .andExpect(jsonPath("$.userDto.id").exists())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600L));
    }

    @Test
    @DisplayName("/api/auth/sign-out 성공: REFRESH_TOKEN 삭제 쿠키와 메시지 반환")
    void signOut_success() throws Exception {
        // SecurityProperties 모킹
        SecurityProperties.Cookie cookie = new SecurityProperties.Cookie();
        cookie.setSecure(false);
        cookie.setSameSite("Lax");
        given(securityProperties.getCookie()).willReturn(cookie);

        mockMvc.perform(post("/api/auth/sign-out")
            .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "refresh.jwt")))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("REFRESH_TOKEN")))
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
        .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
    }

    @Test
    @DisplayName("/api/auth/sign-out 실패: 리프레시 토큰이 없을 때 400 에러")
    void signOut_failure_noToken() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out"))
        .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/api/auth/refresh 성공: 쿠키의 리프레시 토큰으로 새로운 액세스 토큰 발급")
    void refreshToken_success() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtDto jwtDto = JwtDto.builder()
        .userDto(UserDto.builder().id(userId).email("test@example.com").name("tester").locked(false).build())
        .accessToken("new.access.jwt")
        .refreshToken("new.refresh.jwt")
        .expiresIn(3600L)
        .build();

        given(authService.refreshToken("refresh.jwt")).willReturn(jwtDto);

        mockMvc.perform(post("/api/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "refresh.jwt")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("new.access.jwt"))
        .andExpect(jsonPath("$.userDto.id").exists())
        .andExpect(jsonPath("$.refreshToken").doesNotExist()); // 리프레시 토큰이 JSON 응답에 없어야 함
    }

    @Test
    @DisplayName("/api/auth/refresh 실패: 리프레시 토큰이 없을 때 400 에러")
    void refreshToken_failure_noToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
        .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/api/auth/reset-password 성공: 비밀번호 초기화 요청")
    void resetPassword_success() throws Exception {
        String requestJson = objectMapper.writeValueAsString(Map.of("email", "test@example.com"));

        mockMvc.perform(post("/api/auth/reset-password")
            .contentType("application/json")
            .content(requestJson))
        .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("/api/auth/reset-password 실패: 잘못된 JSON 형식")
    void resetPassword_failure_invalidJson() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
            .contentType("application/json")
            .content("invalid json"))
        .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/api/auth/reset-password 실패: 이메일 누락")
    void resetPassword_failure_missingEmail() throws Exception {
        String requestJson = "{}";

        mockMvc.perform(post("/api/auth/reset-password")
            .contentType("application/json")
            .content(requestJson))
        .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("/api/auth/sign-out 실패: 빈 리프레시 토큰")
    void signOut_failure_emptyToken() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out")
            .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "")))
        .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/api/auth/refresh 실패: 빈 리프레시 토큰")
    void refreshToken_failure_emptyToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "")))
        .andExpect(status().isBadRequest());
    }
}
