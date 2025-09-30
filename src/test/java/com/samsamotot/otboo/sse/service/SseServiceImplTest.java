package com.samsamotot.otboo.sse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.follow.mapper.FollowMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * PackageName  : com.samsamotot.otboo.sse.service
 * FileName     : SseServiceImplTest
 * Author       : dounguk
 * Date         : 2025. 9. 29.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SSE 서비스 단위 테스트")
class SseServiceImplTest {

    @InjectMocks
    private SseServiceImpl sseService;

    @Mock
    private ObjectMapper objectMapper;

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

    @SuppressWarnings("unchecked")
    private Map<UUID, Deque<NotificationDto>> backlog() {
        try {
            Field f = SseServiceImpl.class.getDeclaredField("backlog");
            f.setAccessible(true);
            return (Map<UUID, Deque<NotificationDto>>) f.get(sseService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static NotificationDto dtoWithId(UUID id) {
        return NotificationDto.builder()
            .id(id)
            .title("t").content("c")
            .level(NotificationLevel.INFO)
            .build();
    }

    static class CountingEmitter extends SseEmitter {
        int sentCount = 0;
        CountingEmitter() { super(Long.MAX_VALUE); }
        @Override public void send(SseEventBuilder builder) throws IOException { sentCount++; }
    }

    static class FailingReplayEmitter extends SseEmitter {
        int attempted = 0;
        FailingReplayEmitter() { super(Long.MAX_VALUE); }
        @Override public void send(SseEventBuilder builder) throws IOException {
            attempted++;
            throw new IOException("boom");
        }
    }

    static class TestEmitter extends SseEmitter {
        volatile boolean sent = false;
        volatile boolean throwOnSend = false;

        TestEmitter() { super(Long.MAX_VALUE); }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (throwOnSend) throw new IOException("boom");
            sent = true;
        }

        @Override
        public void send(Object object) throws IOException {
            if (throwOnSend) throw new IOException("boom");
            sent = true;
        }
    }


    @Test
    void 연결_성공() throws Exception {
        // fiven
        UUID userId = UUID.randomUUID();

        // when
        SseEmitter emitter = sseService.createConnection(userId);

        // then
        assertNotNull(emitter);
        assertTrue(connections().containsKey(userId));
        assertEquals(1, connections().size());
    }

    @Test
    void 알람_전송_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        connections().put(userId, testEmitter);

        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("t").content("c")
            .level(NotificationLevel.INFO)
            .build();

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenReturn(dto);

        assertDoesNotThrow(() -> sseService.sendNotification(userId, "hello"));
        assertTrue(testEmitter.sent, "send가 호출되어야 함");
        assertTrue(connections().containsKey(userId), "성공 시 연결은 유지");
    }

    @Test
    void 알람_전송_실패시_연결_제거() throws JsonProcessingException {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        testEmitter.throwOnSend = true;
        connections().put(userId, testEmitter);

        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("t").content("c")
            .level(NotificationLevel.INFO)
            .build();

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenReturn(dto);

        // then
        assertDoesNotThrow(() -> sseService.sendNotification(userId, "boom"));
        assertFalse(connections().containsKey(userId), "실패 시 연결이 제거되어야 함");
    }


    @Test
    void 연결_없음() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("t").content("c")
            .level(NotificationLevel.INFO)
            .build();

        when(objectMapper.readValue(
            anyString(),
            org.mockito.ArgumentMatchers.<Class<NotificationDto>>any()
        )).thenReturn(dto);

        // when n then
        assertDoesNotThrow(() -> sseService.sendNotification(userId, "no-conn"));
        assertTrue(connections().isEmpty());
    }

    @Test
    void replay_nullLastEventId_전송_안한다() {
        UUID userId = UUID.randomUUID();
        CountingEmitter emitter = new CountingEmitter();

        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, null, emitter));
        assertEquals(0, emitter.sentCount);
    }

    @Test
    void lastEventId_공백_전송_안한다() {
        UUID userId = UUID.randomUUID();
        CountingEmitter emitter = new CountingEmitter();

        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, "   ", emitter));
        assertEquals(0, emitter.sentCount);
    }

    @Test
    void backlog_문제시_전송_안한다() {
        UUID userId = UUID.randomUUID();
        backlog().remove(userId); // 백로그 없음
        CountingEmitter emitter = new CountingEmitter();
        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, UUID.randomUUID().toString(), emitter));
        assertEquals(0, emitter.sentCount);

        backlog().put(userId, new ArrayDeque<>()); // 빈 큐
        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, UUID.randomUUID().toString(), emitter));
        assertEquals(0, emitter.sentCount);
    }

    @Test
    void lastEventId_이후만_전송() {
        UUID userId = UUID.randomUUID();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        UUID e3 = UUID.randomUUID();

        ArrayDeque<NotificationDto> q = new ArrayDeque<>();
        q.add(dtoWithId(e1));
        q.add(dtoWithId(e2));
        q.add(dtoWithId(e3));
        backlog().put(userId, q);

        CountingEmitter emitter = new CountingEmitter();

        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, e1.toString(), emitter));
        assertEquals(2, emitter.sentCount);
    }

    @Test
    void 마지막이면_전송_안한다() {
        UUID userId = UUID.randomUUID();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();

        ArrayDeque<NotificationDto> q = new ArrayDeque<>();
        q.add(dtoWithId(e1));
        q.add(dtoWithId(e2));
        backlog().put(userId, q);

        CountingEmitter emitter = new CountingEmitter();

        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, e2.toString(), emitter));
        assertEquals(0, emitter.sentCount);
    }

    @Test
    void backlog_lastEvent_없으면_전송_안한다() {
        UUID userId = UUID.randomUUID();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();

        ArrayDeque<NotificationDto> q = new ArrayDeque<>();
        q.add(dtoWithId(e1));
        q.add(dtoWithId(e2));
        backlog().put(userId, q);

        CountingEmitter emitter = new CountingEmitter();

        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, UUID.randomUUID().toString(), emitter));
        assertEquals(0, emitter.sentCount);
    }

    @Test
    @DisplayName("replay: 전송 중 IOException 발생 시 예외를 던지지 않고 중단한다")
    void replay_stopOnIOException_dontThrow() {
        UUID userId = UUID.randomUUID();
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        UUID e3 = UUID.randomUUID();

        ArrayDeque<NotificationDto> q = new ArrayDeque<>();
        q.add(dtoWithId(e1));
        q.add(dtoWithId(e2));
        q.add(dtoWithId(e3));
        backlog().put(userId, q);

        FailingReplayEmitter emitter = new FailingReplayEmitter();

        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, e1.toString(), emitter));
        assertEquals(1, emitter.attempted);
    }
}