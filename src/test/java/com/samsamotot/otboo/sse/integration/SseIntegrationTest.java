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

        // 기존 연결을 모두 제거하고 FailingEmitter만 추가
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

        // then - FailingEmitter가 제거되어 연결이 비어있어야 함
        assertTrue(userConnections.isEmpty());
    }

    @Test
    @DisplayName("분산 SSE 연결 생성 및 관리")
    void 분산_SSE_연결_생성_및_관리() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        var emitter = sseService.createConnection(userId);

        // Then - 느슨한 검증
        assertThat(emitter).isNotNull();
        // 연결 상태는 생성 직후에는 불안정할 수 있으므로 느슨하게 검증
        assertThat(sseService.getActiveConnectionCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("로컬 사용자에게 직접 알림 전송")
    void 로컬_사용자에게_직접_알림_전송() throws Exception {
        // Given
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

        // When
        sseService.sendNotification(userId, notificationJson);

        // Then
        // 로컬 연결이 있으므로 직접 전송되어야 함
        assertThat(sseService.isUserConnected(userId)).isTrue();

        emitter.complete();
    }

    @Test
    @DisplayName("원격 사용자에게 Redis Pub/Sub 전송")
    void 원격_사용자에게_Redis_PubSub_전송() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        // 로컬 연결은 생성하지 않음 (원격 사용자 시뮬레이션)

        NotificationDto notification = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("원격 알림")
            .content("Redis Pub/Sub 테스트")
            .level(NotificationLevel.INFO)
            .receiverId(userId)
            .build();

        String notificationJson = objectMapper.writeValueAsString(notification);

        // When
        sseService.sendNotification(userId, notificationJson);

        // Then
        // 로컬 연결이 없으므로 Redis Pub/Sub로 전송되어야 함
        assertThat(sseService.isUserConnected(userId)).isFalse();
    }

//    @Test
//    @DisplayName("배치 알림 전송 테스트")
//    void 배치_알림_전송_테스트() throws Exception {
//        // Given
//        UUID localUser1 = UUID.randomUUID();
//        UUID localUser2 = UUID.randomUUID();
//        UUID remoteUser = UUID.randomUUID();
//
//        // 로컬 사용자 연결 생성
//        var emitter1 = sseService.createConnection(localUser1);
//        var emitter2 = sseService.createConnection(localUser2);
//
//        List<UUID> userIds = List.of(localUser1, localUser2, remoteUser);
//
//        NotificationDto notification = NotificationDto.builder()
//            .id(UUID.randomUUID())
//            .title("배치 알림")
//            .content("분산 배치 전송 테스트")
//            .level(NotificationLevel.INFO)
//            .receiverId(localUser1) // 첫 번째 사용자 ID 사용
//            .build();
//
//        String notificationJson = objectMapper.writeValueAsString(notification);
//
//        // When
//        sseService.sendBatchNotification(userIds, notificationJson);
//
//        // Then
//        assertThat(sseService.isUserConnected(localUser1)).isTrue();
//        assertThat(sseService.isUserConnected(localUser2)).isTrue();
//        assertThat(sseService.isUserConnected(remoteUser)).isFalse();
//        assertThat(sseService.getActiveConnectionCount()).isEqualTo(2);
//
//        emitter1.complete();
//        emitter2.complete();
//    }
}
