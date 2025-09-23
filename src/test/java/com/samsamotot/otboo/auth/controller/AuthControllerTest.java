package com.samsamotot.otboo.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.auth.dto.LogoutRequest;
import com.samsamotot.otboo.auth.service.AuthService;
import com.samsamotot.otboo.common.security.jwt.JwtDto;
import com.samsamotot.otboo.user.dto.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.samsamotot.otboo.common.security.csrf.CsrfTokenFilter;
import com.samsamotot.otboo.common.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { CsrfTokenFilter.class, JwtAuthenticationFilter.class }
    )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("/api/auth/sign-in 성공: JwtDto 본문, REFRESH_TOKEN 쿠키 설정")
    void signIn_success() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtDto jwtDto = JwtDto.builder()
        .userDto(UserDto.builder().id(userId).email("test@example.com").name("tester").locked(false).build())
        .accessToken("access.jwt")
        .refreshToken("refresh.jwt")
        .expiresIn(3600L)
        .build();

        given(authService.login(any())).willReturn(jwtDto);

        mockMvc.perform(multipart("/api/auth/sign-in")
            .param("username", "test@example.com")
            .param("password", "password"))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("REFRESH_TOKEN")))
        .andExpect(jsonPath("$.accessToken").value("access.jwt"))
        .andExpect(jsonPath("$.refreshToken").value("refresh.jwt"))
        .andExpect(jsonPath("$.userDto.id").exists());
    }

    @Test
    @DisplayName("/api/auth/sign-out 성공: REFRESH_TOKEN 삭제 쿠키와 메시지 반환")
    void signOut_success() throws Exception {
        LogoutRequest request = LogoutRequest.builder().refreshToken("refresh.jwt").build();

        mockMvc.perform(post("/api/auth/sign-out")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("REFRESH_TOKEN")))
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
        .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
    }
}
