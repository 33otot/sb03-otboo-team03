package com.samsamotot.otboo.sse.service;

import com.samsamotot.otboo.follow.mapper.FollowMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
    private FollowMapper followMapper;

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

    static class TestEmitter extends SseEmitter {
        boolean sent = false;
        boolean throwOnSend = false;
        TestEmitter() { super(Long.MAX_VALUE); }
        @Override
        public void send(SseEventBuilder builder) throws IOException {
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

        // when
        sseService.createConnection(userId);

        TestEmitter testEmitter = new TestEmitter();
        connections().put(userId, testEmitter);

        // then
        assertDoesNotThrow(() ->
            sseService.sendNotification(userId, "hello")
        );
        assertTrue(testEmitter.sent, "send가 호출되어야 함");
        assertTrue(connections().containsKey(userId), "성공 시 연결은 유지");
    }

    @Test
    void 연결_없음() throws Exception {
        // gievn
        UUID userId = UUID.randomUUID();

        // when n then
        assertDoesNotThrow(() ->
            sseService.sendNotification(userId, "no-conn")
        );
        assertTrue(connections().isEmpty());
    }
}