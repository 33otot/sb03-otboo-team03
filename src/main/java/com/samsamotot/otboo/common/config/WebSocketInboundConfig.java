package com.samsamotot.otboo.common.config;

import com.samsamotot.otboo.directmessage.interceptor.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * PackageName  : com.samsamotot.otboo.common.config
 * FileName     : WebSocketInbountConfig
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */


@Configuration
@RequiredArgsConstructor
class WebSocketInboundConfig implements WebSocketMessageBrokerConfigurer {
    private final StompAuthChannelInterceptor auth;
    @Override public void configureClientInboundChannel(ChannelRegistration reg) {
        reg.interceptors(auth);
    }
}


