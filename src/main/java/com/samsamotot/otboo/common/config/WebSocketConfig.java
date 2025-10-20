package com.samsamotot.otboo.common.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * PackageName  : com.samsamotot.otboo.common.config
 * FileName     : WebSocketConfig
 * Author       : dounguk
 * Date         : 2025. 9. 23.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        registry.enableSimpleBroker("/sub", "/queue");

        registry.setApplicationDestinationPrefixes("/pub");

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        System.out.println("===================================");
        System.out.println("WebSocket allowed-origins: " + allowedOrigins);
        System.out.println("After split: " + Arrays.toString(allowedOrigins.split(",")));
        System.out.println("===================================");

        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins.split(","))
            .withSockJS()
            .setHeartbeatTime(1000 * 25)
            .setDisconnectDelay(1000 * 5);
    }
}
