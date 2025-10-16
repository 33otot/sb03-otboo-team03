package com.samsamotot.otboo.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {

    /**
     * RedisTemplate Bean을 등록합니다. (직렬화 방식 명시)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory,
        @Qualifier("redisSerializer") GenericJackson2JsonRedisSerializer redisSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // Redis 연결 설정
        template.setConnectionFactory(connectionFactory);

        // 키와 해시 키는 문자열 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 값과 해시 값은 JSON 직렬화
        template.setValueSerializer(redisSerializer);
        template.setHashValueSerializer(redisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis Pub/Sub 메시지 리스너 컨테이너 (SSE용)
     * 테스트 환경에서는 비활성화
     */
    @Bean
    @Profile("!test")
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            com.samsamotot.otboo.sse.listener.SseRedisMessageListener sseRedisMessageListener) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // SSE 알림 채널 구독
        container.addMessageListener(sseRedisMessageListener, new PatternTopic("sse:notification:*"));
        
        log.info("Redis SSE Message Listener Container initialized");
        return container;
    }

    /**
     * 커스텀 GenericJackson2JsonRedisSerializer Bean을 등록합니다.
     */
    @Bean("redisSerializer")
    public GenericJackson2JsonRedisSerializer redisSerializer(ObjectMapper objectMapper) {
        // ObjectMapper 복사 및 타입 정보 활성화 (다양한 객체 타입 지원)
        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.samsamotot.otboo")
                .build(),
            DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }
}
