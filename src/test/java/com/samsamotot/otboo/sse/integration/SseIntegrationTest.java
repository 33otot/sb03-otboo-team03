package com.samsamotot.otboo.sse.integration;

import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.sse.service.SseService;
import com.samsamotot.otboo.config.TestConfig;
import com.samsamotot.otboo.sse.service.SseServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void 알림_전송_실패() throws Exception {
        // given
        UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        BDDMockito.given(jwtTokenProvider.getUserIdFromToken("ok2")).willReturn(userId);

        mockMvc.perform(get("/api/sse")
                .header("Authorization", "Bearer ok2")
                .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        awaitTrue(() -> connections().containsKey(userId), 500);

        java.util.Set<SseEmitter> userConnections = connections().get(userId);
        userConnections.clear();

        class FailingEmitter extends SseEmitter {
            FailingEmitter() { super(Long.MAX_VALUE); }
            @Override public void send(SseEventBuilder builder) throws IOException {
                throw new IOException("boom");
            }
        }
        userConnections.add(new FailingEmitter());

        String json = """
            {
              "id": "%s",
              "title": "t",
              "content": "c",
              "level": "INFO"
            }
            """.formatted(UUID.randomUUID());

        // when
        assertDoesNotThrow(() -> sseService.sendNotification(userId, json));

        // then
        assertTrue(userConnections.isEmpty());
    }

    @Test
    @DisplayName("분산 SSE 연결 생성 및 관리")
    void 분산_SSE_연결_생성_및_관리() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        var emitter = sseService.createConnection(userId);

        // then
        assertThat(emitter).isNotNull();
        assertThat(sseService.getActiveConnectionCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("로컬 사용자에게 직접 알림 전송")
    void 로컬_사용자에게_직접_알림_전송() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        var emitter = sseService.createConnection(userId);

        NotificationDto notification = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("테스트 알림")
            .content("분산 SSE 테스트")
            .level(NotificationLevel.INFO)
            .receiverId(userId)
            .build();

        String notificationJson = objectMapper.writeValueAsString(notification);

        // when
        sseService.sendNotification(userId, notificationJson);

        // then
        assertThat(sseService.isUserConnected(userId)).isTrue();

        emitter.complete();
    }

    @Test
    @DisplayName("원격 사용자에게 Redis Pub/Sub 전송")
    void 원격_사용자에게_Redis_PubSub_전송() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        NotificationDto notification = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("원격 알림")
            .content("Redis Pub/Sub 테스트")
            .level(NotificationLevel.INFO)
            .receiverId(userId)
            .build();

        String notificationJson = objectMapper.writeValueAsString(notification);

        // when
        sseService.sendNotification(userId, notificationJson);

        // then
        assertThat(sseService.isUserConnected(userId)).isFalse();
    }

    @Test
    @DisplayName("Redis에서 받은 메시지를 로컬 연결로만 전송")
    void Redis_메시지_로컬_전송() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        var emitter = sseService.createConnection(userId);

        NotificationDto notification = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("Redis 알림")
            .content("Redis에서 받은 메시지 테스트")
            .level(NotificationLevel.INFO)
            .receiverId(userId)
            .build();

        String notificationJson = objectMapper.writeValueAsString(notification);

        // when
        sseService.sendLocalNotification(userId, notificationJson);

        // then
        assertThat(sseService.isUserConnected(userId)).isTrue();

        emitter.complete();
    }

    @Test
    @DisplayName("Redis 메시지 처리 시 로컬 연결 없음")
    void Redis_메시지_로컬_연결_없음() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        NotificationDto notification = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("Redis 알림")
            .content("로컬 연결 없는 Redis 메시지 테스트")
            .level(NotificationLevel.INFO)
            .receiverId(userId)
            .build();

        String notificationJson = objectMapper.writeValueAsString(notification);

        // when
        sseService.sendLocalNotification(userId, notificationJson);

        // then
        assertThat(sseService.isUserConnected(userId)).isFalse();
    }

    @Test
    @DisplayName("다중 연결 지원 테스트")
    void 다중_연결_지원_테스트() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        
        var emitter1 = sseService.createConnection(userId);
        var emitter2 = sseService.createConnection(userId);
        var emitter3 = sseService.createConnection(userId);

        java.util.Set<SseEmitter> userConnections = connections().get(userId);
        int userConnectionCount = userConnections.size();

        NotificationDto notification = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("다중 연결 알림")
            .content("여러 탭에 동시 전송 테스트")
            .level(NotificationLevel.INFO)
            .receiverId(userId)
            .build();

        String notificationJson = objectMapper.writeValueAsString(notification);

        // when
        sseService.sendNotification(userId, notificationJson);

        // then
        assertThat(sseService.isUserConnected(userId)).isTrue();
        
        assertThat(userConnectionCount).isEqualTo(3);

        emitter1.complete();
        emitter2.complete();
        emitter3.complete();
    }

    @Test
    @DisplayName("다중 연결 중 일부 실패 시 나머지 연결 유지")
    void 다중_연결_일부_실패_테스트() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        
        var normalEmitter = sseService.createConnection(userId);
        
        java.util.Set<SseEmitter> userConnections = connections().get(userId);
        class FailingEmitter extends SseEmitter {
            FailingEmitter() { super(Long.MAX_VALUE); }
            @Override public void send(SseEventBuilder builder) throws IOException {
                throw new IOException("connection failed");
            }
        }
        FailingEmitter failingEmitter = new FailingEmitter();
        userConnections.add(failingEmitter);
        
        int initialConnectionCount = userConnections.size();
        assertThat(initialConnectionCount).isEqualTo(2);

        NotificationDto notification = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("일부 실패 알림")
            .content("일부 연결 실패 테스트")
            .level(NotificationLevel.INFO)
            .receiverId(userId)
            .build();

        String notificationJson = objectMapper.writeValueAsString(notification);

        // when
        sseService.sendNotification(userId, notificationJson);

        // then
        assertThat(sseService.isUserConnected(userId)).isTrue();
        assertThat(userConnections.size()).isEqualTo(1); // 정상 연결만 남아있어야 함
        assertThat(userConnections.contains(failingEmitter)).isFalse(); // 실패한 연결은 제거되어야 함

        normalEmitter.complete();
    }

}
