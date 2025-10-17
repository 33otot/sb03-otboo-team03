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
import java.util.Set;
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
    private Map<UUID, Set<SseEmitter>> connections() {
        try {
            Field f = SseServiceImpl.class.getDeclaredField("connections");
            f.setAccessible(true);
            return (Map<UUID, Set<SseEmitter>>) f.get(sseService);
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
        // given
        UUID userId = UUID.randomUUID();

        // when
        SseEmitter emitter = sseService.createConnection(userId);

        // then
        assertNotNull(emitter);
        assertTrue(connections().containsKey(userId));
        assertEquals(1, connections().get(userId).size());
        assertTrue(connections().get(userId).contains(emitter));
    }

    @Test
    void 알람_전송_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        connections().get(userId).add(testEmitter);

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
    void 알람_전송_실패시_연결_제거() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        testEmitter.throwOnSend = true;
        connections().get(userId).clear(); // 기존 연결 제거
        connections().get(userId).add(testEmitter); // 실패할 emitter만 추가

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
    void replay_nullLastEventId_전송_안한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CountingEmitter emitter = new CountingEmitter();

        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, null, emitter));
        assertEquals(0, emitter.sentCount);
    }

    @Test
    void lastEventId_공백_전송_안한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CountingEmitter emitter = new CountingEmitter();

        assertDoesNotThrow(() -> sseService.replayMissedEvents(userId, "   ", emitter));
        assertEquals(0, emitter.sentCount);
    }

    @Test
    void backlog_문제시_전송_안한다() throws Exception {
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
    void lastEventId_이후만_전송() throws Exception {
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
    void 마지막이면_전송_안한다() throws Exception {
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
    void backlog_lastEvent_없으면_전송_안한다() throws Exception {
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
    void 연결_생성_후_맵에_등록된다() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        SseEmitter emitter = sseService.createConnection(userId);

        // then
        assertNotNull(emitter);
        assertTrue(connections().containsKey(userId), "연결 생성 후 맵에 등록되어야 함");
        assertTrue(connections().get(userId).contains(emitter), "생성된 emitter가 맵에 저장되어야 함");
    }

    @Test
    void 연결_맵_상태_확인한다() throws Exception {
        // given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        // when
        sseService.createConnection(userId1);
        sseService.createConnection(userId2);

        // then
        assertEquals(2, connections().size(), "두 개의 사용자 연결이 생성되어야 함");
        assertTrue(connections().containsKey(userId1), "첫 번째 사용자 연결이 있어야 함");
        assertTrue(connections().containsKey(userId2), "두 번째 사용자 연결이 있어야 함");
        assertEquals(1, connections().get(userId1).size(), "첫 번째 사용자 연결 수");
        assertEquals(1, connections().get(userId2).size(), "두 번째 사용자 연결 수");
    }

    @Test
    void 전송_중_IOException_발생_시_예외를_던지지_않고_중단한다() throws Exception {
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

    @Test
    void 연결_수동_제거() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);
        assertTrue(connections().containsKey(userId), "연결 생성 후 맵에 있어야 함");

        // when
        connections().remove(userId);

        // then
        assertFalse(connections().containsKey(userId), "연결 제거 후 맵에서 사라져야 함");
    }

    @Test
    void 동일_사용자_중복_연결() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        SseEmitter emitter1 = sseService.createConnection(userId);
        SseEmitter emitter2 = sseService.createConnection(userId);

        // then
        assertEquals(1, connections().size(), "동일 사용자이므로 하나의 엔트리만 있어야 함");
        assertEquals(2, connections().get(userId).size(), "두 개의 연결이 모두 유지되어야 함");
        assertTrue(connections().get(userId).contains(emitter1), "첫 번째 연결이 유지되어야 함");
        assertTrue(connections().get(userId).contains(emitter2), "두 번째 연결이 유지되어야 함");
    }

    @Test
    void 알림_전송_성공시_백로그_추가() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        connections().get(userId).add(testEmitter);

        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("t").content("c")
            .level(NotificationLevel.INFO)
            .build();

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenReturn(dto);

        // when
        sseService.sendNotification(userId, "notification");

        // then
        assertTrue(testEmitter.sent, "알림이 전송되어야 함");
        assertTrue(backlog().containsKey(userId), "백로그에 추가되어야 함");
        assertEquals(1, backlog().get(userId).size(), "백로그에 1개 추가되어야 함");
    }

    @Test
    void replay_전체_백로그_전송_생략() throws Exception {
        // given
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

        // when
        sseService.replayMissedEvents(userId, null, emitter);

        // then
        assertEquals(0, emitter.sentCount, "null lastEventId면 전송하지 않아야 함");
    }

    @Test
    void replay_빈_백로그() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        ArrayDeque<NotificationDto> emptyQ = new ArrayDeque<>();
        backlog().put(userId, emptyQ);

        CountingEmitter emitter = new CountingEmitter();

        // when
        sseService.replayMissedEvents(userId, UUID.randomUUID().toString(), emitter);

        // then
        assertEquals(0, emitter.sentCount, "빈 백로그면 전송하지 않아야 함");
    }

    @Test
    void 잘못된_JSON_전송시_무시() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        connections().get(userId).add(testEmitter);

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenThrow(new RuntimeException("Invalid JSON"));

        // when
        assertDoesNotThrow(() -> sseService.sendNotification(userId, "invalid-json"));

        // then
        assertFalse(testEmitter.sent, "잘못된 JSON은 전송되지 않아야 함");
        assertTrue(connections().containsKey(userId), "연결은 유지되어야 함");
        assertFalse(backlog().containsKey(userId), "백로그에 추가되지 않아야 함");
    }

    @Test
    void 백로그_없는_사용자_replay() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        CountingEmitter emitter = new CountingEmitter();

        // when
        sseService.replayMissedEvents(userId, UUID.randomUUID().toString(), emitter);

        // then
        assertEquals(0, emitter.sentCount, "백로그가 없으면 전송하지 않아야 함");
    }

    @Test
    void 백로그_크기_제한_동작_확인() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        connections().get(userId).add(testEmitter);

        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("t").content("c")
            .level(NotificationLevel.INFO)
            .build();

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenReturn(dto);

        // when
        for (int i = 0; i < 1001; i++) {
            sseService.sendNotification(userId, "notification-" + i);
        }

        // then
        Deque<NotificationDto> userBacklog = backlog().get(userId);
        assertNotNull(userBacklog);
        assertEquals(1000, userBacklog.size());

        NotificationDto lastNotification = userBacklog.peekLast();
        assertNotNull(lastNotification);
    }
}