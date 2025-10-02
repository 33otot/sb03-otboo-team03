package com.samsamotot.otboo.directmessage.interceptor;

import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.interceptor
 * FileName     : StompAuthChannelInterceptor
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 * Description  : WebSocket STOMP 메시지 인증 인터셉터
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private static final String STOMP_AUTH = "[StompAuthChannelInterceptor]";
    
    private final JwtTokenProvider jwt;
    
    /**
     * WebSocket 메시지 전송 전 인증 검사를 수행합니다.
     * 
     * <p>STOMP 명령어별로 다음과 같이 처리합니다:</p>
     * <ul>
     *   <li><strong>CONNECT</strong>: JWT 토큰 검증 및 사용자 인증</li>
     *   <li><strong>SEND</strong>: 인증된 사용자 확인</li>
     *   <li><strong>기타</strong>: 로깅 및 모니터링</li>
     * </ul>
     * 
     * @param message 전송될 메시지
     * @param channel 메시지 채널
     * @return 인증 통과 시 원본 메시지, 실패 시 예외 발생
     * @throws AccessDeniedException 인증 실패 시 발생
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.info(STOMP_AUTH + " preSend called - channel: {}", channel.getClass().getSimpleName());
        
        // STOMP 헤더 접근자 추출
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null) {
            log.info(STOMP_AUTH + "Command: {}, User: {}, SessionId: {}", 
                accessor.getCommand(), accessor.getUser(), accessor.getSessionId());
            
            // CONNECT 명령어 처리: WebSocket 연결 시 JWT 토큰 검증
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                handleConnectCommand(accessor);
                
            // SEND 명령어 처리: 메시지 전송 시 인증된 사용자 확인
            } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                handleSendCommand(accessor);
                
            // 기타 명령어 처리: 로깅 및 모니터링
            } else {
                log.info(STOMP_AUTH + " Other command: {} - User: {}", accessor.getCommand(), accessor.getUser());
            }
        } else {
            log.warn(STOMP_AUTH + " StompHeaderAccessor is null");
        }
        
        return message;
    }
    
    /**
     * CONNECT 명령어를 처리하여 JWT 토큰 검증 및 사용자 인증을 수행합니다.
     * 
     * <p>처리 과정:</p>
     * <ol>
     *   <li>Authorization 헤더에서 Bearer 토큰 추출</li>
     *   <li>JWT 토큰 유효성 검증</li>
     *   <li>토큰에서 사용자 ID 추출</li>
     *   <li>Spring Security 인증 객체 설정</li>
     * </ol>
     * 
     * @param accessor STOMP 헤더 접근자
     * @throws AccessDeniedException 토큰이 유효하지 않거나 없을 때 발생
     */
    private void handleConnectCommand(StompHeaderAccessor accessor) {
        // Authorization 헤더에서 Bearer 토큰 추출
        String auth = Optional.ofNullable(accessor.getFirstNativeHeader("Authorization")).orElse("");
        String token = auth.startsWith("Bearer ") ? auth.substring(7) : null;
        log.info(STOMP_AUTH + " CONNECT - Auth header: {}, Token: {}", auth, token != null ? "present" : "null");
        
        // JWT 토큰 유효성 검증
        if (token == null || !jwt.validate(token)) {
            log.error(STOMP_AUTH + " CONNECT - Invalid token: {}", token);
            throw new AccessDeniedException("Invalid token");
        }
        
        // 토큰에서 사용자 ID 추출 및 인증 객체 설정
        UUID userId = jwt.getUserId(token);
        accessor.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
        log.info(STOMP_AUTH + " CONNECT - User authenticated: {}", userId);
    }
    
    /**
     * SEND 명령어를 처리하여 인증된 사용자만 메시지 전송을 허용합니다.
     * 
     * <p>CONNECT 단계에서 설정된 사용자 정보를 확인하여 인증 상태를 검증합니다.</p>
     * 
     * @param accessor STOMP 헤더 접근자
     * @throws AccessDeniedException 사용자가 인증되지 않았을 때 발생
     */
    private void handleSendCommand(StompHeaderAccessor accessor) {
        log.info(STOMP_AUTH + " SEND - User: {}", accessor.getUser());
        
        // 인증된 사용자 확인
        if (accessor.getUser() == null) {
            log.error(STOMP_AUTH + " SEND - User not authenticated");
            throw new AccessDeniedException("User not authenticated");
        }
    }
}
