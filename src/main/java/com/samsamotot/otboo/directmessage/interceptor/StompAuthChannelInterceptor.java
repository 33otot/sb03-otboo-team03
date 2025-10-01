package com.samsamotot.otboo.directmessage.interceptor;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
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
 * PackageName  : com.samsamotot.otboo.common.interceptor
 * FileName     : StompAuthChannelInterceptor
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private static final String STOMP_AUTH = "[StompAuthChannelInterceptor]";
    
    private final JwtTokenProvider jwt;
    

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.info(STOMP_AUTH + " preSend called - channel: {}", channel.getClass().getSimpleName());
        var acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc != null) {
            log.info(STOMP_AUTH+"Command: {}, User: {}, SessionId: {}", 
                acc.getCommand(), acc.getUser(), acc.getSessionId());
            
            if (StompCommand.CONNECT.equals(acc.getCommand())) {
                String auth = Optional.ofNullable(acc.getFirstNativeHeader("Authorization")).orElse("");
                String token = auth.startsWith("Bearer ") ? auth.substring(7) : null;
                log.info(STOMP_AUTH + " CONNECT - Auth header: {}, Token: {}", auth, token != null ? "present" : "null");
                
                if (token == null || !jwt.validate(token)) {
                    log.error(STOMP_AUTH + " CONNECT - Invalid token: {}", token);
                    throw new AccessDeniedException("Invalid token");
                }
                UUID userId = jwt.getUserId(token);
                acc.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
                log.info(STOMP_AUTH + " CONNECT - User authenticated: {}", userId);
            } else if (StompCommand.SEND.equals(acc.getCommand())) {
                log.info(STOMP_AUTH + " SEND - User: {}", acc.getUser());
                if (acc.getUser() == null) {
                    log.error(STOMP_AUTH + " SEND - User not authenticated");
                    throw new AccessDeniedException("User not authenticated");
                }
            } else {
                log.info(STOMP_AUTH + " Other command: {} - User: {}", acc.getCommand(), acc.getUser());
            }
        } else {
            log.warn(STOMP_AUTH + " StompHeaderAccessor is null");
        }
        return message;
    }
}
