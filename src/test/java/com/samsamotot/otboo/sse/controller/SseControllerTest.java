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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;
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

        given(jwtTokenProvider.getUserIdFromToken(eq(token)))
            .willReturn(userId);
        given(sseService.createConnection(eq(userId)))
            .willReturn(new SseEmitter(Long.MAX_VALUE));

        // when n then
        mockMvc.perform(get("/api/sse")
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());
    }

    @Test
    void lastEventId_헤더를_서비스로_전달한다() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        given(jwtTokenProvider.getUserIdFromToken("t")).willReturn(userId);

        String lastId = UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        given(sseService.createConnection(userId)).willReturn(emitter);

        // when
        mockMvc.perform(get("/api/sse")
                .header("Authorization", "Bearer t")
                .header("Last-Event-ID", lastId)
                .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        // then
        then(sseService).should().createConnection(userId);
        then(sseService).should().replayMissedEvents(eq(userId), eq(lastId), eq(emitter));
    }
}