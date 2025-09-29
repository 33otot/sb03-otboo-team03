package com.samsamotot.otboo.sse.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.sse.service.SseServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PackageName  : com.samsamotot.otboo.sse.integration
 * FileName     : SseIntegrationTest
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 */

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 off
@ActiveProfiles("test")
@Transactional
@DisplayName("SSE 통합 테스트")
public class SseIntegrationTest {

    @Autowired private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired private SseServiceImpl sseService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @SuppressWarnings("unchecked")
    private Map<UUID, SseEmitter> connections() {
        try {
            Field f = SseServiceImpl.class.getDeclaredField("connections");
            f.setAccessible(true);
            return (Map<UUID, SseEmitter>) f.get(sseService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void awaitTrue(Supplier<Boolean> cond, long timeoutMs) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (Boolean.TRUE.equals(cond.get())) return;
            Thread.sleep(20);
        }
        fail("조건 충족 실패 within " + timeoutMs + "ms");
    }

    @Test
    void 알림_전송_실패() throws Exception {
        // given
        UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        BDDMockito.given(jwtTokenProvider.getUserIdFromToken("ok2")).willReturn(userId);

        mockMvc.perform(get("/api/sse")
                .header("Authorization", "Bearer ok2")
                .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        awaitTrue(() -> connections().containsKey(userId), 300);

        class FailingEmitter extends SseEmitter {
            FailingEmitter() { super(Long.MAX_VALUE); }
            @Override public void send(SseEventBuilder builder) throws IOException { throw new IOException("boom"); }
        }
        connections().put(userId, new FailingEmitter());

        // when
        assertDoesNotThrow(() -> sseService.sendNotification(userId, "hello"));

        // then
        assertFalse(connections().containsKey(userId));
    }
}
