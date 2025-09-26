package com.samsamotot.otboo.directmessage.interceptor;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider jwt;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc != null && StompCommand.CONNECT.equals(acc.getCommand())) {
            String auth = Optional.ofNullable(acc.getFirstNativeHeader("Authorization")).orElse("");
            String token = auth.startsWith("Bearer ") ? auth.substring(7) : null;
            if (token == null || !jwt.validate(token)) throw new AccessDeniedException("Invalid token");
            UUID userId = jwt.getUserId(token);

            // 잠금 계정 차단
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));
            if (user.isLocked()) throw new OtbooException(ErrorCode.USER_LOCKED);

            acc.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
        }
        return message;
    }
}
