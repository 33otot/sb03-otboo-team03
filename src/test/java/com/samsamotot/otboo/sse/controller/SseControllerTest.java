package com.samsamotot.otboo.sse.controller;

import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.sse.service.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PackageName  : com.samsamotot.otboo.sse.controller
 * FileName     : SseControllerTest
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 */
@WebMvcTest(SseController.class)
@DisplayName("SSE 컨트롤러 슬라이스 테스트")
class SseControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SseService sseService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    void 유효한_토큰_SSE_Emitter_생성() throws Exception {
        // given
        String token = "dummy.jwt.token";
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        BDDMockito.given(jwtTokenProvider.getUserIdFromToken(eq(token)))
            .willReturn(userId);
        BDDMockito.given(sseService.createConnection(eq(userId)))
            .willReturn(new SseEmitter(Long.MAX_VALUE));

        // when & then
        mockMvc.perform(get("/api/sse")
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());
    }
}