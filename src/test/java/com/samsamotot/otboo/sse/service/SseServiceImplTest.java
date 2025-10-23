package com.samsamotot.otboo.sse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.follow.mapper.FollowMapper;
import com.samsamotot.otboo.notification.dto.NotificationDto;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.sse.strategy.SseNotificationStrategy;
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
import static org.mockito.Mockito.verify;
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

    @Mock
    private SseNotificationStrategy sseNotificationStrategy;

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
        assertTrue(backlog().containsKey(userId), "백로그에 추가되어야 함");
        assertEquals(1, backlog().get(userId).size(), "백로그에 1개 추가되어야 함");
        
        verify(sseNotificationStrategy).publishNotification(eq(userId), eq(dto));
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

    @Test
    void 백로그에서_특정_알림_제거() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID notificationId1 = UUID.randomUUID();
        UUID notificationId2 = UUID.randomUUID();
        UUID notificationId3 = UUID.randomUUID();

        ArrayDeque<NotificationDto> q = new ArrayDeque<>();
        q.add(dtoWithId(notificationId1));
        q.add(dtoWithId(notificationId2));
        q.add(dtoWithId(notificationId3));
        backlog().put(userId, q);

        // when
        sseService.removeNotificationFromBacklog(userId, notificationId2);

        // then
        Deque<NotificationDto> userBacklog = backlog().get(userId);
        assertNotNull(userBacklog);
        assertEquals(2, userBacklog.size());
        assertTrue(userBacklog.stream().anyMatch(dto -> dto.getId().equals(notificationId1)));
        assertFalse(userBacklog.stream().anyMatch(dto -> dto.getId().equals(notificationId2)));
        assertTrue(userBacklog.stream().anyMatch(dto -> dto.getId().equals(notificationId3)));
    }

    @Test
    void 존재하지_않는_알림_제거_시도() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID notificationId1 = UUID.randomUUID();
        UUID notificationId2 = UUID.randomUUID();
        UUID nonExistentId = UUID.randomUUID();

        ArrayDeque<NotificationDto> q = new ArrayDeque<>();
        q.add(dtoWithId(notificationId1));
        q.add(dtoWithId(notificationId2));
        backlog().put(userId, q);

        // when
        sseService.removeNotificationFromBacklog(userId, nonExistentId);

        // then
        Deque<NotificationDto> userBacklog = backlog().get(userId);
        assertNotNull(userBacklog);
        assertEquals(2, userBacklog.size()); // 변경 없음
    }

    @Test
    void 빈_백로그에서_알림_제거_시도() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        // when
        sseService.removeNotificationFromBacklog(userId, notificationId);

        // then
        assertFalse(backlog().containsKey(userId)); // 백로그가 없어야 함
    }

    @Test
    void 사용자의_모든_백로그_정리() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID notificationId1 = UUID.randomUUID();
        UUID notificationId2 = UUID.randomUUID();
        UUID notificationId3 = UUID.randomUUID();

        ArrayDeque<NotificationDto> q = new ArrayDeque<>();
        q.add(dtoWithId(notificationId1));
        q.add(dtoWithId(notificationId2));
        q.add(dtoWithId(notificationId3));
        backlog().put(userId, q);

        // when
        sseService.clearBacklog(userId);

        // then
        assertFalse(backlog().containsKey(userId)); // 백로그가 완전히 제거되어야 함
    }

    @Test
    void 이미_비어있는_백로그_정리() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        sseService.clearBacklog(userId);

        // then
        assertFalse(backlog().containsKey(userId)); // 백로그가 없어야 함
    }

    @Test
    void 여러_사용자의_백로그_중_특정_사용자만_정리() throws Exception {
        // given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID notificationId1 = UUID.randomUUID();
        UUID notificationId2 = UUID.randomUUID();

        ArrayDeque<NotificationDto> q1 = new ArrayDeque<>();
        q1.add(dtoWithId(notificationId1));
        backlog().put(userId1, q1);

        ArrayDeque<NotificationDto> q2 = new ArrayDeque<>();
        q2.add(dtoWithId(notificationId2));
        backlog().put(userId2, q2);

        // when
        sseService.clearBacklog(userId1);

        // then
        assertFalse(backlog().containsKey(userId1)); // userId1의 백로그는 제거
        assertTrue(backlog().containsKey(userId2)); // userId2의 백로그는 유지
        assertEquals(1, backlog().get(userId2).size());
    }

    @Test
    void 로컬_알림_전송_연결이_있는_경우() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        connections().get(userId).add(testEmitter);

        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("로컬 알림")
            .content("로컬 전송 테스트")
            .level(NotificationLevel.INFO)
            .build();

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenReturn(dto);

        // when
        sseService.sendLocalNotification(userId, "local-notification");

        // then
        assertTrue(testEmitter.sent, "로컬 연결로 전송되어야 함");
        assertTrue(backlog().containsKey(userId), "백로그에 추가되어야 함");
        assertEquals(1, backlog().get(userId).size());
    }

    @Test
    void 로컬_알림_전송_연결이_없는_경우() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("로컬 알림")
            .content("연결 없는 로컬 전송 테스트")
            .level(NotificationLevel.INFO)
            .build();

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenReturn(dto);

        // when
        sseService.sendLocalNotification(userId, "local-notification");

        // then
        assertTrue(backlog().containsKey(userId), "백로그에는 추가되어야 함");
        assertEquals(1, backlog().get(userId).size());
        assertFalse(sseService.isUserConnected(userId), "연결은 없어야 함");
    }

    @Test
    void 로컬_알림_전송_실패_시_연결_제거() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        testEmitter.throwOnSend = true;
        connections().get(userId).clear();
        connections().get(userId).add(testEmitter);

        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("실패할 알림")
            .content("전송 실패 테스트")
            .level(NotificationLevel.INFO)
            .build();

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenReturn(dto);

        // when
        sseService.sendLocalNotification(userId, "failing-notification");

        // then
        assertFalse(connections().containsKey(userId), "실패 시 연결이 제거되어야 함");
        assertTrue(backlog().containsKey(userId), "백로그에는 추가되어야 함");
    }

    @Test
    void 잘못된_JSON으로_로컬_알림_전송_시_무시() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        connections().get(userId).add(testEmitter);

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenThrow(new RuntimeException("Invalid JSON"));

        // when
        sseService.sendLocalNotification(userId, "invalid-json");

        // then
        assertFalse(testEmitter.sent, "잘못된 JSON은 전송되지 않아야 함");
        assertTrue(connections().containsKey(userId), "연결은 유지되어야 함");
        assertFalse(backlog().containsKey(userId), "백로그에 추가되지 않아야 함");
    }

    @Test
    void 연결_상태_확인_연결된_사용자() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        // when & then
        assertTrue(sseService.isUserConnected(userId), "연결된 사용자는 true여야 함");
    }

    @Test
    void 연결_상태_확인_연결되지_않은_사용자() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when & then
        assertFalse(sseService.isUserConnected(userId), "연결되지 않은 사용자는 false여야 함");
    }

    @Test
    void 활성_연결_수_확인() throws Exception {
        // given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        // when
        sseService.createConnection(userId1);
        sseService.createConnection(userId1);
        sseService.createConnection(userId2);

        // then
        assertEquals(3, sseService.getActiveConnectionCount(), "총 3개의 연결이 있어야 함");
    }

    @Test
    void 연결_생성_시_활성_연결_수_증가() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        assertEquals(0, sseService.getActiveConnectionCount());

        // when
        var emitter = sseService.createConnection(userId);

        // then
        assertEquals(1, sseService.getActiveConnectionCount(), "연결 생성 후 1개여야 함");
        assertTrue(sseService.isUserConnected(userId), "사용자가 연결되어야 함");
        emitter.complete();
    }

    @Test
    void 놓친_이벤트_재전송_lastEventId가_null인_경우() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        var emitter = sseService.createConnection(userId);

        // when
        sseService.replayMissedEvents(userId, null, emitter);

        // then
        emitter.complete();
    }

    @Test
    void 놓친_이벤트_재전송_lastEventId가_빈_문자열인_경우() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        var emitter = sseService.createConnection(userId);

        // when
        sseService.replayMissedEvents(userId, "", emitter);

        // then
        emitter.complete();
    }

    @Test
    void 놓친_이벤트_재전송_백로그가_비어있는_경우() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        var emitter = sseService.createConnection(userId);

        // when
        sseService.replayMissedEvents(userId, "some-event-id", emitter);

        // then
        emitter.complete();
    }

    @Test
    void 놓친_이벤트_재전송_lastEventId를_찾을_수_없는_경우() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        var emitter = sseService.createConnection(userId);

        UUID notificationId = UUID.randomUUID();
        NotificationDto dto = NotificationDto.builder()
            .id(notificationId)
            .title("테스트 알림")
            .content("테스트")
            .level(NotificationLevel.INFO)
            .build();

        ArrayDeque<NotificationDto> q = new ArrayDeque<>();
        q.add(dto);
        backlog().put(userId, q);

        // when
        sseService.replayMissedEvents(userId, "non-existent-id", emitter);

        // then
        emitter.complete();
    }

    @Test
    void 놓친_이벤트_재전송_정상적인_경우() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        var emitter = sseService.createConnection(userId);

        UUID notificationId1 = UUID.randomUUID();
        UUID notificationId2 = UUID.randomUUID();

        NotificationDto dto1 = NotificationDto.builder()
            .id(notificationId1)
            .title("첫 번째 알림")
            .content("테스트")
            .level(NotificationLevel.INFO)
            .build();

        NotificationDto dto2 = NotificationDto.builder()
            .id(notificationId2)
            .title("두 번째 알림")
            .content("테스트")
            .level(NotificationLevel.INFO)
            .build();

        ArrayDeque<NotificationDto> q = new ArrayDeque<>();
        q.add(dto1);
        q.add(dto2);
        backlog().put(userId, q);

        // when
        sseService.replayMissedEvents(userId, notificationId1.toString(), emitter);

        // then
        assertTrue(connections().containsKey(userId), "연결은 유지되어야 함");
        emitter.complete();
    }

    @Test
    void 백로그_크기_제한_테스트() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        sseService.createConnection(userId);

        for (int i = 0; i < 1005; i++) {
            NotificationDto dto = NotificationDto.builder()
                .id(UUID.randomUUID())
                .title("알림 " + i)
                .content("테스트 " + i)
                .level(NotificationLevel.INFO)
                .build();

            when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
                .thenReturn(dto);

            sseService.sendNotification(userId, "notification-" + i);
        }

        // then
        assertTrue(backlog().containsKey(userId));
        assertEquals(1000, backlog().get(userId).size(), "백로그 크기는 제한값을 초과하지 않아야 함");
    }

    @Test
    void 다중_사용자_연결_관리() throws Exception {
        // given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        // when
        var emitter1 = sseService.createConnection(userId1);
        var emitter2 = sseService.createConnection(userId1); // 동일 사용자 다중 연결
        var emitter3 = sseService.createConnection(userId2);
        var emitter4 = sseService.createConnection(userId3);

        // then
        assertEquals(4, sseService.getActiveConnectionCount(), "총 4개의 연결이 있어야 함");
        assertTrue(sseService.isUserConnected(userId1), "사용자1은 연결되어야 함");
        assertTrue(sseService.isUserConnected(userId2), "사용자2는 연결되어야 함");
        assertTrue(sseService.isUserConnected(userId3), "사용자3은 연결되어야 함");

        emitter1.complete();
        emitter2.complete();
        emitter3.complete();
        emitter4.complete();
    }

    @Test
    void 연결이_없는_사용자의_백로그_관리() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        NotificationDto dto = NotificationDto.builder()
            .id(UUID.randomUUID())
            .title("백로그 테스트")
            .content("연결 없는 사용자")
            .level(NotificationLevel.INFO)
            .build();

        when(objectMapper.readValue(anyString(), eq(NotificationDto.class)))
            .thenReturn(dto);

        // when
        sseService.sendNotification(userId, "notification");

        // then
        assertTrue(backlog().containsKey(userId), "백로그에는 추가되어야 함");
        assertEquals(1, backlog().get(userId).size());
        assertFalse(sseService.isUserConnected(userId), "연결은 없어야 함");
    }
}