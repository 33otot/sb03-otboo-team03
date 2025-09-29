package com.samsamotot.otboo.sse.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * PackageName  : com.samsamotot.otboo.notification.config
 * FileName     : RedisSseConfig
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */

//@Configuration
//class RedisSseConfig {
//    @Bean
//    RedisMessageListenerContainer redisContainer(RedisConnectionFactory cf) {
//        var c = new RedisMessageListenerContainer();
//        c.setConnectionFactory(cf);
//        return c;
//    }
//}