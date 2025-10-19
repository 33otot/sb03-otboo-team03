package com.samsamotot.otboo.sse.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.config.TestConfig;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.sse.service.SseService;
import com.samsamotot.otboo.sse.service.SseServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PackageName  : com.samsamotot.otboo.sse.integration
 * FileName     : SseIntegrationTest
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 */

@SpringBootTest
@Import({TestConfig.class, SecurityTestConfig.class})
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 off
@ActiveProfiles("test")
@Transactional
@DisplayName("SSE 통합 테스트")
public class SseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SseService sseService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @SuppressWarnings("unchecked")
    private Map<UUID, java.util.Set<SseEmitter>> connections() {
        try {
            Field f = SseServiceImpl.class.getDeclaredField("connections");
            f.setAccessible(true);
            return (Map<UUID, java.util.Set<SseEmitter>>) f.get(sseService);
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
    void SSE_엔드포인트_통합_테스트_연결_생성() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(userId);

        // when n then
        mockMvc.perform(get("/api/sse")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/event-stream"));

        assertThat(sseService.isUserConnected(userId)).isTrue();
    }

    @Test
    void SSE_엔드포인트_통합_테스트_재연결_시_놓친_이벤트_재전송() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(userId);

        SseEmitter emitter1 = sseService.createConnection(userId);

        NotificationDto notification = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("테스트 알림")
            .content("재연결 테스트")
            .level(NotificationLevel.INFO)
            .receiverId(userId)
            .build();

        String notificationJson = objectMapper.writeValueAsString(notification);
        sseService.sendNotification(userId, notificationJson);

        emitter1.complete();

        // when
        mockMvc.perform(get("/api/sse")
                .header("Authorization", "Bearer test-token")
                .header("Last-Event-ID", notification.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/event-stream"));

        // then
        assertThat(sseService.isUserConnected(userId)).isTrue();
    }

    @Test
    void SSE_엔드포인트_통합_테스트_인증_실패() throws Exception {
        // given
        when(jwtTokenProvider.getUserIdFromToken(anyString()))
            .thenThrow(new RuntimeException("Invalid token"));

        // when n then
        mockMvc.perform(get("/api/sse")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void SSE_엔드포인트_통합_테스트_토큰_없음() throws Exception {
        // when n then
        mockMvc.perform(get("/api/sse"))
                .andExpect(status().isUnauthorized());
    }
}
