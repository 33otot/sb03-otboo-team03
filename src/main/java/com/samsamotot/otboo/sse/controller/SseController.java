package com.samsamotot.otboo.sse.controller;

import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.sse.controller.api.SseApi;
import com.samsamotot.otboo.sse.service.SseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.sse.controller
 * FileName     : SseController
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SseController implements SseApi {
    private static final String SSE_CONTROLLER = "[SseController] ";

    private final SseService sseService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * SSE 연결을 생성하는 엔드포인트.
     * 클라이언트의 Authorization 헤더에서 JWT 토큰을 검증한 뒤,
     * 해당 userId로 SseEmitter를 생성하여 실시간 알림 스트림을 반환한다.
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, " JWT token 없음");
        }

        String token = authHeader.substring(7);
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        String lastEventId = request.getHeader("Last-Event-ID");

        log.info(SSE_CONTROLLER + "커넷션 요청 userId: {}", userId);
        SseEmitter emitter = sseService.createConnection(userId);
        log.info(SSE_CONTROLLER + " 성공 userId: {}", userId);

        sseService.replayMissedEvents(userId, lastEventId, emitter);

        return emitter;
    }
}